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

//int decode( struct pbuf *p) {
//	if(DEBUG) xil_printf("\nEncountered byte. Trying to read as message header\n  reading protocol version ...");
//	int header = get_unaligned(p->payload);
//	int version = floor(header / pow(2, 24));
//	int val = get_unaligned(p->payload + 4);
//	switch(version) {
//	case 1: decode_header_v1(header, val); break;
//	case 2: decode_header_v2(); break;
//	default:
//		if(DEBUG) {
//			xil_printf("\nWARNING: Protocol version %d is unknown to this driver version. The byte will be ignored.", version);
//			xil_printf("\nWARNING: Ignoring byte can lead to later bytes being interpreted as message headers!");
//		}
//	}
//	if(DEBUG) xil_printf("\n");
//	return 0;
//}
