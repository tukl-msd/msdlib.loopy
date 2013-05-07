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
 * This method issues further write operations on the medium.
 * @param first The first integer of the message (which HAS to be part of the header).
 * @return 0 if successful, 1 if errors occurred.
 */
int decode_header_v1(int first);

/**
 * Generates the header for an acknowledgment with protocol version 1.
 * @param pid  Id of the port, which acknowledges data.
 * @param count Number of (integer) values, that are acknowledged.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_ack_v1(unsigned char pid, unsigned int count);

/**
 * Generates the header for a poll with protocol version 1.
 * @param pid  Id of the port, which is polled.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_poll_v1(unsigned char pid);

/**
 * Generates the header for a gpio data message with protocol version 1.
 * This message is sent, when the value of a gpio component changes.
 * @param gid Id of the gpio component.
 * @param val The new state of the gpio component.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_gpio_v1(unsigned char gid, unsigned char val);

/**
 * Generates the header for a data message with protocol version 1.
 * @param pid Id of the port, from which the message is originated.
 * @param size Size of the message in 4-byte blocks (i.e. count of integer values).
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_data_v1(unsigned char pid, unsigned int size);

/**
 * Generates the header for a debug message with protocol version 1.
 * @param type Debug type of the message.
 * @param size Size of the debug message in 4-byte blocks.
 * @return Pointer to an empty message with the generated header.
 */
struct Message* encode_debug_v1(unsigned char type, unsigned int size);

#endif /* PROTOCOL_V1_H_ */
