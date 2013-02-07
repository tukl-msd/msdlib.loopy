/**
 * Protocol interpreter version 0.1.
 * This protocol version supports the three basic gpio components as well as several AXI stream ports.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_V1_H_
#define PROTOCOL_V1_H_

#include "lwip/pbuf.h"

/**
 * Interprets a protocol header of a message with protocol version 1.
 * @return 0 if successful, 1 if errors occurred.
 */
int decode_header_v1(int first);
//int decode_v0_1( struct pbuf *p );
#endif /* PROTOCOL_V1_H_ */
