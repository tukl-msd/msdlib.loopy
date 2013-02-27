/*
 * io.cpp
 * @author Thomas Fischer
 * @since 27.02.2013
 */

#include "io.h"
#include "medium/message.h"
#include "medium/medium.h"
#include "medium/protocol/protocol.h"

static void send_ack(unsigned char pid, unsigned int count) {
	struct Message *m = encode_ack(pid, count);
	medium_send(m);
	message_free(m);
}

void recv_message(unsigned char pid, int payload[], unsigned int size) {
	unsigned int i, count;
	for(i = 0; i < size; i++) {
		// add values, until sw queue is full
		if(inQueue[pid]->size < SW_QUEUE_SIZE) {
			put(inQueue[pid], payload[i]);
			count++;
		} else break;
	}
	// acknowledge all stored values
	send_ack(pid, count);
}

void send_poll(unsigned char pid) {
	struct Message *m = encode_poll(pid);
	medium_send(m);
	message_free(m);
}

void flush_queue(unsigned char pid) {
	// return, if the queue is empty
	if(outQueue[pid]->size == 0) return;
	struct Message *m = encode_data(pid, outQueue[pid]->size);
	medium_send(m);
	message_free(m);
}
