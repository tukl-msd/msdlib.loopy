/**
 * Wrapper for setting up and shutting down the driver.
 * @file
 * @author Thomas Fischer
 * @since 16.04.2013
 */

#ifndef CONTROL_H_
#define CONTROL_H_

/**
 * Starts up the writer and reader threads.
 */
void startup();

/**
 * Shuts down the writer and reader threads.
 */
void shutdown();

#endif /* CONTROL_H_ */
