/**
 * Wrapper for setting up and shutting down the driver.
 * @file
 * @author Thomas Fischer
 * @since 16.04.2013
 */

#ifndef SETUP_H_
#define SETUP_H_

#include "../constants.h"
#include <string>

/**
 * Starts up the client-side driver by starting
 * writer and reader threads and setting up ethernet-specific
 * configuration.
 */
// TODO Currently only considers Ethernet.
// Other mediums should be supported from here already as well, resulting in different startup methods.
#ifdef IP
void startup();
#else
void startup(std::string ip);
#endif

/**
 * Shuts down the writer and reader threads.
 */
void shutdown();

#endif /* SETUP_H_ */

