/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

// header file
#include "io.h"
#include <unistd.h>

// data types
#include "protocol.h"
#include "../constants.h"

// communication interface
// TODO this is currently non-generic ): (the only part of this file)
//      move it to... interface is guess?
interface *intrfc = new ethernet("192.168.1.10", 8844);

// locks
std::mutex writer_mutex;
std::condition_variable can_write;

// ports
in   *inPorts[ IN_PORT_COUNT];
out *outPorts[OUT_PORT_COUNT];

// gpio queues
gpi *gpis [GPI_COUNT];
gpo *gpos [GPO_COUNT];

bool is_active = true;

std::vector<int> take(std::shared_ptr<LinkedQueue<WriteState>> q, unsigned int count) {
	std::vector<int> rslt;

	std::shared_ptr<WriteState> s = q->peek();
	unsigned int p = 0, t = s->finished();
	while(rslt.size() < count) {
		if(t == s->remaining()) {
			s = q->peek(++p);
			if(s == NULL) break; // abort, if we reached the end of the queue
			t = s->finished();
		}

		rslt.push_back(s->values[t]);
		t++;
	}

	return rslt;
}

void scheduleWriter() {
	if(DEBUG) printf("\nbegin write loop");

	// terminate if not active
	while(is_active) {
		printf("\n locking writer...");
		std::unique_lock<std::mutex> lock(writer_mutex);
        printf(" locked");

		// gpi values are not acknowledged. They are not queued on the board, since there
		// is virtually now processing time. The value is simply written into memory.
		for(unsigned char i = 0; i < GPI_COUNT; i++) {
			// atomically get the old value and set -1 as new value
			int val = gpis[i]->state.exchange(-1);

			// skip, if the gpio value was invalid
			if (val == -1) continue;

			// append a header with the specified protocol
			std::vector<int> vals = proto->encode_gpio(i, val);

			// send the values
			intrfc->send(vals);
		}

		// send all data from in-going ports
		for(unsigned char i = 0; i < IN_PORT_COUNT; i++) {
			// skip the port, if there are values in transit
			if(*inPorts[i]->transit != 0) continue;
			// skip the port, if it's task queue is empty
			if(inPorts[i]->writeTaskQueue->empty()) continue;

			// gather values to be sent.
			std::vector<int> val = take(inPorts[i]->writeTaskQueue, QUEUE_SIZE);

			// set the transit counter and write variable
			*inPorts[i]->transit  = val.size();

			// append a header with the specified protocol
			val = proto->encode_data(i, val);

			// send the values
			intrfc->send(val);
		}

		// sleep, until there is data to write
		// wake on:
		//  - client-side write (which CAN be sent directly,
		//     i.e. nothing in transit and a previously empty queue)
		//  - server-side ack or poll (received by reader thread)
		//  - shutdown
		printf("\n writer will wait now...");
		can_write.wait(lock);
	}

	if(DEBUG) printf("\n stopped write loop");
}

void scheduleReader() {
	printf("\nbegin read loop");

	while(is_active) {
		printf("\ntrying to read...");
		// wait 2 seconds for input
		if(intrfc->waitForData(2)) {
			// read and interpret a value
			int a;
			if(intrfc->readInt(&a)) proto->decode(a);
		}
	}
}

// read a value without locking or notifications
void read_unsafe(unsigned char pid, int val) {
	if(DEBUG) printf("\n  storing value %d ...", val);

	if(outPorts[pid]->readTaskQueue->empty()) {
		// if the task queue of the target port is empty, append to the value queue
		std::shared_ptr<int> ptr(&val);
		outPorts[pid]->readValueQueue->put(ptr);
	} else {
		// otherwise, add the value to the first task
		std::shared_ptr<ReadState> s = outPorts[pid]->readTaskQueue->peek();
		s->values[s->done] = val;
		s->done++;

		if(s->finished()) outPorts[pid]->readTaskQueue->take();
	}

	if(DEBUG) printf(" done");
}

void read(unsigned char pid, int val[], int size) {
	if(DEBUG) printf("\n locking port %d ...", pid);

	// acquire the port lock
	std::unique_lock<std::mutex> lock(outPorts[pid]->out_port_mutex);

	if(DEBUG) printf(" done\n storing values (count: %d) ...", size);

	// store the read value without recursive locking
	for(int i = 0; i < size; i++) read_unsafe(pid, val[i]);

	if(outPorts[pid]->readTaskQueue->empty()) outPorts[pid]->task_empty.notify_one();
}

// acknowledge without locking or notifications
void acknowledge_unsafe(unsigned char pid, unsigned int count) {

//	if(inPorts[pid] == NULL) {
//		if(DEBUG) printf("port is null");
//		return;
//	}
//	if(inPorts[pid]->writeTaskQueue == NULL) {
//		if(DEBUG) printf("queue is null");
//		return;
//	}

	// return, if the queue is empty (count == 0 or unexpected ack)
	if(inPorts[pid]->writeTaskQueue->peek() == NULL) {
		if(DEBUG) printf("queue is empty, count: %d", count);
		return;
	}

	// if all values of the first task got acknowledged...
	if(count >= (inPorts[pid]->writeTaskQueue->peek()->remaining())) {
		// remove the state from the queue

		std::shared_ptr<WriteState> s = inPorts[pid]->writeTaskQueue->take();

		// update count, transit counter and state
		                 count -= s->remaining();
		*inPorts[pid]->transit -= s->remaining();
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
	// acquire writer lock
	std::unique_lock<std::mutex> lock(writer_mutex);

	// acknowledge the data without recursive locking
	acknowledge_unsafe(pid, count);

	// notify writer thread, if everything was acknowledged
	if(inPorts[pid]->transit == 0) can_write.notify_one();
	// otherwise, set the transit counter to -1 to mark a blocked port
	else *inPorts[pid]->transit = -1;
}

void poll(unsigned char pid) {
	// set transit counter to 0 to enable transmissions
	*inPorts[pid]->transit = 0;

	// notify writer thread, if there are waiting tasks
	if(!inPorts[pid]->writeTaskQueue->empty()) can_write.notify_one();

}

void recv_gpio(unsigned char gid, unsigned char val) {
	// set the value of the gpio component accordingly
	gpos[gid]->state = val;

	// notify gpo condition variable
	gpos[gid]->has_changed.notify_one();
}
