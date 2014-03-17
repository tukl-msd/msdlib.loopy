/**
 * @author Thomas Fischer
 * @since 27.02.2013
 */

#include "io.h"
#include "medium/message.h"
#include "medium/medium.h"
#include "medium/protocol/protocol.h"

#include "xparameters.h"
#include "xbasic_types.h"

#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

int outQueueSize = 0;

/**
 * Resets all software queues.
 * Note, that a hardware reset has to be performed as well.
 * TODO The reset has probably be performed in parallel,
 *      in order to guarantee that no more values remain somewhere
 *      -> input queue -> hw reset -> output queue
 */
void reset_queues() {
	log_info("reset queues ...");

	// TODO set some reset flag
	int i;
	for(i = 0; i < IN_STREAM_COUNT; i++) clear( inQueue[i]);
	outQueueSize = 0;
	// TODO does this guarantee, that no more values will be written to the MB queues??
}

/**
 * Sends an acknowledgment to the client-side driver.
 * This should happen automatically after an in-going data package.
 * @param pid Port, for which data is acknowledged.
 * @param count Number of values that is acknowledged
 */
static void send_ack(unsigned char pid, unsigned int count) {
    // send several acknowledges, if the protocol cannot fit a full ack
    // this should not be the case with the current protocol impl, since data and ack messages have equal maxsize.
    while(count > PROTO_ACK_SIZE) {
        send_ack(pid, PROTO_ACK_SIZE);
        count -= PROTO_ACK_SIZE;
    }

    struct Message *m = encode_ack(pid, count);
	print_message(m);
	medium_send(m);
	message_free(m);
}

void recv_message(unsigned char pid, int payload[], unsigned int size) {
	unsigned int i = 0;
	for(i = 0; i < size; i++) {
		// add values, until corresponding sw queue is full
		if(inQueue[pid]->size < inQueue[pid]->cap) {
			put(inQueue[pid], payload[i]);
		} else break;
	}

	log_fine("send ack for pid %d with %d values", pid, i);

	// acknowledge all stored values
	send_ack(pid, i);
}

void send_poll(unsigned char pid) {
	struct Message *m = encode_poll(pid);
    print_message(m);
	medium_send(m);
	message_free(m);
}

void send_gpio(unsigned char gid, unsigned char val) {
	struct Message *m = encode_gpio(gid, val);
    print_message(m);
	medium_send(m);
	message_free(m);
}

//void XUartLite_SendByte(u32 BaseAddress, u8 Data);
//
//void UartSendInt(int number) {
//	int i;
//	char byte;
//	for(i = 3; i>= 0; i--) {
//		byte = (number >> i*8) &0xff;
//		XUartLite_SendByte(XPAR_RS232_UART_1_BASEADDR, byte);
//	}
//}
//
//void test_send(struct Message *m) {
//	int i;
//	for(i = 0; i < m->headerSize;  i++) UartSendInt(m->header[i]);
//	for(i = 0; i < m->payloadSize; i++) UartSendInt(m->payload[i]);
//}

int flush_queue(unsigned char pid) {
	log_fine("flushing %d ...", pid);

	// return, if the queue is empty
	if(outQueueSize == 0) {
		log_fine("empty");
		return 0;
	}

	log_fine("count: %d", outQueueSize);

	// otherwise, create a data message and send it
	// FIXME ensure, that outQueueSize < proto_data_size
	// or rather: split into several packages, if this is the case
	struct Message *m = encode_data(pid, outQueueSize);
	message_payload(m, outQueue, outQueueSize);
    print_message(m);
	int rslt = medium_send(m);

	message_free(m);
	outQueueSize = 0;

	return rslt;
}

void send_debug(unsigned char type, const char *format, ...) {

	// make a run over the vararg parameter to determine the size
	va_list args;
	va_start(args, format);
	unsigned int size = vsnprintf(NULL, 0, format, args) + 1;
	va_end(args);

    // properly align the char array to sizeof(int)
    // under the assumption, that sizeof(int) % sizeof(char) = 0
    #define cpi (sizeof(int) / sizeof(char))
    size += (cpi - (size % cpi)) % cpi;

    // FIXME check, that the resulting size is smaller than the maxsize of the protocol!?
//    if(size > )

    // allocate memory and store the formatted string
    char *c = malloc(sizeof(char) * size);
    va_start(args, format);
    vsnprintf(c, size, format, args);
    va_end(args);

	// encode and send a message (using sizeof(int) as size)
	struct Message *m = encode_debug(type, size / cpi);
	message_payload(m, (int*)c, size / cpi);
	medium_send(m);

	// free allocated memory
	message_free(m);
	free(c);
}
