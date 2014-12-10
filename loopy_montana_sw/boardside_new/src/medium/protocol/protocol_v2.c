/*
 * protocol_v0_2.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#include "protocol.h"

#if PROTO_VERSION == 2

#include <stdlib.h>
#include <stdio.h>

int decode_header(int first) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return 0;
}

struct Message* encode_ack(unsigned char pid, unsigned int count) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return NULL;
}

struct Message* encode_poll(unsigned char pid) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return NULL;
}

struct Message* encode_data(unsigned char pid, unsigned int size) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return NULL;
}

struct Message* encode_debug(unsigned char type, unsigned int size) {
    xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
    return NULL;
}

#endif /* PROTO_VERSION */
