/*
 * state.cpp
 *
 *  Created on: 26.02.2013
 *      Author: thomas
 */

#include "state.h"

// standard library
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

bool State::finished() {
	printf("size: %d, done: %d", size, done);
	return size == done;
}

unsigned int State::processed() {
	return done;
}

unsigned int State::remaining() {
	return size - done;
}

unsigned int State::total() {
	return size;
}

WriteState::WriteState(int val) : State(1, 0) {
	values = (int*)malloc(1 * sizeof(int));
	values[0] = val;
}

WriteState::WriteState(std::vector<int> val) : State(val.size(), 0) {
	values = (int*)malloc(size * sizeof(int));
	for(unsigned int i = 0; i < size; i++) values[i] = val.at(i);
}

WriteState::WriteState(int val[], int size) : State(size, 0) {
	values = (int*)malloc(size * sizeof(int));
	for(int i = 0; i < size; i++) values[i] = val[i];
}

WriteState::~WriteState() {
	free(values);
	values = NULL;
}

ReadState::ReadState(int *val) : State(1, 0) {
	values = val;
}
ReadState::ReadState(std::vector<int> *val) : State(val->size(), 0) {
	values = val->data();
}
ReadState::ReadState(int val[], int size) : State(size,0) {
	values = val;
}

ReadState::~ReadState() { }

