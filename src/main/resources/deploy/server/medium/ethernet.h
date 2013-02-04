/*
 * Ethernet.h
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#ifndef ETHERNET_H_
#define ETHERNET_H_

#include <stdio.h>
#include <string.h>

#include "lwip/tcp.h"
#include "lwip/err.h"

// initialise this communication medium
void init_medium();

// start listening for in-going packages
int start_application();

// callback method (to be executed whenever a package arrives)
//err_t accept_callback(void *arg, struct tcp_pcb *newpcb, err_t err);

// or maybe THIS is the callback method?
//err_t recv_callback(void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err);
#endif /* ETHERNET_H_ */
