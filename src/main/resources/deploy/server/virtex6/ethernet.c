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
struct ip_addr ip, mask, gw;
struct netif *netif_ptr, server_netif;
struct tcp_pcb *pcb;
struct tcp_pcb *data_pcb;

struct tcp_pcb *con;

/**
 * Prints a single ip address, prefixed with a message.
 * @msg The message to be printed before the address.
 * @ip The ip address to be printed.
 */
static void print_ip ( char *msg, struct ip_addr *ip ) {
    loopy_print(msg);
    loopy_print("%d.%d.%d.%d", ip4_addr1(ip), ip4_addr2(ip), ip4_addr3(ip), ip4_addr4(ip));
}

/**
 * prints the provided ip settings.
 * @param ip The ip address of this device.
 * @param mask The subnet mask.
 * @param gw The standard gateway.
 */
static void print_ip_settings(struct ip_addr *ip, struct ip_addr *mask, struct ip_addr *gw) {
	print_ip("  Board IP : ", ip);
	loopy_print(":%d", PORT);
	print_ip("\n  Netmask  : ", mask);
	print_ip("\n  Gateway  : ", gw);
//	print_ip("\n  Host IP  : ", host);
	loopy_print(":%d\n", PORT);

}

static void set_unaligned ( int *target, int *data ) {
	#if DEBUG
		loopy_print("\nunaligning value: %d", *data);
	#endif /* DEBUG */

    int offset, i;
    char *byte, *res;

    offset = ((int)target) % 4;
    if (offset != 0) {
        byte = (void*)data;
        res = (void*)target;
        for (i=0; i<4; i++) *(res++) = ((*(byte++)) & 0xFF);
    } else *target = *data;

	#if DEBUG
		loopy_print(" to %d", *target);
	#endif /* DEBUG */
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
static struct pbuf *msgFst = NULL, *msgCurSeg = NULL;
/** stores the position of the next word to read in the received message */
static unsigned short wordIndex = 0, rWordIndex = 0;

static void inline print_message(struct Message *m) {
	loopy_print("\nsending message of size %d", m->headerSize + m->payloadSize);
	loopy_print("\nheader int:  %d", m->header[0]);

	loopy_print(" (Payload: ", m->payloadSize);
	int i;
	for(i = 0; i < m->payloadSize-1; i++) loopy_print("%d, ", m->payload[i]);
	loopy_print("%d", m->payload[m->payloadSize-1]);
	loopy_print(" )");
}

void medium_send(struct Message *m) {
//	if (tcp_sndbuf(tpcb) > p->len) {
//			err = tcp_write(tpcb, p->payload, p->len, 1);
//		} else
//			loopy_print("no space in tcp_sndbuf\n\r");
	// I have no idea what I'm doing!
	#if DEBUG
		print_message(m);
	#endif /* DEBUG */

	// allocate transport buffer for the message
	struct pbuf *p = pbuf_alloc(PBUF_TRANSPORT, sizeof(int) * (m->headerSize + m->payloadSize), PBUF_POOL);

	int i;

	// unalign header and data
	for(i = 0; i < m->headerSize; i++)
		set_unaligned(&m->header[i], &m->header[i]);
	for(i = 0; i < m->payloadSize; i++)
		set_unaligned(&m->payload[i], &m->payload[i]);

	// write header and payload
	for(i = 0; i < m->headerSize; i++)
		((int*)p->payload)[i] = m->header[i];
	for(i = 0; i < m->payloadSize; i++)
		((int*)p->payload)[i+m->headerSize] = m->payload[i];

//	for(i = 0; i < m->headerSize + m->payloadSize; i++) {
//		loopy_print("\nval: %d", ((int*)p->payload)[i]);
//	}
//
//	// write header data in buffer
//	for(i = 0; i < m->headerSize; i++)
//		set_unaligned((void*)(((int)(p->payload))+(sizeof(int)*i)), &m->header[i]);
//
////	for(i = 0; i < m->headerSize; i++) *(((int*)(p->payload))+(4*i)) = m->header[i];
//
//	// write payload in buffer
//	for(i = 0; i < m->payloadSize; i++)
//		set_unaligned((void*)(((int)(p->payload))+(sizeof(int)*(i+m->headerSize))), &m->payload[i]);

	// set i to total size of application layer message
//	i = m->headerSize + m->payloadSize;

	// create new tcp package
	err_t err;

	// setup a connection to the host data port
//	err = tcp_connect(data_pcb, &host, 8848, test);
//	if (err != ERR_OK) loopy_print("Err: %d\r\n", err);

	if (tcp_sndbuf(con) > p->len) {
		// write the package (doesn't send automatically!)
		err = tcp_write(con, p->payload, p->len, TCP_WRITE_FLAG_COPY);

		#ifdef DEBUG
			if (err != ERR_OK) loopy_print("Err: %d\r\n", err);
		#endif

		// send the package?
		err = tcp_output(con);

		#ifdef DEBUG
			if (err != ERR_OK) loopy_print("Err: %d\r\n", err);
		#endif
	} else {
		loopy_print("No space in tcp_sndbuf\n\r");
	}

//	tcp_sent(data_pcb, test2);
//	loopy_print("\nclosing connection");

	// close the connection
//	err = tcp_close(data_pcb);
//#ifdef DEBUG
//		if (err != ERR_OK) loopy_print("Err: %d\r\n", err);
//	#endif

	// free the buffer (this is probably a bad idea, if we do not copy and do not output manually beforehand)
//	m->header = NULL;
//	m->payload = NULL;
	pbuf_free(p);
}

/**
 * Read the next integer value from a received message.
 */
int recv_int() {
    // get next pbuf, if there is currently none (might block indefinitely, if client is faulty...)
    while(msgFst == NULL) xemacif_input(netif_ptr);

	// get an integer value
	int word = get_unaligned(msgCurSeg->payload + rWordIndex*4);

	// increment word indices
	wordIndex++; rWordIndex++;

	// end of pbuf chain - free memory
	if(wordIndex*4 >= msgFst->tot_len) {
		pbuf_free(msgFst);
		msgFst = NULL;
	// end of current pbuf segment - get the next (and reset relative word index)
	} else if(rWordIndex*4 >= msgCurSeg->len) {
		msgCurSeg = msgCurSeg->next;
		rWordIndex = 0;
	}

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

	// set message pointers, reset word indices
	msgFst    = p; msgCurSeg  = msgFst;
	wordIndex = 0; rWordIndex = 0;

//	while(wordIndex * 4 < msg->len)	decode_header(recv_int());

	// indicate that the packet has been received (this MIGHT be too early now...)
	tcp_recved(tpcb, p->tot_len);

	loopy_print("\npbuf len: %d", msgFst->len);
	loopy_print("\ntotal pbuf len: %d", msgFst->tot_len);

	// free the received pbuf (this DEFINITELY is too early now...)
//	pbuf_free(p);

	return ERR_OK;
}

/**
 * Gets the next set of values from the lwip stack and runs protocol interpretation.
 *
 * If there still is an unfinished pbuf, the first unread value of this pbuf will be
 * processed. Otherwise, the procedure tries to acquire a new pbuf.
 * If none is available, this basically is a no-op.
 *
 */
void medium_read() {
    // if no pbuf is cached, get the next one
    if(msgFst == NULL) xemacif_input(netif_ptr);
    // if there is a pbuf (now), decode the first int as header
    if(msgFst != NULL) decode_header(recv_int());
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

	loopy_print("Starting server application ...");

	err_t err;

	// create new TCP PCB structure
	pcb = tcp_new();
	if (!pcb) {
		loopy_print("Error creating PCB. Out of Memory\n\r");
		return -1;
	}

	// bind to specified @port
	err = tcp_bind(pcb, IP_ADDR_ANY, PORT);
	if (err != ERR_OK) {
		loopy_print("Unable to bind to port %d: err = %d\n\r", PORT, err);
		return -2;
	}

	// we do not need any arguments to callback functions
	tcp_arg(pcb, NULL);

	// listen for connections
	pcb = tcp_listen(pcb);
	if (!pcb) {
		loopy_print("Out of memory while tcp_listen\n\r");
		return -3;
	}

	// specify callback to use for incoming connections
	tcp_accept(pcb, accept_callback);

	// init the data pcb...
	data_pcb = tcp_new();

	loopy_print(" done\n");

	// receive and process packets
//	while (1) {
//		xemacif_input(netif_ptr);
//	}

	return 0;
}

void init_medium() {
	#if DEBUG
		loopy_print("\nSetting up Ethernet interface ...\n");
	#endif /*D EBUG */

	// the mac address of the board. this should be unique per board
	unsigned char mac_ethernet_address[] = { MAC_1, MAC_2, MAC_3, MAC_4, MAC_5, MAC_6 };

	netif_ptr = &server_netif;

	// initialize IP addresses to be used
	IP4_ADDR(&ip,   IP_1,   IP_2,   IP_3,   IP_4);
	IP4_ADDR(&mask, MASK_1, MASK_2, MASK_3, MASK_4);
	IP4_ADDR(&gw,   GW_1,   GW_2,   GW_3,   GW_4);

//	IP4_ADDR(&host, 192, 168, 1, 23);

	#if DEBUG
		print_ip_settings(&ip, &mask, &gw);
	#endif /* DEBUG */

	lwip_init();

	// This fails completely (as in "does not terminate"), if there is no Ethernet cable attached to the board...
	// Reason? Somewhere in here is the link speed auto-negotiation
  	// Add network interface to the netif_list, and set it as default
	if (!xemac_add(netif_ptr, &ip, &mask, &gw, mac_ethernet_address, XPAR_ETHERNET_LITE_BASEADDR)) {
		loopy_print("Error adding N/W interface\n\r");
//		return -1;
		return;
	}
	netif_set_default(netif_ptr);

	/* Create a new DHCP client for this interface.
	 * Note: you must call dhcp_fine_tmr() and dhcp_coarse_tmr() at
	 * the predefined regular intervals after starting the client.
	 */
	/* dhcp_start(netif_ptr); */

	// enable interrupts
	// TODO This is basically a platform-dependent call... (we're on a microblaze, but there is also an implementation for ppc and zynq, maybe more)
	// TODO This should already be handled by init_platform. We do not require the medium to set up interrupts again...
//	loopy_print("\nenable platform interrupts ...");
//	platform_enable_interrupts();
//	loopy_print("done");

	// specify that the network if is up
	netif_set_up(netif_ptr);
}
