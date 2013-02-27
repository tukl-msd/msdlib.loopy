/**
 * Generic protocol decoder.
 * Delegates calls to the respective protocol decoder for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

#include "lwip/pbuf.h"

/**
 * Delegates calls to the respective protocol decoder for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * @param first The first integer of the incoming message.
 * @return 0 if decoding was successful, 1 otherwise.
 */
int decode_header( int first );

/**
 * Delegates calls to the respective protocol encoder for the protocol version the message should be encoded with.
 * @param id Id of the component, the message was sent from.
 * @param size Size of the message to be sent
 * @return The encoded header.
 */
int endode_header_axi(unsigned char id, unsigned int size);

#endif /* PROTOCOL_H_ */
