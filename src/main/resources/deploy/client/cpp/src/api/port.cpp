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


in::in(int pid) : writeTaskQueue(new LinkedQueue<WriteState>()), transit(new unsigned int(0)) {
	inPorts[pid] = this;
}

// GENERAL WRITE
std::shared_ptr<State> in::write(std::shared_ptr<WriteState> s) {
	// acquire writer lock
	std::unique_lock<std::mutex> lock(writer_mutex);

	// notify, if queue is empty and nothing is in transit
	if(writeTaskQueue->empty() && transit == 0) can_write.notify_one();

	// put the value in the queue
	writeTaskQueue->put(s);

	return s;
}

void in::block() {
	// TODO Wait until task queue is empty
}

// BLOCKING WRITES
void in::write(int val) {
	write(std::shared_ptr<WriteState>(new WriteState(val)));
	block();
}
void in::write(std::vector<int> val) {
	write(std::shared_ptr<WriteState>(new WriteState(val)));
	block();
}
void in::write(int val[], int size) {
	write(std::shared_ptr<WriteState>(new WriteState(val, size)));
	block();
}

// NON-BLOCKING WRITES
std::shared_ptr<State> in::nbwrite(int val) {
	return write(std::shared_ptr<WriteState>(new WriteState(val)));
}
std::shared_ptr<State> in::nbwrite(std::vector<int> val) {
	return write(std::shared_ptr<WriteState>(new WriteState(val)));
}
std::shared_ptr<State> in::nbwrite(int val[], int size) {
	return write(std::shared_ptr<WriteState>(new WriteState(val, size)));
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

std::shared_ptr<State> out::read(std::shared_ptr<ReadState> &s) {
	// acquire port lock
	 std::lock_guard<std::mutex> lock(out_port_mutex);

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
	std::unique_lock<std::mutex> lock(out_port_mutex);

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
	block();
}
void out::read(std::vector<int> &val) {
	std::shared_ptr<ReadState> s (new ReadState(&val));
	read(s);
	block();
//	unsigned int size = val.size();
//	val.clear();
//	for(unsigned int i = 0; i < size; i++) val.push_back(read());
}
void out::read(int val[], unsigned int size) {
	std::shared_ptr<ReadState> s (new ReadState(val, size));
	read(s);
	block();
	//	for(unsigned int i = 0; i < size; i++) val[i] = read();
}

// NON-BLOCKING READS
std::shared_ptr<State> out::nbread(int &val) {
	std::shared_ptr<ReadState> s(new ReadState(&val));
	read(s);
	return s;
}

std::shared_ptr<State> out::nbread(std::vector<int> &val) {
	std::shared_ptr<ReadState> s(new ReadState(&val));
	read(s);
	return s;

	//	// store the vector size and clear the vector
//	unsigned int size = val.size();
//	val.clear();
//
//	// read queue
//	for(unsigned int i = 0; i < size; i++) {
//		// abort, if the queue is empty
//		if(portResultQueue->size() == 0) break;
//		// otherwise append the first value and update the state
//		val.push_back(*portResultQueue->take());
//		s->done ++;
//	}
}
std::shared_ptr<State> out::nbread(int val[], int size) {
	std::shared_ptr<ReadState> s(new ReadState(val, size));
	read(s);
	return s;

//	for(int i = 0; i < size; i++) {
//		// abort, if the queue is empty
//		if(portResultQueue->size() == 0) break;
//		// otherwise append the first value and update the state
//		val[i] = *portResultQueue->take();
//		s->done ++;
//	}
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
