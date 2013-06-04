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
	#if DEBUG
		loopy_print("reset queues ...\n");
	#endif /* DEBUG */

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
	struct Message *m = encode_ack(pid, count);
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

	#if DEBUG
		loopy_print("\nsend ack for pid %d with %d values", pid, i);
	#endif /* DEBUG */

	// acknowledge all stored values
	send_ack(pid, i);
}

void send_poll(unsigned char pid) {
	struct Message *m = encode_poll(pid);
	medium_send(m);
	message_free(m);
}

void send_gpio(unsigned char gid, unsigned char val) {
	struct Message *m = encode_gpio(gid, val);
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

void flush_queue(unsigned char pid) {
	#if DEBUG
		loopy_print("\nflushing %d ...", pid);
	#endif /* DEBUG */

	// return, if the queue is empty
	if(outQueueSize == 0) {
		#if DEBUG
			loopy_print(" empty");
		#endif /* DEBUG */
		return;
	}


	#if DEBUG
		loopy_print(" count: %d", outQueueSize);
	#endif /* DEBUG */
	// otherwise, encode a data header
	struct Message *m = encode_data(pid, outQueueSize);

	// append the actual payload
	message_payload(m, outQueue, outQueueSize);

	medium_send(m);

	message_free(m);
	outQueueSize = 0;
}

void send_debug(unsigned char type, const char *format, ...) {

	// make a run over the vararg parameter to determine the size
	va_list args;
	va_start(args, format);
	int size = vsnprintf(NULL, 0, format, args) + 1;
	va_end(args);

	// TODO check, that the size is smaller than the maxsize of the protocol!

	// allocate memory and store the formatted string
	char *c = malloc(sizeof(char) * size);
	va_start(args, format);
	vsnprintf(c, size, format, args);
	va_end(args);

	// encode and send a message
	struct Message *m = encode_debug(type, size);
	message_payload(m, (int*)c, size);
	medium_send(m);
}
