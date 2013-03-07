/**
 * Wrapper to encapsulate io operations of the board-side driver.
 * Contains wrapper procedures for communication with the host-side
 * driver over the medium on the one hand and vhdl components on the
 * board over axi-stream.
 * @file
 * @author Thomas Fischer
 * @since 27.02.2013
 */

#ifndef IO_H_
#define IO_H_

#include "constants.h"
#include "queueUntyped.h"

/** Microblaze input queues */
Queue  *inQueue[ IN_STREAM_COUNT];
/** Microblaze output queue */
int outQueue[SW_QUEUE_SIZE];

extern int outQueueSize;

/**
 * Initialises the software queues on the microblaze.
 */
void init_queue();

/**
 * Process an incoming data package.
 * This procedure forwards the received data to a vhdl component
 * over axi stream.
 * @param pid Target port, for which the message is intended.
 * @param payload Data that is sent to the component.
 * @param size Number of values sent to the component.
 */
void recv_message(unsigned char pid, int payload[], unsigned int size);

/**
 * Sends a poll for more data to the host-side driver.
 * This results in additional data packages to be sent from the
 * host-side driver.
 * @param pid Port, for which data is requested.
 */
void send_poll(unsigned char pid);

/**
 * Flushes the software output queue and sends its contents to the host-side client.
 * This does neither influence input queues nor the hardware output queues for any port.
 * @param pid The port, of which data is currently stored in the output queue.
 */
void flush_queue(unsigned char pid);

#endif /* IO_H_ */
