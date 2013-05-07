/*
 * protocol_v0_2.h
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */


#ifndef PROTOCOL_V2_H_
#define PROTOCOL_V2_H_

#include "../message.h"

int decode_header_v2(int first);

struct Message* encode_ack_v2(unsigned char pid, unsigned int count);

struct Message* encode_poll_v2(unsigned char pid);

struct Message* encode_data_v2(unsigned char pid, unsigned int size);

struct Message* encode_debug_v2(unsigned char type, unsigned int size);

#endif /* PROTOCOL_V2_H_ */
