/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "port.h"
#include <iostream>
#include <stdio.h>

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
	printf("\nwriting %d", val);
	return true;
}

in& operator<< (in &a, const std::vector<int> val) {
	printf("\nit's a stream!");
	for(unsigned int i = 0; i < val.size(); i++) a.write(val.at(i));
	return a;
}

in& operator<< (in &a, const int val) {
	printf("\nit's a stream!");
	a.write(val);
	return a;
}

//in in::operator<< (int val) {
//	printf("\nit's a stream!");
//	write(val);
//	return *this;
//}

//template<typename T, size_t n>
//size_t array_size(const T (&)[n])
//{
//    return n;
//}
//in in::operator<< (int val[]) {
//	printf("\nit's an array stream! - i have no idea how to read from this!");
//	return *this;
//}

bool out::read(int val[], int size) {
	// TODO while this would work, it's probably inefficient considering the protocol overhead...
	for(int i = 0; i < size; i++) val[i] = read();
	return true;
}

bool out::read(int *val) {
	*val = read();
	return true;
}

int out::read() {
	return 5;
}

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
