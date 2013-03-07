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
#include <math.h>

#include <netdb.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>


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
//		sockfd(socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)),
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
//	sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
//
//	char ipstr[INET6_ADDRSTRLEN];
//	inet_ntop(res->ai_family, res->ai_addr, ipstr, sizeof ipstr);
//	printf("  %s\n", ipstr);


//	struct sockaddr_in my_addr;
//
//	sockfd = socket(PF_INET, SOCK_STREAM, 0);
//
//	my_addr.sin_family = AF_INET;
//	my_addr.sin_port = htons(port2);     // short, network byte order
//	my_addr.sin_addr.s_addr = inet_addr("192.168.1.23");
//	memset(my_addr.sin_zero, '\0', sizeof my_addr.sin_zero);
//
//	// bind it to the port we passed in to getaddrinfo():
//	bind(sockfd, (struct sockaddr *)&my_addr, sizeof my_addr);
////	bind(sockfd, res->ai_addr, res->ai_addrlen);
//
//	printf("  %s\n", inet_ntoa(my_addr.sin_addr));
//
//	// listen to the port
//	listen(sockfd, 20);

	//everything else --> listening loop...

	printf(" done\n");
}

void ethernet::teardown() {
	// Disconnect the Socket
//	shutdown(Data_SocketFD, SHUT_RDWR);
	close(Data_SocketFD);
//	shutdown(sockfd, SHUT_RDWR);
//	close(sockfd);
}

bool ethernet::send(int val) {
	return send(&val, 1);
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
	return recv(Data_SocketFD, val, 4, 0) > 0;
}

bool ethernet::waitForData(int timeout) {

	struct timeval tv;
	fd_set readfds;

	tv.tv_sec = timeout;
	tv.tv_usec = 0;

	FD_ZERO(&readfds);
	FD_SET(Data_SocketFD, &readfds);

	select(Data_SocketFD, &readfds, NULL, NULL, &tv);

	if (FD_ISSET(Data_SocketFD+1, &readfds)) return true;
//	struct sockaddr_storage their_addr;
//	socklen_t addr_size;
//
//	addr_size = sizeof their_addr;
//	int n = accept(sockfd, (struct sockaddr *)&their_addr, &addr_size);
//	if(n > 0) printf("\ngot a new socket!!!");
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
