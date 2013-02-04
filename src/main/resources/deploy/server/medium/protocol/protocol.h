/*
 * protocol.h
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#ifndef PROTOCOL_H_
#define PROTOCOL_H_

//#include "lwip/init.h"
#include "lwip/pbuf.h"
//#include "netif/xadapter.h"

int decode( struct pbuf *p );

#endif /* PROTOCOL_H_ */
