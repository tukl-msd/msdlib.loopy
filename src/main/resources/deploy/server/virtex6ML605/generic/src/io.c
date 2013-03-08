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

int outQueueSize = 0;

void init_queue() {
	if(DEBUG) xil_printf("initialise queues ...\n");
	int i;
	for(i = 0; i <  IN_STREAM_COUNT; i++)  inQueue[i] = createQueue();
}

/**
 * Resets all software queues.
 * Note, that a hardware reset has to be performed as well.
 * TODO The reset has probably be performed in parallel,
 *      in order to guarantee that no more values remain somewhere
 *      -> input queue -> hw reset -> output queue
 */
void reset_queues() {
	if(DEBUG) xil_printf("reset queues ...\n");
	// TODO set some reset flag
	int i;
	for(i = 0; i <  IN_STREAM_COUNT; i++) clear( inQueue[i]);
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
		// add values, until sw queue is full
		if(inQueue[pid]->size < SW_QUEUE_SIZE) {
			put(inQueue[pid], payload[i]);
		} else break;
	}
	if(DEBUG) xil_printf("\nsend ack for pid %d with %d values", pid, i);

	// acknowledge all stored values
	send_ack(pid, i);
}

void send_poll(unsigned char pid) {
	struct Message *m = encode_poll(pid);
	medium_send(m);
	message_free(m);
}

void XUartLite_SendByte(u32 BaseAddress, u8 Data);

void UartSendInt(int number) {
	int i;
	char byte;
	for(i = 3; i>= 0; i--) {
		byte = (number >> i*8) &0xff;
		XUartLite_SendByte(XPAR_RS232_UART_1_BASEADDR, byte);
	}
}

void test_send(struct Message *m) {
	int i;
	for(i = 0; i < m->headerSize;  i++) UartSendInt(m->header[i]);
	for(i = 0; i < m->payloadSize; i++) UartSendInt(m->payload[i]);
}

void flush_queue(unsigned char pid) {
	if(DEBUG) xil_printf("\nflushing %d ...", pid);

	// return, if the queue is empty
	if(outQueueSize == 0) {
		if(DEBUG) xil_printf(" empty");
		return;
	}

	if(DEBUG) xil_printf(" count: %d", outQueueSize);
	// otherwise, encode a data header
	struct Message *m = encode_data(pid, outQueueSize);

	// append the actual payload
	message_payload(m, outQueue, outQueueSize);

	medium_send(m);
//	if(DEBUG) xil_printf(" (RAW: ");
//	test_send(m);
//	if(DEBUG) xil_printf(" )");

	message_free(m);
	outQueueSize = 0;
}
