/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

// header file
#include "io.h"
#include <unistd.h>
#include <iostream>

// data types
#include "protocol.h"
#include "../constants.h"
#include "../logger.h"

#include <math.h>

// exceptions
#include "../exceptions.h"

// communication interface
interface *intrfc = new ethernet(IP, PORT);

// locks
std::mutex writer_mutex;
std::condition_variable can_write;

// ports
abstractInPort   *inPorts[ IN_PORT_COUNT];
abstractOutPort *outPorts[OUT_PORT_COUNT];

// gpio queues
abstract_gpi *gpis [GPI_COUNT];
abstract_gpo *gpos [GPO_COUNT];

bool is_active = true;

std::vector<int> take(std::shared_ptr<LinkedQueue<abstractWriteState>> q, unsigned int count) {
	std::vector<int> rslt;

	std::shared_ptr<abstractWriteState> s = q->peek();
	unsigned int p = 0;

	while(count > 0) {
		// abort, if the state is null
		if(s == NULL) break;

		// peek the first <count> values of state s (might return a smaller number of values)
		int array[count];
		int valueCount = s->peek(array, count);

		// store all values in the result vector
		for(int i = 0; i < valueCount; i++) {
			rslt.push_back(array[i]);
			count--;
		}

		// get the next state
		s = q->peek(++p);
	}

	return rslt;
}

void scheduleWriter() {
	logger_host << INFO << "begin write loop" << endl;

	// terminate if not active
	while(is_active) {
	    logger_host << FINE << "locking writer ...";

	    std::unique_lock<std::mutex> lock(writer_mutex);

	    logger_host << " locked" << endl;

		// gpi values are not acknowledged. They are not queued on the board, since there
		// is virtually now processing time. The value is simply written into memory.
		for(unsigned char i = 0; i < GPO_COUNT; i++) {
			// atomically get the old value and set -1 as new value
			int val = gpos[i]->state.exchange(-1);

			// skip, if the gpio value was invalid
			if (val == -1) continue;

			// append a header with the specified protocol
			std::vector<int> vals = proto->encode_gpio(i, val);

			// send the values
			try {
				intrfc->send(vals);
			} catch (mediumException &e) {

			} catch (protocolException &e) {

			}
		}

		// send all data from in-going ports
		for(unsigned char i = 0; i < IN_PORT_COUNT; i++) {

		    logger_host << FINE << " trying to lock port " << i << " ...";

			// try to lock the port
			std::unique_lock<std::mutex> port_lock(inPorts[i]->port_mutex, std::try_to_lock);

			if(port_lock.owns_lock()) logger_host << " success" << endl;
			else logger_host << " failed" << endl;

			// if we could not acquire the lock, continue with the next port
			if(! port_lock.owns_lock()) continue;

			// skip the port, if there are values in transit
			if(*inPorts[i]->transit != 0) continue;
			// skip the port, if it's task queue is empty
			if(inPorts[i]->writeTaskQueue->empty()) continue;

			// TODO this should be set individually for each port binding! not global
			unsigned int size = QUEUE_SIZE_SW;
			unsigned int sendSize = std::min(size, proto->max_size())
			// gather i values to be sent, where i the minimum of the maximal numbers of values
			// receivable by the board and the maximal size of a message with the used protocol version
			std::vector<int> val = take(inPorts[i]->writeTaskQueue, sendSize);

			// set the transit counter and write variable
			*inPorts[i]->transit  = val.size();

			// append a header with the specified protocol
			val = proto->encode_data(i, val);

			// send the values
			try {
				intrfc->send(val);
			} catch (mediumException &e) {
				while(!inPorts[i]->writeTaskQueue->empty()) {
					std::shared_ptr<abstractWriteState> s = inPorts[i]->writeTaskQueue->take();
					s->fail = true;
					s->m = std::string("could not write values to medium: ") + e.what();
				}
			} catch (protocolException &e) {
				while(!inPorts[i]->writeTaskQueue->empty()) {
					std::shared_ptr<abstractWriteState> s = inPorts[i]->writeTaskQueue->take();
					s->fail = true;
					s->m = std::string("protocol encoder reported an exception: ") + e.what();
				}
			}
		}

		// sleep, until there is data to write
		// wake on:
		//  - client-side write (which CAN be sent directly,
		//     i.e. nothing in transit and a previously empty queue)
		//  - server-side ack or poll (received by reader thread)
		//  - shutdown
		logger_host << FINE << "writer will wait now ..." << endl;

		can_write.wait(lock);
	}

	logger_host << INFO << "stopped write loop" << endl;
}

void scheduleReader() {
    logger_host << INFO << "begin read loop" << endl;

	while(is_active) {
	    logger_host << FINE << "trying to read ..." << endl;

	    // wait 2 seconds for input
		if(intrfc->waitForData(2,0)) {
			try {
				// read and interpret a value
				int a;
				intrfc->readInt(&a);
				proto->decode(a);
			} catch(mediumException &e) {
				// there should be data, but there is no data.
				// this is a bit weird...
			    logger_host << ERROR << e.what();
			} catch(protocolException &e) {
				// marks an error in decoding the message
			    logger_host << ERROR << e.what();
			}
		}
	}
}

void send_poll(unsigned char pid, unsigned int count) {
	std::vector<int> val = proto->encode_poll(pid, count);
	try {
		intrfc->send(val);
		} catch (mediumException &e) {
		} catch (protocolException &e) {
		}
}

/**
 * store a read value at a port without locking or notifications.
 * @param pid Id of the target port.
 * @param val Value to be stored.
 */
void read_unsafe(unsigned char pid, int val) {
    logger_host << FINE << " storing value " << val << " ...");

	std::cout.flush();

	if(outPorts[pid]->readTaskQueue->empty()) {
		// if the task queue of the target port is empty, append to the value queue
		std::shared_ptr<int> ptr((int*)malloc(sizeof(int)));
		*ptr = val;
		outPorts[pid]->readValueQueue->put(ptr);
	} else {
		// otherwise, add the value to the first task
		std::shared_ptr<abstractReadState> s = outPorts[pid]->readTaskQueue->peek();
		s->store(&val, 1);

		if(s->finished()) outPorts[pid]->readTaskQueue->take();
	}

	logger_host << " done" << endl;
}

void read(unsigned char pid, int val[], int size) {

    logger_host << FINE << " locking port " << pid << " ...";

	std::cout.flush();

	// acquire the port lock
	std::unique_lock<std::mutex> lock(outPorts[pid]->port_mutex);

	logger_host << " done" << endl;
	logger_host << FINE << " storing values (count: " << size << ") ..." << endl);

	std::cout.flush();

	// store the read value without recursive locking
	for(int i = 0; i < size; i++) read_unsafe(pid, val[i]);

	// if the task queue is empty now, notify the application
	if(outPorts[pid]->readTaskQueue->empty()) outPorts[pid]->task_empty.notify_one();
}

// acknowledge without locking or notifications
void acknowledge_unsafe(unsigned char pid, unsigned int count) {

	// return, if the queue is empty (count == 0 or unexpected ack)
	if(inPorts[pid]->writeTaskQueue->peek() == NULL) {
	    logger_host << FINE << "queue is empty, count: " << count << endl);
		return;
	}

	// if all values of the first task got acknowledged... (i.e. count >= remainder of the first task)
	if(count >= (inPorts[pid]->writeTaskQueue->peek()->size - inPorts[pid]->writeTaskQueue->peek()->done)) {

		// remove the state from the queue
		std::shared_ptr<abstractWriteState> s = inPorts[pid]->writeTaskQueue->take();

		// update count, transit counter and state
		                 count -= (s->size - s->done);
		*inPorts[pid]->transit -= (s->size - s->done);
		               s->done  = s->size;

		// acknowledge further values
		acknowledge_unsafe(pid, count);
	} else {
		// update state (if count == 0, nothing happens;)
		inPorts[pid]->writeTaskQueue->peek()->done += count;

		// update transit counter
		*inPorts[pid]->transit -= count;

	}
}

// acquire locks, notify and call acknowledge_unsafe
void acknowledge(unsigned char pid, unsigned int count) {
	// acquire port lock
	std::unique_lock<std::mutex> port_lock(inPorts[pid]->port_mutex);

	// acknowledge the data without recursive locking
	acknowledge_unsafe(pid, count);

	// if task queue is empty, notify the port cv and return
	if(inPorts[pid]->writeTaskQueue->empty()) {
		inPorts[pid]->task_empty.notify_one();
		return;
	}

	// otherwise there are still values to write.
	// acquire writer lock
	std::unique_lock<std::mutex> lock(writer_mutex);

	// notify writer thread, if everything was acknowledged
	if(*inPorts[pid]->transit == 0) can_write.notify_one();
	// otherwise, set the transit counter to -1 to mark a blocked port
	else *inPorts[pid]->transit = -1;
}

void recv_poll(unsigned char pid) {
	// acquire port lock
	std::unique_lock<std::mutex> port_lock(inPorts[pid]->port_mutex);

    // ignore the poll, if there are unacknowledged values in transit
    if(*inPorts[pid]->transit > 0) return;

	// set transit counter to 0 to enable transmissions
	*inPorts[pid]->transit = 0;

	// notify writer thread, if there are waiting tasks
	if(!inPorts[pid]->writeTaskQueue->empty()) {
		// acquire the writer lock
		std::unique_lock<std::mutex> lock(writer_mutex);

		// release the port lock!
		port_lock.unlock();

		// notify writer
		can_write.notify_one();
	}
}

void recv_gpio(unsigned char gid, unsigned char val) {
	// set the value of the gpio component accordingly
	gpis[gid]->state = val;

	// notify gpo condition variable
	gpis[gid]->has_changed.notify_one();
}
