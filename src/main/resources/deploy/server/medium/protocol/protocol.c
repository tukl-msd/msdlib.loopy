/*
 * protocol.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
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

#include "alignment.h"
#include "protocol.h"
#include "protocol_v0_1.h"
#include "protocol_v0_2.h"
#include <math.h>

int decode( struct pbuf *p ) {

	int header = get_unaligned((void*)(((int)p->payload)));
	int version = floor(header / pow(2, 24));
	switch (version) {
	case 1: decode_v0_1(p); break;
	case 2: decode_v0_2(p); break;
	default:
		// TODO return some exception over the medium?
		xil_printf("unknown protocol version %d", version);
		break;
	}

	return 1;
}
