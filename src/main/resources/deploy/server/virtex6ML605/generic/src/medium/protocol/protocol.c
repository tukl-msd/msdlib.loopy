/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

// TODO
// I would LOVE to see some sort of polymorphism here...
// - the main "class" should include all other protocol classes defined here (which are basically versions of the protocol)
// - these protocol "classes" contain specific information on how to decode a protocol header and where to distribute its contents
// - the main "class" calls more specialised "classes" according to the read protocol value
//
// Simple solution: append version string to files and methods and delegate from the this source file
//                  - advantages: IT WORKS
//                  - disadvantages: need to include version strings in ALL files and procedures...
// namespaces??

#include "protocol.h"
#include "protocol_v1.h"
#include "protocol_v2.h"
#include <math.h>
#include "../../constants.h"

/**
 * Interprets the header of messages and delegates its contents accordingly.
 * This procedure only delegates the call to a dedicated interpreter depending on the received version number.
 */
int decode_header( int first ) {
	unsigned char version = floor(first / pow(2,24));
	if(DEBUG) xil_printf("\nInteger received. Trying to interpret as message header (protocol version %d)", version);
	switch(version) {
	case 1: decode_header_v1(first); break;
	case 2: decode_header_v2(first); break;
	default:
		if(DEBUG) {
			xil_printf("\nWARNING: Protocol version %d is unknown to this driver version. The byte will be ignored.", version);
			xil_printf("\nWARNING: Ignoring byte can lead to later bytes being interpreted as message headers!");
		}
	}
	if(DEBUG) xil_printf("\n");
	return 0;
}

struct Message* encode_ack(unsigned char pid, unsigned int count) {
	switch(PROTO_VERSION) {
	case 1: return encode_ack_v1(pid, count);
	case 2: return encode_ack_v2(pid, count);
	default:
		if(DEBUG) xil_printf("\nWARNING: Unknown protocol version %d. Will use default protocol %d.", PROTO_VERSION, 1);
		return encode_ack_v1(pid, count);
	}
}

struct Message* encode_poll(unsigned char pid) {
	switch(PROTO_VERSION) {
	case 1: return encode_poll_v1(pid);
	case 2: return encode_poll_v2(pid);
	default:
		if(DEBUG) xil_printf("\nWARNING: Unknown protocol version %d. Will use default protocol %d.", PROTO_VERSION, 1);
		return encode_poll_v1(pid);
	}
}

struct Message* encode_data(unsigned char pid, unsigned int size) {
	switch(PROTO_VERSION) {
	case 1: return encode_data_v1(pid, size);
	case 2: return encode_data_v2(pid, size);
	default:
		if(DEBUG) xil_printf("\nWARNING: Unknown protocol version %d. Will use default protocol %d.", PROTO_VERSION, 1);
		return encode_data_v1(pid, size);
	}
}
