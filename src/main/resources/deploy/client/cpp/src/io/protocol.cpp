/**
 * @author Thomas Fischer
 * @since 19.02.2013
 */

// header file
#include "protocol.h"

// standard library
#include <cmath>
#include <stdio.h>

// constants
#include "../constants.h"

// others
#include "io.h"

// protocol instance to be used
protocol *proto = new protocol_v1();

protocol::protocol() {}

protocol_v1::protocol_v1() {}

int protocol_v1::decode(int first) {
	printf("\nfound something to decode: %d", first);
	int version = floor(first / pow(2, 24));

	first = fmod(first, pow(2, 24));
	int type = floor(first / pow(2, 20));

	// set id as specified in protocol version 1
	first = fmod(first, pow(2, 20));
	unsigned char id = floor(first / pow(2, 16));

	// the last two bytes mark the size of this frame
	unsigned int size = fmod(first, pow(2, 16));

	if(DEBUG) printf("\nver : %d", version);
	if(DEBUG) printf("\ntype: %d", type);
	if(DEBUG) printf("\ntarg: %d", id);
	if(DEBUG) printf("\nsize: %d", size);

	// read size more bytes?

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
		if(size > 0) {
			// the size is given in byte
			int payload[size];
			unsigned int i = 0;

			// try to read a value from the medium
			while(i < size) if(intrfc->readInt(payload + sizeof(int)*i)) i++;

			// shift read values to the respective queue
			read(id, payload, size);
		}
		break;
	case 10: // This is a non-blocking poll.
		poll(id); break;
	case 11: // This is a blocking poll.
		break;
		// 12&13 are not assigned
	case 14: // This marks a GPIO message.
		recv_gpio(id, size); break;
	case 15: // This is an acknowledgment.
		acknowledge(id, size);
		break;
	default:
		printf("\nWARNING: unknown type %d for protocol version 1. The frame will be ignored.", type);
		return 1;
	}

	printf("\nfinished message interpretation");

	return 0;
}

std::vector<int> protocol_v1::encode_data(unsigned char pid, std::vector<int> val) {
	val.insert(val.begin(),construct_header(8, pid, val.size()));
	return val;
}

std::vector<int> protocol_v1::encode_gpio(unsigned char gid, unsigned char val) {
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

// construct a data header (what about other header types?? esp the gpio type?
//int protocol_v1::constructHeader(unsigned char cid, unsigned int size) {
//	return (  1 << 24) +  // protocol version
//           (  8 << 20) +  // message type (data)
//           (cid << 16) +  // target component
//            size;         // size of the data package
//}

//void protocol::printHeader(int header) {
//	int version;
//
//	// read the version information from the header
//	version = floor(header / pow(2, 24));
//}

void protocol_v1::printHeader(int header) {
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
		printf("  version     : 1\n");
		printf("  type        : %d\n", type);
		printf("  payload size: %d\n", size);
		break;
	default:
		printf("ERROR: unknown protocol version %d", version);
	}
}
