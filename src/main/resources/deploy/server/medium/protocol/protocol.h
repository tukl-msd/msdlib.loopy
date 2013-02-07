/**
 * Generic protocol interpreter.
 * Delegates calls to the respective protocol interpreter for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

//#include "lwip/init.h"
#include "lwip/pbuf.h"
//#include "netif/xadapter.h"

/**
 * Delegates calls to the respective protocol interpreter for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * @param version The version of the incoming message header.
 * @return 0 if successful, 1 otherwise.
 */
int decode_header( unsigned char version );
//int decode ( struct pbuf *p);

#endif /* PROTOCOL_H_ */
