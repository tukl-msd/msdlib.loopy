/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "port.h"
#include "constants.h"
#include <iostream>
#include <stdio.h>
#include <stdlib.h>

in::in(interface *intrfc) {
	this->intrfc = intrfc;
	writeQueue = (state*)malloc(QUEUE_SIZE * sizeof(state));
}

in::~in() {
	free(writeQueue);
}

out::out(interface *intrfc) {
	this->intrfc = intrfc;
	readQueue = (state*)malloc(QUEUE_SIZE * sizeof(state));
}

out::~out() {
	free(readQueue);
}

//
//out::~out() {
//
//}
//
//dual::~dual() {
//
//}

state::state(int val) : size(1), done(0) {
	values = (int*)malloc(1 * sizeof(int));
	values[0] = val;
}

state::state(std::vector<int> val) : size(val.size()), done(0) {
	values = (int*)malloc(size * sizeof(int));
	for(int i = 0; i < size; i++) values[i] = val.at(i);
}

state::state(int val[], int size) : size(size), done(0) {
	values = (int*)malloc(size * sizeof(int));
	for(int i = 0; i < size; i++) values[i] = val[i];
}

state::~state() {
	free(values);
	values = NULL;
}

bool state::isFinished() {
	printf("size: %d, done: %d", size, done);
	return size == done;
}

int state::processedValues() {
	return done;
}

// BLOCKING WRITES
void in::write(int val) {
	intrfc->send(val);
}

void in::write(std::vector<int> val) {
	//idk ...  copy to array? copy the array above to a vector? what's better here?
}
void in::write(int val[], int size) {
	for(int i = 0; i < size; i++) printf("\nwriting %d", val[i]);
}

// NON-BLOCKING WRITES
state* in::nbwrite(int val) {
	state *s = new state(val);
	printf("\nwriting %d", val);
	s->done ++;
	return s;
}
state* in::nbwrite(std::vector<int> val) {
	state *s = new state(val);

	return s;
}
state* in::nbwrite(int val[], int size) {
	state *s = new state(val, size);
	for(int i = 0; i < size; i++) {
 		printf("\nwriting %d", val[i]);
		s->done ++;
	}
	return s;
}

// STREAMED WRITES (NON-BLOCKING)
in& operator<< (in &a, const int val) {
	printf("\nit's a stream!");
	a.write(val);
	return a;
}
in& operator<< (in &a, const std::vector<int> val) {
	printf("\nit's a stream!");
	a.write(val);
	return a;
}

// BLOCKING READS
int out::read() {
	// TODO intrfc->recv() or something
	return 5;
}
void out::read(int &val) {
	val = read();
}
void out::read(std::vector<int> &val) {
	unsigned int size = val.size();
	val.clear();
	for(unsigned int i = 0; i < size; i++) val.push_back(read());
}
void out::read(int val[], int size) {
	// TODO while this would work, it's probably inefficient considering the protocol overhead...
	for(int i = 0; i < size; i++) val[i] = read();
}

// NON-BLOCKING READS
state* out::nbread(int &val) {
	state *s = new state(val);
	val = read();
	s->done ++;
	return s;
}
state* out::nbread(std::vector<int> &val) {
	state *s = new state(val);
	unsigned int size = val.size();
	val.clear();
	// TODO this evidently blocks - and is inefficient as hell...
	for(unsigned int i = 0; i < size; i++) {
		val.push_back(read());
		s->done ++;
	}
	return s;
}
state* out::nbread(int val[], int size) {
	state *s = new state(val, size);
	// TODO this evidently blocks - and is inefficient as hell...
	for(int i = 0; i < size; i++) {
		val[i] = read();
		s->done ++;
	}
	return s;
}

// STREAMED READS (IDK)
out& operator >>(out &o, int &val) {
	val = o.read();
	return o;
}
out& operator >>(out &o, std::vector<int> &val) {
	unsigned long int size = val.size();
	int values[size];
	o.read(values, size);
	val.assign(values, values + size);
	return o;
}

//out out::operator>> (std::vector<int>) {
//	return *this;
//}
//
//out out::operator>> (int val) {
//	val = read();
//	return *this;
//}
