/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "port.h"
#include <iostream>

//in::~in() {
//
//}
//
//out::~out() {
//
//}
//
//dual::~dual() {
//
//}

bool in::write(int val[], int size) {
	return true;
}

bool in::write(int val) {
	return true;
}

std::istream& operator >>(std::istream &is,in &port) {
	return is;
}

bool out::read(int val[], int size) {
	int i;
	// TODO while this would work, it's probably inefficient considering the protocol overhead...
	for(i = 0; i < size; i++) val[i] = read();
	return true;
}

bool out::read(int *val) {
	*val = read();
	return true;
}

int out::read() {
	return 5;
}
