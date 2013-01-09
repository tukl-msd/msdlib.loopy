//============================================================================
// Name        : lwipClient.cpp
// Author      :
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

//#include <iostream>
#include <stdio.h>
//#include <time.h>
//#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <math.h>
#include <errno.h>

/**
 * @file
 */

//int busyWait(int wait_time) {
//
//	long time1 = clock(),
//	time2 = clock();
//
////	int i =0;
//
//	// TODO busy waiting... \o/
////	while(1) {
//
//		// wait
//		while(time2 - time1 < wait_time) time2 = clock();
//
////		printf("done waiting");
////		i++;
////		printf("%i\n", i);
//		// reset timers
////		time1 = clock();
////		time2 = clock();
////	}
//
//	return 0;
//}


// These are the defines as used in MicroBlaze net2axis application.
// Make sure to use them correct, ex. input on MB is our output etc.

#define NW_DATA_PORT 8844
//#define ip "172.16.13.144";

#define DEBUG 1

/**
 * The data socket.
 * @deprecated because it's not necessary
 */
int Data_SocketFD;

/**
 * The setup method. This is a longer description of the method.
 * @deprecated no longer required
 * @return 0 if success, 1 otherwise
 * @param data_SocketFD pointer to the data socket
 */
int setup(int *Data_SocketFD) {
	struct sockaddr_in stSockAddr;
	int Res;
	char *ip = "131.246.92.144";
//	char *ip = "192.168.1.10";

	if(DEBUG) printf("setting up data socket @%s:%d ...", ip, NW_DATA_PORT);

	if (-1 == *Data_SocketFD){ //|| -1 == Config_SocketFD){
		printf(" failed to create socket");
		exit(EXIT_FAILURE);
	}

	// Initialize Socket memory
	memset(&stSockAddr, 0, sizeof(stSockAddr));

	// Connect the Input Socket
	stSockAddr.sin_family = AF_INET;
	stSockAddr.sin_port = htons(NW_DATA_PORT);
	Res = inet_pton(AF_INET, ip, &stSockAddr.sin_addr);

	if (0 > Res){
		printf(" error: first parameter is not a valid address family");
		close(*Data_SocketFD);
		exit(EXIT_FAILURE);
	}
	else if (0 == Res){
		printf(" char string (second parameter does not contain valid ip address)");
		close(*Data_SocketFD);
		exit(EXIT_FAILURE);
	}

	if (-1 == connect(*Data_SocketFD, (struct sockaddr *)&stSockAddr, sizeof(stSockAddr))){
		printf(" connect failed: %s (%d)", strerror(errno), errno);
//		printf("errorcode: %d", errno);
		close(*Data_SocketFD);
		exit(EXIT_FAILURE);
	}

	printf(" done\n");

	return 0;
}

// conversion from "boolean" array to single value
int convertToInt(int values[8]) {

	if(DEBUG == 1) printf("\n  converting 8-bit array to single value ...");

	int i, rslt = 0;
	for(i = 0; i < 8; i++)
		if(values[i] != 0) rslt = rslt + (int)pow(2, i);

	if(DEBUG == 1) printf(" done\n  hex value after conversion: 0x%X", rslt);

	return rslt;
}

// conversion from single int value to a "boolean" array (1 = true, 0 = false)
/**
 * Writes \a count bytes from \a buf to the filedescriptor \a fd.
 * @param size the size. really!
 */
int convertToArr(int size, int values[], int dataRead) {
	if(DEBUG == 1) printf("\n    converting single value to %d-bit array ...", size);

	// check, if given value is to large for specified array
	if(dataRead >= (int)pow(2,size)) {
		if(DEBUG == 1) printf("\n  ERROR: value was to large for given array");
		return 1;
	}

	// set values array accordingly
	int i;
	for(i = size-1; i >= 0; i--) {
		int cur = (int)pow(2,i);
		if(dataRead / cur > 0) {
			values[i] = 1;
			dataRead -= cur;
		} else values[i] = 0;
	}

	if(DEBUG == 1) {
		printf(" done\n  value after conversion: [");
		for(i = 0; i < size; i++) printf(" %d", values[i]);
		printf(" ]");
	}

	return 0;
}

int writeValues(int buf[], int size) {

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
		return 1;
	}

	if(DEBUG) printf(" done");

	return 0;
}

int readInt(int buf[], int size) {

	if(DEBUG) printf("\nreading package of size %d ... ", size);

	// read data
	if(read(Data_SocketFD, buf, size*4) < 0) {
		// catch read errors
		if(DEBUG) printf(" ERROR reading from socket");
		return 1;
	}

	if(DEBUG) {
		printf(" values: ");
		int i;
		for(i = 0; i < size; i++) {
			printf("%d", buf[i]);
			if(i < size-1) printf(", ");
		}
	}

	return 0;
}

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

int setLEDStateByArr(int state[8]) {
	int send [2];

	send[0] = constructHeader(1,0,1);
	send[1] = convertToInt(state);

	return writeValues(send, 8);
}

int setLEDState(int state) {
	int send [2];

	send[0] = constructHeader(1,0,1);
	send[1] = state;

	return writeValues(send, 8);
}

#define MIN_VALUE 3

/**
 * Short description.
 * Longer description of this definition.
 */
#define MAX_VALUE 192

/** Sets the next LED out of the current LED state and a direction. The next
 * LED state is considered to be the current state shifted one LED into direction
 * if possible, otherwise shifted in the opposite direction.
 *
 * @param direction the current direction, into which the LEDs should are shifted.
 *        0 will shift to the right, 1 will shift to the left (I guess)
 * @return the direction for the next step of LED shifting.
 * @param state pointer to the current LED state. The state will be changed by
 *        this procedure
 * @return nope - just trolling... no second return value
 */
int next(int direction, int * state) {
	if(direction) {
		if(*state == MAX_VALUE) return next(!direction, state);
		*state = *state * 2;
	} else {
		if(*state == MIN_VALUE) return next(!direction, state);
		*state = *state / 2;
	}
	return direction;
}

int main(void){
	printf("starting lwip echo test ...\n");

	Data_SocketFD = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

	// setup data socket
	setup(&Data_SocketFD);

	// setup send and receive buffers;
//	int n, send[10], rcv[1];
//	int n, send[10];
//	for(n = 0 ; n<10; n++) send[n] = n;
//	send[7] = 23;
//
//
//	send[0] = constructHeader(1,0,1);
//	unsigned int a = 170;
//	send[1] = a;
	// perform read write operations ...
//	writeValues(send, 8);
//	int a [8] = {1,0,0,0,1,0,0,0};
//	setLEDState(a);

//	int state [8] = {1,1,0,0,0,0,0,0};
//	int direction = 1;
//	while(1) {
//		setLEDState(state);
//		direction = next(direction, state);
//		usleep(125000);
//	}
	int direction = 1, state = 3;
	while(1) {
		setLEDState(state);
		direction = next(direction, &state);
		usleep(175000);
	}

	// Disconnect the Socket
	shutdown(Data_SocketFD, SHUT_RDWR);
	close(Data_SocketFD);

	return 0;
}

/*class a {
	public:
	*
	 * A really cool method. Returning 5.
	 * @return 5. five. FIVE!
	 *
	int m() {
		return 5;
	}
};*/




