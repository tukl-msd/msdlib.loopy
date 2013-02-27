/*
 * io.h
 * @author Thomas Fischer
 * @since 27.02.2013
 */

#ifndef IO_H_
#define IO_H_

#include "constants.h"
#include "queueUntyped.h"

// queues
/** Microblaze  input queues */ Queue  *inQueue[ IN_STREAM_COUNT];
/** Microblaze output queues */ Queue *outQueue[OUT_STREAM_COUNT];

// "default" operations: process messages, process events
void recv_message(unsigned char pid, int payload[], unsigned int size);

void send_poll(unsigned char pid);

void flush_queue(unsigned char pid);

#endif /* IO_H_ */
