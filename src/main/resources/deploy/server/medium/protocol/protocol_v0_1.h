/**
 * Protocol interpreter version 0.1.
 * This protocol version supports the three basic gpio components as well as several AXI stream ports.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_V0_1_H_
#define PROTOCOL_V0_1_H_

#include "lwip/pbuf.h"

/**
 * Interprets a protocol header of a message with protocol version 0.1.
 * @param p The message to be interpreted.
 * @return 0 if successful, 1 if errors occurred.
 */
int decode_v0_1( struct pbuf *p );

#endif /* PROTOCOL_V0_1_H_ */
