/**
 * Generic protocol encoder and decoder.
 * Delegates calls to the respective protocol decoder for the protocol version the message was sent with.
 * The protocol version is always stored in the same field of the header.
 * Calls to encoder procedures are delegated to an encoder of the protocol version
 * stored in the constants file (which has been specified by the user).
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef PROTOCOL_V2_H_
#define PROTOCOL_V2_H_

#define PROTO_MAX_SIZE   0

#define PROTO_ACK_SIZE   PROTO_MAX_SIZE
#define PROTO_DATA_SIZE  PROTO_MAX_SIZE
#define PROTO_DEBUG_SIZE PROTO_MAX_SIZE

#endif /* PROTOCOL_V2_H_ */
