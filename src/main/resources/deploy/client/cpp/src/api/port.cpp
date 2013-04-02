/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

// locking
#include <mutex>

// standard library
#include <iostream>
#include <stdlib.h>

// data types
#include "port.h"
#include "../constants.h"
#include "../io/io.h"


in::in(int pid) : writeTaskQueue(new LinkedQueue<WriteState>()), transit(new int(0)) {
	inPorts[pid] = this;
}

// GENERAL WRITE
void in::write(std::shared_ptr<WriteState> s) {
	// acquire port lock
	std::unique_lock<std::mutex> port_lock(port_mutex);

	// put the value in the queue
	writeTaskQueue->put(s);

	// acquire writer lock
	std::unique_lock<std::mutex> write_lock(writer_mutex);

	// notify, if the port is ready (may notify for tasks further ahead in queue, but doesn't matter)
	if(*transit == 0) can_write.notify_one();

	// release the port lock!
	port_lock.unlock();

	// wait for the queue to get empty
	task_empty.wait(write_lock);
}

std::shared_ptr<State> in::nbwrite(std::shared_ptr<WriteState> s) {
	// acquire port lock
	std::unique_lock<std::mutex> port_lock(port_mutex);

	// put the value in the queue
	writeTaskQueue->put(s);

	// acquire writer lock
	std::unique_lock<std::mutex> write_lock(writer_mutex);

	// notify, if the port is ready
	if(*transit == 0) can_write.notify_one();

	// release locks and return state pointer
	return s;
}

void in::block() {
	//acquire port lock
	std::unique_lock<std::mutex> port_lock(port_mutex);

	// if the queue is empty, return
	if(writeTaskQueue->empty()) return;

	// otherwise wait for it to get empty
	task_empty.wait(port_lock);
}

// BLOCKING WRITES
void in::write(int val) {
	write(std::shared_ptr<WriteState>(new WriteState(val)));
}
void in::write(std::vector<int> val) {
	write(std::shared_ptr<WriteState>(new WriteState(val)));
}
void in::write(int val[], int size) {
	write(std::shared_ptr<WriteState>(new WriteState(val, size)));
}

// NON-BLOCKING WRITES
std::shared_ptr<State> in::nbwrite(int val) {
	return nbwrite(std::shared_ptr<WriteState>(new WriteState(val)));
}
std::shared_ptr<State> in::nbwrite(std::vector<int> val) {
	return nbwrite(std::shared_ptr<WriteState>(new WriteState(val)));
}
std::shared_ptr<State> in::nbwrite(int val[], int size) {
	return nbwrite(std::shared_ptr<WriteState>(new WriteState(val, size)));
}

// STREAMED WRITES (BLOCKING)
in& operator<< (in &i, const int val) {
	i.write(val);
	return i;
}
in& operator<< (in &i, const std::vector<int> val) {
	i.write(val);
	return i;
}

// GENERAL READ
out::out(int pid) : readValueQueue(new LinkedQueue<int>()), readTaskQueue(new LinkedQueue<ReadState>()) {
	outPorts[pid] = this;
}

void out::read(std::shared_ptr<ReadState> &s) {
	// acquire port lock
	 std::unique_lock<std::mutex> lock(port_mutex);

	// if there are unfinished tasks in the read queue, append this one
	if(! readTaskQueue->empty()) {
		readTaskQueue->put(s);
	} else while(!s->finished()) {
		// if the result queue is empty, append this read to the task list and return
		if(readValueQueue->empty()) {
			readTaskQueue->put(s);
			break;
		}
		// otherwise, take a value, update the state and do another iteration
		int a = *readValueQueue->take();
		s->values[s->processed()] = a;
		s->done++;
	}

	task_empty.wait(lock);
}

std::shared_ptr<State> out::nbread(std::shared_ptr<ReadState> &s) {
	// acquire port lock
	 std::unique_lock<std::mutex> lock(port_mutex);

	// if there are unfinished tasks in the read queue, just append this one
	if(! readTaskQueue->empty()) {
		readTaskQueue->put(s);
		return s;
	}

	while(!s->finished()) {
		// if the result queue is empty, append this read to the task list and return
		if(readValueQueue->empty()) {
			readTaskQueue->put(s);
			return s;
		}
		// otherwise, take a value, update the state and do another iteration
		int a = *readValueQueue->take();
		s->values[s->processed()] = a;
		s->done++;
	}

	return s;
}

void out::block() {
	// acquire port lock (yet again)
	std::unique_lock<std::mutex> lock(port_mutex);

	// return, if the task queue is empty
	if(readTaskQueue->empty()) return;

	// otherwise wait for it to get empty
	task_empty.wait(lock);
}

// BLOCKING READS
int out::read() {
	int val;
	read(val);
	return val;
}
void out::read(int &val) {
	std::shared_ptr<ReadState> s (new ReadState(&val));
	read(s);
}
void out::read(std::vector<int> &val) {
	std::shared_ptr<ReadState> s (new ReadState(&val));
	read(s);
}
void out::read(int val[], unsigned int size) {
	std::shared_ptr<ReadState> s (new ReadState(val, size));
	read(s);
}

// NON-BLOCKING READS
std::shared_ptr<State> out::nbread(int &val) {
	std::shared_ptr<ReadState> s(new ReadState(&val));
	nbread(s);
	return s;
}

std::shared_ptr<State> out::nbread(std::vector<int> &val) {
	std::shared_ptr<ReadState> s(new ReadState(&val));
	nbread(s);
	return s;
}
std::shared_ptr<State> out::nbread(int val[], int size) {
	std::shared_ptr<ReadState> s(new ReadState(val, size));
	nbread(s);
	return s;
}

// STREAMED READS (BLOCKING)
out& operator >>(out &o, int &val) {
	o.read(val);
	return o;
}
out& operator >>(out &o, std::vector<int> &val) {
	o.read(val);
	return o;
}
