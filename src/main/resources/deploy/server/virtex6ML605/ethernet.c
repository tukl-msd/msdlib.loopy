/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "medium.h"

#include "xparameters.h"
#include "netif/xadapter.h"

// do we actually need these includes or would fwd definitions be sufficient?? i have no idea ):
#include "lwip/init.h"
#include "lwip/tcp.h"
#include "lwip/err.h"

#include "protocol/protocol.h"
#include "../platform.h"
#include "../constants.h"
#include "../queueUntyped.h"
#include <math.h>

// attributes of Driver
struct ip_addr ip, mask, gw, host;
struct netif *netif, server_netif;
struct tcp_pcb *pcb;
struct tcp_pcb *data_pcb;

struct tcp_pcb *con;

/**
 * Prints a single ip address, prefixed with a message.
 * @msg The message to be printed before the address.
 * @ip The ip address to be printed.
 */
static void print_ip ( char *msg, struct ip_addr *ip ) {
    xil_printf(msg);
    xil_printf("%d.%d.%d.%d", ip4_addr1(ip), ip4_addr2(ip), ip4_addr3(ip), ip4_addr4(ip));
}

/**
 * prints the provided ip settings.
 * @param ip The ip address of this device.
 * @param mask The subnet mask.
 * @param gw The standard gateway.
 */
static void print_ip_settings(struct ip_addr *ip, struct ip_addr *mask, struct ip_addr *gw, struct ip_addr *host) {
	print_ip("  Board IP : ", ip);
	xil_printf(":%d", PORT);
	print_ip("\n  Netmask  : ", mask);
	print_ip("\n  Gateway  : ", gw);
	print_ip("\n  Host IP  : ", host);
	xil_printf(":%d\n", PORT);

}
//32506054
static void set_unaligned ( int *target, int *data ) {
	if(DEBUG) xil_printf("\nunaligning value: %d", *data);
    int offset, i;
    char *byte, *res;

    offset = ((int)target) % 4;
    if (offset != 0) {
        byte = (void*)data;
        res = (void*)target;
        for (i=0; i<4; i++) *(res++) = ((*(byte++)) & 0xFF);
    } else *target = *data;
}

static int get_unaligned ( int *data ) {
    unsigned int offset, res, tmp;
    int i;
    char *byte;

    offset = ((int)data) % 4;
    if (offset != 0) {
        byte = (void*)data;
        res = 0;
        for (i=0; i<4; i++) {
            // make sure only rightmost 8bit are processed
            tmp = (*(byte++)) & 0xFF;
            // shift the value to the correct position
            tmp <<= (i*8);
            // sum up the 32bit value
            res += tmp;
        }
        return res;
    }
    return *data;
}

/** stores a received message */
static struct pbuf *msg;
/** stores the position of the next word to read in the received message */
static int wordIndex;

/**
 * Reads a package from the medium.
 * No... I have no idea how this works...
 */
void medium_read() {
	xemacif_input(netif);
}

err_t test(void *arg, struct tcp_pcb *newpcb, err_t err) {
	printf("\nwhatever...");
	return err;
}


err_t test2(void *arg, struct tcp_pcb *tpcb, u16_t len) {
	printf("\nclosing");
	tcp_close(tpcb);
	return 0;
}

void medium_send(struct Message *m) {
//	if (tcp_sndbuf(tpcb) > p->len) {
//			err = tcp_write(tpcb, p->payload, p->len, 1);
//		} else
//			print("no space in tcp_sndbuf\n\r");
	// I have no idea what I'm doing!

	if(DEBUG) xil_printf("\nsending message of size %d", m->headerSize + m->payloadSize);
	if(DEBUG) xil_printf("\nheader int:  %d", m->header[0]);

	// allocate transport buffer for the message
	struct pbuf *p = pbuf_alloc(PBUF_TRANSPORT, 4 * (m->headerSize + m->payloadSize), PBUF_POOL);

	int i;
	// write header data in buffer
	for(i = 0; i < m->headerSize; i++)
		set_unaligned((void*)(((int)(p->payload))+(4*i)), &m->header[i]);

//	for(i = 0; i < m->headerSize; i++) *(((int*)(p->payload))+(4*i)) = m->header[i];

	// write payload in buffer
	for(i = 0; i < m->payloadSize; i++)
		set_unaligned((void*)(((int)(p->payload))+(4*(i+m->headerSize))), &m->payload[i]);

	// set i to total size of application layer message
//	i = m->headerSize + m->payloadSize;

	// create new tcp package
	err_t err;

	// setup a connection to the host data port
//	err = tcp_connect(data_pcb, &host, 8848, test);
//	if (err != ERR_OK) xil_printf("Err: %d\r\n", err);

	if (tcp_sndbuf(con) > p->len) {
		// write the package (doesn't send automatically!)
		err = tcp_write(con, p->payload, p->len, TCP_WRITE_FLAG_COPY);

		#ifdef DEBUG
			if (err != ERR_OK) xil_printf("Err: %d\r\n", err);
		#endif

		// send the package?
		err = tcp_output(con);

		#ifdef DEBUG
			if (err != ERR_OK) xil_printf("Err: %d\r\n", err);
		#endif
	} else {
		xil_printf("No space in tcp_sndbuf\n\r");
	}

//	tcp_sent(data_pcb, test2);
//	xil_printf("\nclosing connection");

	// close the connection
	err = tcp_close(data_pcb);
#ifdef DEBUG
		if (err != ERR_OK) xil_printf("Err: %d\r\n", err);
	#endif

	// free the buffer (this is probably a bad idea, if we do not copy and do not output manually beforehand)
	pbuf_free(p);
}

/**
 * Read the next integer value from a received message.
 * Requires, that a message has indeed been received beforehand.
 * @returns The read integer value.
 */
int recv_int() {
	// some check over the length of the message
	int word = get_unaligned(msg->payload + wordIndex*4);
	wordIndex++;
	return word;
}

/**
 * This procedure is called, whenever a packet is received.
 * It simply delegates the content of the package to the protocol decoder
 * and frees the memory after it has been processed
 */
err_t recv_callback(void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err) {
	// do not read the packet if we are not in ESTABLISHED state
	if (!p) {
		tcp_close(tpcb);
		tcp_recv(tpcb, NULL);
		return ERR_OK;
	}

	// indicate that the packet has been received
	tcp_recved(tpcb, p->len);

	// let the protocol interpreter handle the package
	msg = p; wordIndex = 0;
	while(wordIndex * 4 < msg->len)	decode_header(recv_int());

	// free the received pbuf
	pbuf_free(p);

	return ERR_OK;
}

/**
 * This procedure is called, whenever a new connection is opened.
 * It subsequently sets up the callback method for packages received over this connection.
 */
err_t accept_callback(void *arg, struct tcp_pcb *newpcb, err_t err) {
	static int connection = 1;

	con = newpcb;

	// set the receive callback for this connection
	tcp_recv(newpcb, recv_callback);

	// just use an integer number indicating the connection id as the callback argument
	tcp_arg(newpcb, (void*)connection);

	// increment for subsequent accepted connections
	connection++;

	return ERR_OK;
}

int start_application() {

	if(DEBUG) xil_printf("Starting server application ...");
	err_t err;

	// create new TCP PCB structure
	pcb = tcp_new();
	if (!pcb) {
		xil_printf("Error creating PCB. Out of Memory\n\r");
		return -1;
	}

	// bind to specified @port
	err = tcp_bind(pcb, IP_ADDR_ANY, PORT);
	if (err != ERR_OK) {
		xil_printf("Unable to bind to port %d: err = %d\n\r", PORT, err);
		return -2;
	}

	// we do not need any arguments to callback functions
	tcp_arg(pcb, NULL);

	// listen for connections
	pcb = tcp_listen(pcb);
	if (!pcb) {
		xil_printf("Out of memory while tcp_listen\n\r");
		return -3;
	}

	// specify callback to use for incoming connections
	tcp_accept(pcb, accept_callback);

	// init the data pcb...
	data_pcb = tcp_new();

	if(DEBUG) xil_printf(" done\n");


	// receive and process packets
//	while (1) {
//		xemacif_input(netif);
//	}

	return 0;
}

void init_medium() {
	if(DEBUG) xil_printf("\nSetting up Ethernet interface ...\n");

	// the mac address of the board. this should be unique per board
	unsigned char mac_ethernet_address[] = { MAC_1, MAC_2, MAC_3, MAC_4, MAC_5, MAC_6 };

	netif = &server_netif;

	// initialize IP addresses to be used
	IP4_ADDR(&ip,   IP_1,   IP_2,   IP_3,   IP_4);
	IP4_ADDR(&mask, MASK_1, MASK_2, MASK_3, MASK_4);
	IP4_ADDR(&gw,   GW_1,   GW_2,   GW_3,   GW_4);

	IP4_ADDR(&host, 192, 168, 1, 23);

	if(DEBUG) print_ip_settings(&ip, &mask, &gw, &host);

	lwip_init();

	// This fails completely (as in "does not terminate"), if there is no Ethernet cable attached to the board...
	// Reason? Somewhere in here is the link speed auto-negotiation
  	// Add network interface to the netif_list, and set it as default
	if (!xemac_add(netif, &ip, &mask, &gw, mac_ethernet_address, XPAR_ETHERNET_LITE_BASEADDR)) {
		xil_printf("Error adding N/W interface\n\r");
//		return -1;
		return;
	}
	netif_set_default(netif);

	/* Create a new DHCP client for this interface.
	 * Note: you must call dhcp_fine_tmr() and dhcp_coarse_tmr() at
	 * the predefined regular intervals after starting the client.
	 */
	/* dhcp_start(netif); */

	// enable interrupts
	// TODO This is basically a platform-dependent call... (we're on a microblaze, but there is also an implementation for ppc and zynq, maybe more)
	// TODO This should already be handled by init_platform. We do not require the medium to set up interrupts again...
//	xil_printf("\nenable platform interrupts ...");
//	platform_enable_interrupts();
//	xil_printf("done");

	// specify that the network if is up
	netif_set_up(netif);
}
