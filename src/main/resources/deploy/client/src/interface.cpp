/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "interface.h"
#include "constants.h"

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

int constructHeader(int version, int type, int size) {
	return (version * pow(2, 24)) + (type * pow(2, 16)) + size;
}

// TODO yes, in theory a single int is not sufficient. We need the complete buffer here
int decodeHeader(int header) {
	int version, type, size;

	// read the version information from the header
	version = floor(header / pow(2, 24));

	// remove the version information to retain the rest
	header = fmod(header, pow(2, 24));

	// now do some case distinction based on the received number. Currently, there only is version 1 ;)
	// Backward compatibility probably isn't too much of an issue for this, but it's always better to be prepared...
	switch(version) {
	case 1:
		type = floor(header / pow(2, 16));
		size = fmod(header, pow(2, 16));
		printf("decoded the following header\n");
		printf("  version     : %d\n", version);
		printf("  type        : %d\n", type);
		printf("  payload size: %d\n", size);

		break;
	default:
		printf("ERROR: unknown protocol version %d", version);
		return 1;
	}

	return 0;
}

interface::interface() {
	refCount = 0;
}

void interface::incRef() {
	refCount ++;
}

void interface::decRef() {
	refCount --;
	if(refCount == 0) {
		// if the ref counter has reached zero, disconnect
		teardown();
		// and remove this object
		delete this;
	}
}

bool interface::setLEDState(int state) {
	int val [2];

	val[0] = constructHeader(1,0,1);
	val[1] = state;

	return send(val, 8);
}

// new constructor using member initialisation list
ethernet::ethernet(const char *ip, unsigned short int port) :
		Data_SocketFD(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
		ip(ip), port(port) {
	setup();
}

ethernet::~ethernet() {
	// TODO Auto-generated destructor stub
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
		if(DEBUG) printf(" ERROR writing to socket");
		return false;
	}

	if(DEBUG) printf(" done");

	return true;
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

/**
 * Sets the LED state using a single integer value.
 * @param state The new LED state represented by a single integer value.
 *              The value has to be in the interval [0;255].
 */
//bool ethernet::setLEDState(int state) {
//	int send [2];
//
//	send[0] = constructHeader(1,0,1);
//	send[1] = state;
//
//	return writeValues(send, 8);
//}
