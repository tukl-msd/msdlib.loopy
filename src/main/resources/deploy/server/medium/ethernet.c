/*
 * Ethernet.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

#include "ethernet.h"

#include "xparameters.h"
#include "netif/xadapter.h"

// do we actually need these includes or would fwd definitions be sufficient?? i have no idea ):
#include "lwip/init.h"
#include "protocol/protocol.h"

// these inclusions probably are't a good idea...
#include "mb_interface.h"
#include "../platform.h"

#include "../constants.h"

// definitions of Driver
#define PORT 8844

// attributes of Driver
unsigned char (mac_ethernet_address) [6] = { 0x00, 0x0a, 0x35, 0x00, 0x01, 0x02 };
struct ip_addr ip, mask, gw;
struct netif *netif, server_netif;

void print_ip ( char *msg, struct ip_addr *ip ) {
    xil_printf(msg);
    xil_printf("%d.%d.%d.%d\n", ip4_addr1(ip), ip4_addr2(ip), ip4_addr3(ip), ip4_addr4(ip));
}

void print_ip_settings(struct ip_addr *ip, struct ip_addr *mask, struct ip_addr *gw) {
	print_ip("Board IP: ", ip);
	print_ip("Netmask : ", mask);
	print_ip("Gateway : ", gw);
}

err_t recv_callback(void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err) {
	/* do not read the packet if we are not in ESTABLISHED state */
	if (!p) {
		tcp_close(tpcb);
		tcp_recv(tpcb, NULL);
		return ERR_OK;
	}

	/* indicate that the packet has been received */
	tcp_recved(tpcb, p->len);

	/* echo back the payload */
	/* in this case, we assume that the payload is < TCP_SND_BUF */
	if (tcp_sndbuf(tpcb) > p->len) {
		// TODO call application? some sort of distribution here!
//		err = tcp_write(tpcb, p->payload, p->len, 1);
		decode(p);
	} else
		xil_printf("no space in tcp_sndbuf\n\r");

	/* free the received pbuf */
	pbuf_free(p);

	return ERR_OK;
}

err_t accept_callback(void *arg, struct tcp_pcb *newpcb, err_t err) {
	static int connection = 1;

	/* set the receive callback for this connection */
	tcp_recv(newpcb, recv_callback);

	/* just use an integer number indicating the connection id as the
	   callback argument */
	tcp_arg(newpcb, (void*)connection);

	/* increment for subsequent accepted connections */
	connection++;

	return ERR_OK;
}


int start_application() {
	struct tcp_pcb *pcb;
	err_t err;

	/* create new TCP PCB structure */
	pcb = tcp_new();
	if (!pcb) {
		xil_printf("Error creating PCB. Out of Memory\n\r");
		return -1;
	}

	/* bind to specified @port */
	err = tcp_bind(pcb, IP_ADDR_ANY, PORT);
	if (err != ERR_OK) {
		xil_printf("Unable to bind to port %d: err = %d\n\r", PORT, err);
		return -2;
	}

	/* we do not need any arguments to callback functions */
	tcp_arg(pcb, NULL);

	/* listen for connections */
	pcb = tcp_listen(pcb);
	if (!pcb) {
		xil_printf("Out of memory while tcp_listen\n\r");
		return -3;
	}

	/* specify callback to use for incoming connections */
	tcp_accept(pcb, accept_callback);

	xil_printf("TCP echo server started @ port %d\n\r", PORT);

	/* receive and process packets */
	while (1) {
		xemacif_input(netif);
	}

	return 0;
}

void init_medium() {
	/* the mac address of the board. this should be unique per board */
	unsigned char mac_ethernet_address[] = { 0x00, 0x0a, 0x35, 0x00, 0x01, 0x02 };

	netif = &server_netif;

	/* initialize IP addresses to be used */
	IP4_ADDR(&ip,   192, 168,   1, 10);
	IP4_ADDR(&mask, 255, 255, 255,  0);
	IP4_ADDR(&gw,   192, 168,   1,  1);

	if(DEBUG) print_ip_settings(&ip, &mask, &gw);

	lwip_init();

	// This fails completely (as in "does not terminate"), if there is no Ethernet cable attached to the board...
	// Reason? Somewhere in here is the link speed auto-negotiation
  	/* Add network interface to the netif_list, and set it as default */
	if (!xemac_add(netif, &ip, &mask, &gw, mac_ethernet_address, XPAR_ETHERNET_LITE_BASEADDR)) {
		xil_printf("Error adding N/W interface\n\r");
//		return -1;
	}

	netif_set_default(netif);

	/* Create a new DHCP client for this interface.
	 * Note: you must call dhcp_fine_tmr() and dhcp_coarse_tmr() at
	 * the predefined regular intervals after starting the client.
	 */
	/* dhcp_start(netif); */

	/* now enable interrupts */
	// TODO This is basically a platform-dependent call... (we're on a microblaze, but there is also an implementation for ppc and zynq, maybe more)
	xil_printf("\nenable platform interrupts ...");
	platform_enable_interrupts();
	xil_printf("done");

	/* specify that the network if is up */
	netif_set_up(netif);
}
