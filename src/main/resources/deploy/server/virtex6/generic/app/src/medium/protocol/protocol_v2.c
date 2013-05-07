/*
 * protocol_v0_2.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */
#include "protocol_v2.h"

int decode_header_v2(int first) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return -1;
}

struct Message* encode_ack_v2(unsigned char pid, unsigned int count) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return -1;
}

struct Message* encode_poll_v2(unsigned char pid) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return -1;
}

struct Message* encode_data_v2(unsigned char pid, unsigned int size) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return -1;
}

struct Message* encode_debug_v2(unsigned char type, unsigned int size) {
	xil_printf(" \nERROR: protocol version 2 is not implemented.\n");
	return -1;
}
