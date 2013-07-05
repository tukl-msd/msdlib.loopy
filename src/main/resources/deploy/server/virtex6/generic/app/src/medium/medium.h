/**
 * Handles communication over the medium.
 * This includes medium-specific initialisation as well as the listening loop.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef MEDIUM_H_
#define MEDIUM_H_

#include "message.h"

/** initialise the communication medium */
int init_medium();

/**
 * Start listening for in-going packages
 * @returns A negative value, if failed. Otherwise this procedure will not return.
 *          If it does return anyway without failing, it will return 0.
 */
int start_application();

/**
 * Sends a message over the medium. Does not de-allocate the message nor its contents in the process.
 *
 * May fail due to problems with the medium or insufficient memory for sending.
 *
 * @param m The message to be sent.
 * @return 0 if successful, 1 if failed.
 */
int medium_send(struct Message *m);

/**
 * Reads a message from the medium and pushes it to the procol interpreter.
 * @return 1 if a message was available, 0 if no message was available.
 */
int medium_read();

#endif /* MEDIUM_H_ */

