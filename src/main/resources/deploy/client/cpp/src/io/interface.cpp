/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "interface.h"
#include "../constants.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <math.h>
#include <iostream>

#include <netdb.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

#include "../exceptions.h"

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
		socketFD_send(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
//		socketFD_recv(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
				ip(ip), port(port) {

	setup();
}

ethernet::~ethernet() {
	teardown();
}

void ethernet::setup() {

	struct sockaddr_in stSockAddr;
	int Res;

#if DEBUG
	printf("setting up data socket @%s:%d ...", ip, port);
#endif /* DEBUG */

	// throw an exception, if socket creation faileds
	if (-1 == socketFD_send)
		throw mediumException(std::string("failed to create socket: ") +
						strerror(errno) + " (" + std::to_string(errno) + ")");

	// Initialize Socket memory
	memset(&stSockAddr, 0, sizeof(stSockAddr));

	// Connect the Input Socket
	stSockAddr.sin_family = AF_INET;
	stSockAddr.sin_port = htons(port);
	Res = inet_pton(AF_INET, ip, &stSockAddr.sin_addr);

	if (0 > Res){
//		close(sockedFD_send);
		teardown();
		throw mediumException("first parameter is not a valid address family");

	}
	else if (0 == Res){
//		close(sockedFD_send);
		teardown();
		throw mediumException("second parameter does not contain valid ip address");
	}

	if (-1 == connect(socketFD_send, (struct sockaddr *)&stSockAddr, sizeof(stSockAddr))){
//		close(socketFD_send);
		teardown();
		throw mediumException(std::string("failed to open Ethernet connection: ") +
				strerror(errno) + " (" + std::to_string(errno) + ")");
	}

//	// listening socket
//	struct addrinfo hints, *res;
//
//	// first, load up address structs with getaddrinfo():
//
//	memset(&hints, 0, sizeof hints);
//	hints.ai_family = AF_INET;
//	hints.ai_socktype = SOCK_STREAM;
//	hints.ai_flags = AI_PASSIVE; // fill in my IP for me
//
//	getaddrinfo("192.168.1.23", "8845", &hints, &res);
//
//	// make a socket:
//	sockedFD_recv = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
//
//	char ipstr[INET6_ADDRSTRLEN];
//	inet_ntop(res->ai_family, res->ai_addr, ipstr, sizeof ipstr);
//	printf("  %s\n", ipstr);


//	struct sockaddr_in my_addr;
//
//	socketFD_recv = socket(PF_INET, SOCK_STREAM, 0);
//
//	my_addr.sin_family = AF_INET;
//	my_addr.sin_port = htons(port2);     // short, network byte order
//	my_addr.sin_addr.s_addr = inet_addr("192.168.1.23");
//	memset(my_addr.sin_zero, '\0', sizeof my_addr.sin_zero);
//
//	// bind it to the port we passed in to getaddrinfo():
//	bind(sockedFD_recv, (struct sockaddr *)&my_addr, sizeof my_addr);
////	bind(socketFD_recv, res->ai_addr, res->ai_addrlen);
//
//	printf("  %s\n", inet_ntoa(my_addr.sin_addr));
//
//	// listen to the port
//	listen(socketFD_recv, 20);

	//everything else --> listening loop...

#if DEBUG
	printf(" done\n");
#endif /* DEBUG */
}

void ethernet::teardown() {
	// Disconnect the Socket
	if(close(socketFD_send) != 0) throw mediumException(
			std::string("failed to close Ethernet connection: ") +
			strerror(errno) + " (" + std::to_string(errno) + ")");
//	shutdown(socketFD_recv, SHUT_RDWR);
//	close(sockedFD_recv);
}

void ethernet::send(int val) {
	send(&val, 1);
}

void ethernet::send(int buf[], int size) {
	// print debug message
#if DEBUG
		printf("\nsending package of size %d with values: ", size);
		int i;
		for(i = 0; i < size; i++) {
			printf("%d", buf[i]);
			if(i < size-1) printf(", ");
		}
		printf(" ...");
#endif /* DEBUG */

	// write data
	if(write(socketFD_send, buf, size*4) < 0) throw mediumException(
			std::string("failed writing to socket: ") +
			strerror(errno) + " (" + std::to_string(errno) + ")");

	// print finishing debug message
#if DEBUG
	printf(" done");
#endif /* DEBUG */
}

void ethernet::send(std::vector<int> val) {
	// use array method
	send(val.data(), val.size());
}

void ethernet::readChar(char* val) {
    if(recv(socketFD_send, val, 1, 0) < 0) throw mediumException(
        std::string("failed reading from socket: ") +
        strerror(errno) + " (" + std::to_string(errno) + ")");
}

void ethernet::readInt(int *val) {
    unsigned int tmp = 0;
    int i = 0;
    *val = 0;

    while(i < 4) {
        int j = recv(socketFD_send, &tmp, 4-i, 0);
        if(i < 0) throw mediumException(
            std::string("failed reading from socket: ") +
            strerror(errno) + " (" + std::to_string(errno) + ")");
        i += j;

        *val = *val << (j*8);
        *val = *val | tmp;
    }
}

bool ethernet::waitForData(unsigned int timeout, unsigned int utimeout) {

	struct timeval tv;
	fd_set readfds;

	tv.tv_sec = timeout;
	tv.tv_usec = utimeout;

	FD_ZERO(&readfds);
	FD_SET(socketFD_send, &readfds);

	// wait for the timeout or data
	if(select(socketFD_send+1, &readfds, NULL, NULL, &tv) < 0) throw mediumException(
			std::string("failed while waiting for incoming messages: ") +
			strerror(errno) + " (" + std::to_string(errno) + ")");

	// if we have data, return true
	if (FD_ISSET(socketFD_send, &readfds)) return true;

	// else false
	return false;

	//	struct sockaddr_storage their_addr;
	//	socklen_t addr_size;
	//
	//	addr_size = sizeof their_addr;
	//	int n = accept(sockfd, (struct sockaddr *)&their_addr, &addr_size);
	//	if(n > 0) printf("\ngot a new socket!!!");
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
