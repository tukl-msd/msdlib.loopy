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

#endif /* MEDIUM_H_ */
