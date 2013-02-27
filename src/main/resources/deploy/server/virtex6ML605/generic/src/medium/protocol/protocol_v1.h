/**
 * Dedicated protocol interpreter for protocol version 1.
 * This protocol version supports the three basic gpio components as well as several AXI stream ports.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_V1_H_
#define PROTOCOL_V1_H_

#include "../message.h"

/**
 * Interprets a protocol header of a message with protocol version 1.
 * @return 0 if successful, 1 if errors occurred.
 */
int decode_header_v1(int first);

struct Message* encode_ack_v1(unsigned char pid, unsigned int count);

struct Message* encode_poll_v1(unsigned char pid);

struct Message* encode_data_v1(unsigned char id, unsigned int size);

#endif /* PROTOCOL_V1_H_ */
