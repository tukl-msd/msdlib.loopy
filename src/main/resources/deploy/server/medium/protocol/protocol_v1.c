/**
 * Dedicated protocol interpreter for protocol version 1.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include <math.h>
#include "protocol_v1.h"
#include "../../constants.h"

// forward declarations
int recv_int();
void set_LED(u32 state);
void xil_printf(const char *ctrl1, ...);

//int decode_v0_1( struct pbuf *p ) {
//
////	int header = get_unaligned((void*)(((int)p->payload)));
//
//	int header = get_unaligned(p->payload);
//	xil_printf("\n%d", header);
//
//	int type, size;
//
//	// read the version information from the header
//	if(floor(header / pow(2, 24)) != 1) {
//		// if this happens, the delegation failed!
//		xil_printf("ERROR: fatal error during protocol delegation");
//		return 1;
//	}
//
//	// remove the version information to retain the rest
//	header = fmod(header, pow(2, 24));
//
//	// set type and size as specified in header type 1
//	type = floor(header / pow(2, 16));
//	size = fmod(header, pow(2, 16));
//
//	// TODO: handle this with flags? lowest 16 bit for source/target,
//	//       17th & 18th bit for type (master, slave, read, write, gpio? not sure what is possible)
//	switch(type) {
//	case 0:
//		// This message type marks setting of the LED state in protocol version 1.
//		// We COULD directly use the size for this... or we require size one and then read only the lowest bits...
//		// QUESTION: size = byte or word? since word is dependent on the architecture, it is probably better to use bytes...
//		// QUESTION: is it possible to send "single" bytes? yes, unsigned chars. BUT: alignment methods are currently not made for this...
//		if(size != 1) {
//			xil_printf("ERROR: wrong payload for LED state (has to be exactly one)");
//			return 1;
//		}
//		// read the first byte of the "data" buffer
//		unsigned int a = get_unaligned(p->payload + 4);
//		if(a > 255) {
//			xil_printf("ERROR: to large number for LED state (255 is max)");
//			return 1;
//		}
//
//		set_LED(a);
//		break;
//	case 1:
//		// This message type marks poll of the Switch state in protocol version 1.
//		break;
//	case 2:
//		// This message type marks poll of the Button state in protocol version 1.
//		break;
//	default:
//		xil_printf("ERROR: unknown type %d for protocol version 0.1", type);
//		return 1;
//	}
//
//	size = fmod(header, pow(2, 16));
//
//	xil_printf("received package (version 0.1, type: %d, size: %d)\n", type, size);
//
//	return 0;
//
//}

/**
 * Decode a header version 1.
 * Reads parts of the message from the medium using recv_int().
 */
int decode_header_v1(int first) {
	if(DEBUG) xil_printf("\n  reading message type ...");

//	int first = recv_int();

	// remove the version information to retain the rest
	first = fmod(first, pow(2, 24));

	// set type and size as specified in first type 1
	int type = floor(first / pow(2, 16));

	if(DEBUG) xil_printf(" %d\n  reading message size ...", type);

	// the next two bytes mark the size of this frame
	int size = fmod(first, pow(2, 16));

	if(DEBUG) xil_printf(" %d", size);

	// assumption: size is in byte...
	int payload[size];

	if(size > 0) {
		if(DEBUG) xil_printf("\n  reading payload ...");

		// read size bytes
		int i;
		for(i = 0; i < size; i++) {
			payload[i] = recv_int();
			if(DEBUG) xil_printf("\n    %d", payload[i]);
		}
	}

	// TODO should do this with flags - something like...
	// 0 1 2 3 4 5 6 7
	// bit 0-1 mark the message type...
	//  0: send to port
	//  1: recv from port
	//  2: gpio send/request
	//  3: reserved for future use...
	// bit 2 is reserved for future use...
	// bits 3-7 are reserved for addressing... (--> 32 addresses)

	// depending on the message type, distribute the payload
	switch(type) {
	case 0:
		// This message type marks setting of the LED state in protocol version 1.
		// We COULD directly use the size for this... or we require size one and then read only the lowest bits...
		// QUESTION: size = byte or word? since word is dependent on the architecture, it is probably better to use bytes...
		// QUESTION: is it possible to send "single" bytes? yes, unsigned chars.
		//           BUT: alignment methods are currently not designed for this...

		// reset led state to 0 for messages with size 0
		if(size == 0) {
			if(DEBUG) xil_printf("\nWARNING: no payload given for setting LED state. Will reset LED state to 0.");
			set_LED(0); break;
		}

		// print a warning for larger payloads (only use the first byte)
		if(size > 1 && DEBUG) xil_printf("\nWARNING: to large payload for setting LED state. Everything but the first byte will be ignored.");

		// read the first byte of the "data" buffer
		set_LED(payload[0]);
		break;
	case 1:
		// This message type marks poll of the Switch state in protocol version 1.
		break;
	case 2:
		// This message type marks poll of the Button state in protocol version 1.
		break;
	default:
		if(DEBUG) xil_printf("\nWARNING: unknown type %d for protocol version 1. The frame will be ignored.", type);
		return 1;
	}

	if(DEBUG) xil_printf("\nfinished message interpretation");

	return 0;
}

