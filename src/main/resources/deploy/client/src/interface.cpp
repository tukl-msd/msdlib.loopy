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

// conversion from "boolean" array to single value
int convertToInt(bool values[8]) {

	if(DEBUG) printf("\n  converting 8-bit array to single value ...");

	int i, rslt = 0;
	for(i = 0; i < 8; i++)
		if(values[i]) rslt = rslt + (int)pow(2, i);

	if(DEBUG) printf(" done\n  hex value after conversion: 0x%X", rslt);

	return rslt;
}

/**
 * converts a single integer value to a binary number represented by a boolean array.
 * true marks a 1 value, false a 0 value.
 * @param size size of the binary number and therefore boolean array
 * @param values pointer to the array where the result will be stored
 * @param val the integer number which will be converted
 * @returns returns true if the conversion was successful, false otherwise
 */
bool convertToArr(int size, bool values[], int val) {
	if(DEBUG) printf("\n    converting single value to %d-bit array ...", size);

	// check, if given value is to large for specified array
	if(val >= (int)pow(2,size)) {
		if(DEBUG) printf("\n  ERROR: value was to large for given array");
		return 1;
	}

	// set values array accordingly
	int i;
	for(i = size-1; i >= 0; i--) {
		int cur = (int)pow(2,i);
		if(val / cur > 0) {
			values[i] = true;
			val -= cur;
		} else values[i] = false;
	}

	if(DEBUG) {
		printf(" done\n  value after conversion: [");
		for(i = 0; i < size; i++) printf(" %d", values[i]);
		printf(" ]");
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

/**
 * Minimal value for the LED test application.
 * This value marks the two least significant LEDs to be set.
 */
#define MIN_VALUE 3
/**
 * Maximal value for the LED test application.
 * This value marks the two most significant LEDs to be set.
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

//ethernet::ethernet(const char *ip, unsigned short int port) {
//	Data_SocketFD = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
//
//	this->ip = ip;
//	this->port = port;
//}
// new constructor using member initialisation list
ethernet::ethernet(const char *ip, unsigned short int port):
		Data_SocketFD(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
		ip(ip), port(port) {}

ethernet::~ethernet() {
	// TODO Auto-generated destructor stub
}

void ethernet::setup() {
	struct sockaddr_in stSockAddr;
	int Res;

	//	char *ip = "131.246.92.144";
	//	char *ip = "192.168.1.10";

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

// test application
void ethernet::test() {
	printf("starting hopp lwip led test ...\n");
	setup();

	int direction = 1, state = 3;
	while(1) {
		setLEDState(state);
		direction = next(direction, &state);
		usleep(175000);
	}
	teardown();

}

// all following procedures & methods are helpers for the test application

bool ethernet::writeValues(int buf[], int size) {

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
 * Sets the LED state using an array of integer values.
 * @param state The new LED state represented by an integer array of size 8.
 *              Each value has to be either 1 for a glowing LED or 0 otherwise.
 */
bool ethernet::setLEDStateByArr(bool state[8]) {
	int send [2];

	send[0] = constructHeader(1,0,1);
	send[1] = convertToInt(state);

	return writeValues(send, 8);
}

/**
 * Sets the LED state using a single integer value.
 * @param state The new LED state represented by a single integer value.
 *              The value has to be in the interval [0;255].
 */
bool ethernet::setLEDState(int state) {
	int send [2];

	send[0] = constructHeader(1,0,1);
	send[1] = state;

	return writeValues(send, 8);
}
