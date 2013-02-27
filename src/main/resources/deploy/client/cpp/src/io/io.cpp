/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

// header file
#include "io.h"

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

// gpi queues
std::shared_ptr<std::atomic<int>> gpi [GPI_COUNT];
std::shared_ptr<std::atomic<int>> gpo [GPO_COUNT];

bool is_active = true;

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
			int val = gpi[i]->exchange(-1);

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
			if(inPorts[i]->transit != 0) continue;
			// skip the port, if it's task queue is empty
			if(inPorts[i]->writeTaskQueue->empty()) continue;

			// TODO gather values to be sent.
//			std::vector<int> val; //= inPortQueue[i]->take(MICROBLAZE_QUEUE_SIZE);
//
//			// set the transit size
//			*inTransit[i] = val.size();
//
//			// append a header with the specified protocol
//			val = proto->encode_data(i, val);
//
//			// send the values
//			intrfc->send(val);
		}

		// sleep, until there is data to write
		// wake on:
		//  - client-side write (which CAN be sent directly,
		//     i.e. nothing in transit and a previously empty queue)
		//  - TODO server-side ack or poll (received by reader thread)
		//  - shutdown
		// Question here: does wait reset signals send beforehand? Answer: YES
		//  (actually, it's more like a listener. It only registers signals, after it started waiting)
		//  !!!! --> if yes, this can deadlock in it's current implementation ): <-- !!!!
		//                   this happens, if a value is written to an already processed queue
		//                   but the corresponding notification is then dumped
		//           if no,  the loop might be called incidentally without values to write (not a big issue)
		//                   this happens, if a value is written to a queue that hasn't been processed,
		//                   resulting in a notification for an already consumed value
		// Solution: basically, we have to acquire the writer lock for each write operation.
		//           this would be easier using a single thread per port, since each queue already has a lock
		//           which could be reused for this purpose.
		//             (only "problem" is, I'm not sure if this can also be done with atomics for GPIs ;)
		//              maybe I would have to use "proper" locks there as well)
		printf("\n writer will wait now...");
		can_write.wait(lock);
	}

	if(DEBUG) printf("\n stopped write loop");
}

void scheduleReader() {
	while(is_active) {
		// wait 2 seconds for input
		if(intrfc->waitForData(2)) {
			// try to read and interpret a value
			int a;
			if(intrfc->readInt(&a)) proto->decode(a);
		}
	}
}

// read a value without locking or notifications
void read_unsafe(int pid, int val) {
	if(outPorts[pid]->readTaskQueue->empty()) {
		// if the task queue of the target port is empty, append to the value queue
		std::shared_ptr<int> ptr(&val);
		outPorts[pid]->readValueQueue->put(ptr);
	} else {
		// otherwise, add the value to the first task
		std::shared_ptr<ReadState> s = outPorts[pid]->readTaskQueue->peek();
		s->values[s->remaining()] = val;
		s->done++;

		if(s->finished()) outPorts[pid]->readTaskQueue->take();
	}
}

void read(int pid, int val) {
	// acquire the port lock
	std::unique_lock<std::mutex> lock(outPorts[pid]->out_port_mutex);

	// store the read value without recursive locking
	read_unsafe(pid, val);

	// TODO This implementation results in the restriction to a single application thread.
	//      If multiple threads are running, a finished blocking call is no longer equivalent
	//      with an empty task queue.
	//      While this can (eventually) be circumvented using notify_all instead, it seems
	//      very inefficient and is not guaranteed to return ...

	// if the task queue is empty now, notify
	if(outPorts[pid]->readTaskQueue->empty()) outPorts[pid]->task_empty.notify_one();
}

// acknowledge without locking or notifications
void acknowledge_unsafe(unsigned char pid, unsigned int count) {
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
	}
}

// acquire locks, notify and call acknowledge_unsafe
void acknowledge(unsigned char pid, unsigned int count) {
	// acquire writer lock
	std::unique_lock<std::mutex> lock(writer_mutex);

	// acknowledge the data without recursive locking
	acknowledge_unsafe(pid, count);

	// notify writer thread, if everything was acknowledged
	if(*inPorts[pid]->transit == count) can_write.notify_one();

}
