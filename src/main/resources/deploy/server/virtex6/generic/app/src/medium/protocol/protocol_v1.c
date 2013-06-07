/**
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include <math.h>
#include <stdlib.h>
#include "xbasic_types.h"
#include "protocol_v1.h"
#include "../../constants.h"
#include "../../io.h"

// medium communication
int recv_int();

// gpio
#if gpi_count > 0 || gpo_count > 0
void gpio_write(int target, int val);
#endif

// axi stream interface
void axi_write ( int  val, int target );
void axi_read  ( int *val, int target );

// generic print function
void xil_printf(const char *ctrl1, ...);

#define version 1

// message types
#define reset 0
#define debug 7
#define data  9
#define poll 10
#define gpio 14
#define ack  15

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
	// 4 bit component identifier
	// 16 bit size or value, depending on type
	// <size> bytes data, depending on type

	// perform actions, depending on the message type
	switch(type) {
	case  reset: // This is a soft reset.
		     // Clear all queues and propagate a hardware reset.
		     // Afterwards, answer with a reset type message to acknowledge successful reset.
		break;
		// 1-6 are not assigned
	case  debug: // This is an error message.
		     // By design, error messages should only be sent by the server.
		     // Consequently, receiving such a message is an error ;)
		break;
	case  data: // This is a blocking data package.
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

			recv_message(id, payload, size);
		}
		break;
	case poll: // This is a poll. Receiving a poll from the client means reading <size> values from out-going port <id>.
        pollCount[id] += size;
		break;
	case gpio: // This marks a GPIO message. We need to switch over the target component.
#if gpi_count > 0 || gpo_count > 0
	    gpio_write(id, size);
#endif
	case ack: // This is an acknowledgement.
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

struct Message* encode_gpio_v1(unsigned char gid, unsigned char val) {
	struct Message *m = message_new();
	int header = (version << 24) + (gpio << 20) + (gid << 16) + val;
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_data_v1(unsigned char pid, unsigned int size) {
	xil_printf("\nencoding data message %d %d %d", data, pid, size);
	struct Message *m = message_new();
	int header = (version << 24) + (data << 20) + (pid << 16) + size;
	xil_printf("\nencoded header: %d", header);
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_debug_v1(unsigned char type, unsigned int size) {
	struct Message *m = message_new();
	int header = (version << 24) + (debug << 20) + (type << 16) + size;
	message_header(m, &header, 1);
	return m;
}
