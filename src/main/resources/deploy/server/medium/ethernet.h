/**
 * Handles communication over Ethernet.
 * This includes medium-specific initialisation as well as the listening loop.
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#ifndef ETHERNET_H_
#define ETHERNET_H_

#include <stdio.h>
#include <string.h>

#include "lwip/tcp.h"
#include "lwip/err.h"

/** initialise this communication medium */
void init_medium();

/** start listening for in-going packages */
int start_application();

#endif /* ETHERNET_H_ */
