/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "interface.h"
#include "../constants.h"

#include <stdio.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <math.h>
#include <errno.h>

#define LED_SETTING 0
#define SWITCH_POLL 1
#define BUTTON_POLL 2

interface::interface() {
	refCount = 0;
}

void interface::incRef() {
	refCount ++;
}

void interface::decRef() {
	refCount --;
	// remove the object, if the refcount reaches 0
	if(refCount == 0) delete this;
}

// new constructor using member initialisation list
ethernet::ethernet(const char *ip, unsigned short int port) :
		Data_SocketFD(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
		ip(ip), port(port) {
	setup();
}

ethernet::~ethernet() {
	teardown();
}

void ethernet::setup() {

	struct sockaddr_in stSockAddr;
	int Res;

	//	char *ip = "131.246.92.144";
	//  char *ip = "192.168.1.10";

	if(DEBUG) printf("setting up data socket @%s:%d ...", ip, port);

	if (-1 == Data_SocketFD){ //|| -1 == Config_SocketFD){
		printf(" failed to create socket");
		exit(EXIT_FAILURE);
	}

	// Initialize Socket memory
	memset(&stSockAddr, 0, sizeof(stSockAddr));

	// Connect the Input Socket
	stSockAddr.sin_family = AF_INET;
	stSockAddr.sin_port = htons(port);
	Res = inet_pton(AF_INET, ip, &stSockAddr.sin_addr);

	if (0 > Res){
		printf(" error: first parameter is not a valid address family");
		close(Data_SocketFD);
		exit(EXIT_FAILURE);
	}
	else if (0 == Res){
		printf(" char string (second parameter does not contain valid ip address)");
		close(Data_SocketFD);
		exit(EXIT_FAILURE);
	}

	if (-1 == connect(Data_SocketFD, (struct sockaddr *)&stSockAddr, sizeof(stSockAddr))){
		printf(" connect failed: %s (%d)", strerror(errno), errno);
		close(Data_SocketFD);
		exit(EXIT_FAILURE);
	}

	printf(" done\n");
}

void ethernet::teardown() {
	// Disconnect the Socket
	shutdown(Data_SocketFD, SHUT_RDWR);
	close(Data_SocketFD);
}

bool ethernet::send(int val) {
	return false;
}

// all following procedures & methods are helpers for the test application

bool ethernet::send(int buf[], int size) {

	if(DEBUG) {
		printf("\nsending package of size %d with values: ", size);
		int i;
		for(i = 0; i < size; i++) {
			printf("%d", buf[i]);
			if(i < size-1) printf(", ");
		}
		printf(" ...");
	}

	// write data
	if(write(Data_SocketFD, buf, size*4) < 0) {
		// catch write errors
		if(DEBUG) {
			printf(" ERROR writing to socket");
			perror(NULL);
		}
		return false;
	}

	if(DEBUG) printf(" done");

	return true;
}

// lazy version... probably to many conversions between vectors and arrays currently ;)
bool ethernet::send(std::vector<int> val) {
	int buf[val.size()];

	for(unsigned int i = 0; i < val.size(); i++) {
		buf[i] = val.at(i);
	}
	return send(buf, val.size());
}

bool ethernet::readInt(int buf[], int size) {

	if(DEBUG) printf("\nreading package of size %d ... ", size);

	// read data
	if(read(Data_SocketFD, buf, size*4) < 0) {
		// catch read errors
		if(DEBUG) printf(" ERROR reading from socket");
		return false;
	}

	if(DEBUG) {
		printf(" values: ");
		int i;
		for(i = 0; i < size; i++) {
			printf("%d", buf[i]);
			if(i < size-1) printf(", ");
		}
	}

	return true;
}

bool ethernet::readInt(int *val) {
	return read(Data_SocketFD, val, 4) < 0;
}

bool ethernet::waitForData(int timeout) {

	struct timeval tv;
	fd_set readfds;

	tv.tv_sec = timeout;
	tv.tv_usec = 0;

	FD_ZERO(&readfds);
	FD_SET(Data_SocketFD, &readfds);

	select(Data_SocketFD+1, &readfds, NULL, NULL, &tv);

	if (FD_ISSET(Data_SocketFD, &readfds)) return true;

	return false;
}

//uart::uart() {
//	setup();
//}
//
//uart::~uart() {
//	teardown();
//}
//
//bool uart::send(int val) {
//	return false;
//}
//
//bool uart::send(int val[], int size) {
//	return false;
//}
//
//void uart::setup() {
//	printf("\nsetup uart");
//}
//
//void uart::teardown() {
//	printf("\nteardown uart");
//}
