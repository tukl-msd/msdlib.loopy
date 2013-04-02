/**
 * @author Thomas Fischer
 * @since 19.02.2013
 */

// header file
#include "protocol.h"

// standard library
#include <cmath>
#include <stdio.h>
#include <iostream>

// constants
#include "../constants.h"

// others
#include "io.h"
#include "../exceptions.h"

// protocol instance to be used
protocol *proto = new protocol_v1();

protocol::protocol() {}

protocol_v1::protocol_v1() {}

unsigned int protocol_v1::max_size() {
	return MAX_SIZE;
}

void protocol_v1::decode(int first) {
	int version = floor(first / pow(2, 24));

	// check if the version fits this decoder
	if(version != 1) throw protocolException("unknown protocol version " + version);

	first = fmod(first, pow(2, 24));
	int type = floor(first / pow(2, 20));

	// set id as specified in protocol version 1
	first = fmod(first, pow(2, 20));
	unsigned char id = floor(first / pow(2, 16));

	// the last two bytes mark the size of this frame
	unsigned int size = fmod(first, pow(2, 16));

	if(DEBUG) {
		printf("\ndecoded the following message header: %d", first);
		printf("\n  version : %hhu", version);
		printf("\n  type    : %hhu", type);
		printf("\n  target  : %hhu", id);
		printf("\n  size    : %d", size);
	}


	std::cout.flush();

	// 8 bit protocol version
	// 4 bit message type
	//   0xxx Config related
	//     0000 "Soft Reset"
	//     0111 Error
	//   1xxx Data related
	//     1000 Data Non-Blocking
	//     1001 Data Blocking
	//     1010 Poll Non-Blocking
	//     1011 Poll Blocking
	//     1110 GPIO
	//     1111 ACK
	// 4 bit component identifier
	// 16 bit size or value, depending on type
	// <size> bytes data, depending on type

	// encoding v2
	// 00xx config related
	//  0000 soft reset    0
	//  0011 info          3
	// 01xx data related
	//  0100 data          4
	//  0101 gpio          5
	//  0110 poll          6
	//  0111 ack           7
	switch(type) {
	case  0: // This is a soft reset.
			 // receiving a soft reset from the board indicates, that the board performed a successful reset.
		     // signal the application (since reset() is a blocking call)
		break;
		// 1-6 are not assigned
	case  7: // This is an error message. It should be stored in some error queue (throw it, if the call was synchronous?)
		break;
	case  8: // This is a non-blocking data package.
		     // In this case we actually have to read the content of the message ;)
	case  9: // This is a blocking data package.
		if(size == 0) break;
		else {
			// check if the pid is in range
			if(id > OUT_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
					std::to_string(id) + ") of received data message exceeded count of out-going ports (" +
					std::to_string(OUT_PORT_COUNT) + ")");

			// the size is given in byte
			int payload[size];
			unsigned int i = 0;

			std::cout.flush();

			// try to read a value from the medium
			for(i = 0; i < size; i++)
				try { //if(intrfc->waitForData(0, 250000)) {
					intrfc->readInt(payload + i);
//					intrfc->readInt(&payload[i]);
				//}
			} catch(mediumException &e) {
				// TODO is it a good idea, to ignore read "exceptions" here??
				// what does it mean, to have an exception here?
				// there should be another value according to size, but it can't be read from the medium currently
				// this might happen, if...
				// a) the medium is really slow and we loop faster than values arrive
				// b) considering e.g. Ethernet, the message is split into multiple layer 3 messages
				//    and the next one hasn't arrived yet
				// c) broken board-side driver didn't send all the values specified in the size field...
				// d) broken medium (lost connection for some reason)
				// while a) and b) fix themselves by ignoring this message, c) and d) do not...
				printf("%s", e.what());
				// SOLUTION for d): perform a short wait and die over there, if something is wrong with the medium
				// this however does NOT solve c) - buggy drivers will not send enough values...
				//  this probably is a stupid use-case though. We do not look at attackers...
				intrfc->waitForData(0, 250000);
			}

			std::cout.flush();

			// shift read values to the respective queue
			read(id, payload, size);
		}
		break;
	case 10: // This is a non-blocking poll.
		if(id > IN_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
				std::to_string(id) + ") of received poll message exceeded count of in-going ports (" +
				std::to_string(IN_PORT_COUNT) + ")");
		poll(id); break;
	case 11: // This is a blocking poll.
		break;
		// 12&13 are not assigned
	case 14: // This marks a GPIO message.
		if(id > GPI_COUNT-1) throw protocolException(std::string("GPIO id value (") +
				std::to_string(id) + ") of received GPIO message exceeded count of GPIO input devices (" +
				std::to_string(GPI_COUNT) + ")");
		recv_gpio(id, size); break;
	case 15: // This is an acknowledgment.
		if(id > IN_PORT_COUNT-1) throw protocolException(std::string("pid value (") +
				std::to_string(id) + ") of received acknowledgment exceeded count of in-going ports (" +
				std::to_string(IN_PORT_COUNT) + ")");
		acknowledge(id, size);
		break;
	default:
		throw protocolException(std::string("unknown message type (") + std::to_string(type) +
				") for protocol version 1");
	}

	if(DEBUG) printf("\nfinished message interpretation");
}

std::vector<int> protocol_v1::encode_data(unsigned char pid, std::vector<int> val) {
	// check value size
	if(val.size() > MAX_SIZE) throw protocolException(std::string("actual message size (") +
			std::to_string(val.size()) + ") exceeded message capacity (" + std::to_string(MAX_SIZE) + ")");
	// check port id
	if(pid > IN_PORT_COUNT-1) throw protocolException(std::string("port id (") +
			std::to_string(pid) + ") exceeded port range for in-going ports (" + std::to_string(IN_PORT_COUNT) + ")");

	// append header and return
	val.insert(val.begin(),construct_header(8, pid, val.size()));
	return val;
}

std::vector<int> protocol_v1::encode_gpio(unsigned char gid, unsigned char val) {
	// check port id
	if(gid > GPI_COUNT-1) throw protocolException(std::string("GPIO id (") +
			std::to_string(gid) + ") exceeded GPIO output  device range (" + std::to_string(GPI_COUNT) + ")");

	// construct message and return
	std::vector<int> v;
	v.push_back(construct_header(14, gid, val));
	return v;
}

std::vector<int> protocol_v1::encode_reset() {
	std::vector<int> v;
	v.push_back(construct_header(0, 0, 0));
	return v;
}

int protocol_v1::construct_header(unsigned char type, unsigned char id, unsigned int size) {
	return (1 << 24) + (type << 20) + (id << 16) + size;
}
