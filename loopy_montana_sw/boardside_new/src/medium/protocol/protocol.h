/**
 * Protocol encoder and decoder.
 * Delegates calls to the respective protocol decoder for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * Calls to encoder procedures are delegated to an encoder of the protocol version
 * stored in the constants file (which has been specified by the user).
 * @file
 * @author Thomas Fischer
 * @author Mathias Weber
 * @since 01.02.2013
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

#include "../../constants.h"
#include "../message.h"

#if PROTO_VERSION == 1
#include "protocol_v1.h"
#elif PROTO_VERSION == 2
#include "protocol_v2.h"
#else
#include "protocol_v1.h"
#define PROTO_VERSION 1
#endif /* PROTO_VERSION */

/**
 * Delegates calls to the respective protocol decoder for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * This method will issue further reads on the medium, depending on the message.
 * @param first The first integer of the message (which HAS to be part of the header).
 * @return 0 if decoding was successful, 1 otherwise.
 */
int decode_header( int first );

/**
 * Delegates calls to the respective protocol encoder for the protocol version the acknowledgment should be encoded with.
 * @param pid  Id of the port, which acknowledges data.
 * @param count Number of (integer) values, that are acknowledged. Has to be below protocol_max_size!
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_ack(unsigned char pid, unsigned int count);

/**
 * Delegates calls to the respective protocol encoder for the protocol version the poll should be encoded with.
 * @param pid  Id of the port, which is polled.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_poll(unsigned char pid);

/**
 * Delegates calls to the respective protocol encoder for the protocol version the gpio data message should be encoded with.
 * @param gid Id of the gpio component.
 * @param val The new state of the gpio component.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_gpio(unsigned char gid, unsigned char val);

/**
 * Delegates calls to the respective protocol encode for the protocol version the debug message should be encoded with.
 * @param type Debug type of the message.
 * @param size Size of the debug message. Has to be below protocol_max_size!
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_debug(unsigned char type, unsigned int size);

/**
 * Delegates calls to the respective protocol encoder for the protocol version the data message should be encoded with.
 * @param pid Id of the port, from which the message is originated.
 * @param size Size of the message in 4-byte blocks (i.e. count of integer values). Has to be below protocol_max_size!
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_data(unsigned char pid, unsigned int size);

/**
 * Delegates calls to the respective protocol encoder for the protocol version the checksum message should be encoded with.
 */
struct Message* encode_checksum();

#endif /* PROTOCOL_H_ */
