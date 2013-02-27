/**
 * Handles communication over the medium.
 * This includes medium-specific initialisation as well as the listening loop.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef MEDIUM_H_
#define MEDIUM_H_

/** initialise the communication medium */
void init_medium();

/**
 * Start listening for in-going packages
 * @returns A negative value, if failed. Otherwise this procedure will not return.
 *          If it does return anyway without failing, it will return 0.
 */
int start_application();

// TODO probably not the best idea... also provide some array send operation...
/**
 * Sends a single integer value over the medium.
 * @param val The value to be sent.
 */
void medium_send(int val);

void medium_read();

#endif /* MEDIUM_H_ */
