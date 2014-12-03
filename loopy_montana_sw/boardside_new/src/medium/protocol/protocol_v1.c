/**
 * @author Thomas Fischer
 * @author Mathias Weber
 * @since 01.02.2013
 */

#include "protocol.h"

#if PROTO_VERSION == 1

#include <math.h>
#include <stdlib.h>

#include "../../io.h"

#define PROTO_MAX_SIZE 65535

#define PROTO_ACK_SIZE   PROTO_MAX_SIZE
#define PROTO_DATA_SIZE  PROTO_MAX_SIZE
#define PROTO_DEBUG_SIZE PROTO_MAX_SIZE

// medium communication
int medium_recv_int();

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
#define reset    0
#define reqcheck 1
#define checksum 2
#define debug    7
#define data     9
#define poll    10
#define gpio    14
#define ack     15

/**
 * Decode a header version 1.
 * Reads parts of the message from the medium using recv_int().
 */
int decode_header(int first) {
	log_finer("decoding message header ...");

	// set type as specified in protocol version 1
	first = fmod(first, pow(2, 24));
	int type = floor(first / pow(2, 20));

	log_finest("message type: %d", type);

	// set id as specified in protocol version 1
	first = fmod(first, pow(2, 20));
	int id = floor(first / pow(2, 16));

	log_finest("target id   : %d", id);

	// the last two bytes mark the size of this frame
	int size = fmod(first, pow(2, 16));
	log_finest("payload size: %d", size);


  printf("Message received: (type: %d, id: %d, size: %d)", type, id, size);

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
	case reqcheck:
	    log_fine("FINE: sending checksum to host.");
        send_checksum();
	    break;
	case checksum:
        log_warn("WARNING: got checksum message without requesting it. The board will not check the checksum. Frame ignored.");
	    return 1;
		// 3-6 are not assigned
	case  debug: // This is an error message.
		     // By design, error messages should only be sent by the server.
		     // Consequently, receiving such a message is an error ;)
		break;
	case  data: // This is a blocking data package.
		if(size > 0) {
			// the size is given in byte
			int payload[size];

			log_finer("reading payload ...");

			// read <size> bytes
			int i;
			for(i = 0; i < size; i++) {
				payload[i] = medium_recv_int();
        printf("value: %d: %d", i, payload[i]);

				log_finest("value %d: %d", i, payload[i]);
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
		log_warn("WARNING: unknown type %d for protocol version 1. The frame will be ignored.", type);
		return 1;
	}

	log_finer("finished message interpretation");

	return 0;
}

struct Message* encode_ack(unsigned char pid, unsigned int count) {
	struct Message *m = message_new();
	int header = (version << 24) + (ack << 20) + (pid << 16) + count;
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_poll(unsigned char pid) {
	struct Message *m = message_new();
	int header = (version << 24) + (poll << 20) + (pid << 16);
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_gpio(unsigned char gid, unsigned char val) {
	struct Message *m = message_new();
	int header = (version << 24) + (gpio << 20) + (gid << 16) + val;
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_data(unsigned char pid, unsigned int size) {
	log_fine("encoding data message %d %d %d", data, pid, size);
	struct Message *m = message_new();
	int header = (version << 24) + (data << 20) + (pid << 16) + size;
	log_fine("encoded header: %d", header);
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_debug(unsigned char type, unsigned int size) {
	struct Message *m = message_new();
	int header = (version << 24) + (debug << 20) + (type << 16) + size;
	message_header(m, &header, 1);
	return m;
}

struct Message* encode_checksum() {
    struct Message *m = message_new();
    int header = (version << 24) + (checksum << 20);
    message_header(m, &header, 1);
    return m;
}

#endif /* PROTO_VERSION */
