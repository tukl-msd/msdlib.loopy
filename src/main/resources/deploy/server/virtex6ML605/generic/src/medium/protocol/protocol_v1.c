/**
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include <math.h>
#include <stdlib.h>
#include "xbasic_types.h"
#include "protocol_v1.h"
#include "../../constants.h"

// medium communication
int recv_int();

// gpio
void set_LED(u32 state);

// axi stream interface
void axi_write ( int  val, int target );
void axi_read  ( int *val, int target );

// generic print function
void xil_printf(const char *ctrl1, ...);

/**
 * Decode a header version 1.
 * Reads parts of the message from the medium using recv_int().
 */
int decode_header_v1(int first) {
	if(DEBUG) xil_printf("\n  reading message type ...");

	// set type as specified in protocol version 1
	first = fmod(first, pow(2, 24));
	int type = floor(first / pow(2, 20));

	if(DEBUG) xil_printf(" %d\n  reading component id ...", type);

	// set id as specified in protocol version 1
	first = fmod(first, pow(2, 20));
	int id = floor(first / pow(2, 16));

	if(DEBUG) xil_printf(" %d\n  reading message size ...", id);


	// the last two bytes mark the size of this frame
	int size = fmod(first, pow(2, 16));
	if(DEBUG) xil_printf(" %d", size);

	// 8 bit protocol version
	// 4 bit message type
	//   0xxx Config related
	//     0000 "Soft Reset"
	//     0111 Error
	//   1xxx Data related
	//     1000 Data Non-Blocking
	//     1001 Data Blocking
	//     1010 Poll Non-Blocking
	//     1011 Poll Blocking
	//     1110 GPIO
	//     1111 ACK
	// 4 bit component identifier
	// 16 bit size or value, depending on type
	// <size> bytes data, depending on type

	// perform actions, depending on the message type
	switch(type) {
	case  0: // This is a soft reset.
		     // Clear all queues and propagate a hardware reset.
		     // Afterwards, answer with a reset type message to acknowledge successful reset.
		break;
		// 1-6 are not assigned
	case  7: // This is an error message.
		     // By design, error messages should only be sent by the server.
		     // Consequently, receiving such a message is an error ;)
		break;
	case  8: // This is a non-blocking data package.
		     // In this case we actually have to read the content of the message ;)
	case  9: // This is a blocking data package.
		if(size > 0) {
			// the size is given in byte
			int payload[size];

			if(DEBUG) xil_printf("\n  reading payload ...");

			// read <size> bytes
			int i;
			for(i = 0; i < size; i++) {
				payload[i] = recv_int();
				if(DEBUG) xil_printf("\n    %d", payload[i]);
			}

			// TODO DO SOMETHING WITH THE DAMN MESSAGE
		}
		break;
	case 10: // This is a non-blocking poll.
		     // Receiving a poll from the client means reading <size> 32-bit values from an out-going port.
	case 11: // This is a blocking poll.
			// Receiving a poll from the client means reading <size> 32-bit values from an out-going port.
		break;
		// 12&13 are not assigned
	case 14: // This marks a GPIO message. We need to switch over the target component.
		switch(id) {
		case 0: // This marks setting of the LED state. In this case, we use the size field as value.
			set_LED(size);
			break;
		case 1: // switch poll - answer with current state
			break;
		case 2: // button poll - answer with current state
			break;
		default: if(DEBUG) xil_printf("\nWARNING: unknown GPIO component identifier %d. The frame will be ignored.", id);
		}
		break;
	case 15: // This is an acknowledgement.
		     // By design, acks should only be sent by the server.
		     // Consequently, receiving such a message is an error ;)
		break;
	default:
		if(DEBUG) xil_printf("\nWARNING: unknown type %d for protocol version 1. The frame will be ignored.", type);
		return 1;
	}

	if(DEBUG) xil_printf("\nfinished message interpretation");

	return 0;
}
#define version 1
#define ack 15
#define poll 10
#define data 8

struct Message* encode_ack_v1(unsigned char pid, unsigned int count) {
	struct Message *m = message_new();
	int header = (version << 24) + (ack << 20) + (pid << 16) + count;
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_poll_v1(unsigned char pid) {
	struct Message *m = message_new();
	int header = (version << 24) + (poll << 20) + (pid << 16);
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_data_v1(unsigned char id, unsigned int size) {
	struct Message *m = message_new();
	int header = (version << 24) + (data << 20) + (id << 16) + size;
	message_header(m, &header, 1);
	return m;
}

