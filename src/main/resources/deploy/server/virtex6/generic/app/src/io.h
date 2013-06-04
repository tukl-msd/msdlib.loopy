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
Queue *inQueue[IN_STREAM_COUNT];
/** Microblaze output queue */
int outQueue[MAX_OUT_SW_QUEUE_SIZE];
/** capacity of the output queues */
int outQueueCap[OUT_STREAM_COUNT];
/** stores if the port is a polling port */
int isPolling[OUT_STREAM_COUNT];
/** current poll counter of the port */
unsigned int pollCount [OUT_STREAM_COUNT];
/** current size of the output queue */
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
 * Sends the state of a gpio component to the host-side driver.
 * @param gid Id of the gpio component.
 * @param val State of the gpio component.
 */
void send_gpio(unsigned char gid, unsigned char val);

/**
 * Flushes the software output queue and sends its contents to the host-side client.
 * This does neither influence input queues nor the hardware output queues for any port.
 * @param pid The port, of which data is currently stored in the output queue.
 */
void flush_queue(unsigned char pid);

/**
 * Sends a debug message to the host-side driver.
 * @param type Debug type of the message.
 * @param format Format string of the message.
 */
void send_debug(unsigned char type, const char *format, ...);

#endif /* IO_H_ */
