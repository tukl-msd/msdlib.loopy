/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "medium.h"

#include "xparameters.h"
#include "netif/xadapter.h"

#include "protocol/protocol.h"

#include "../constants.h"
#include "../platform.h"
#include "../platform_config.h"
#include "../queueUntyped.h"

#include "lwip/init.h"
#include "lwip/tcp.h"
#include "lwip/err.h"
#if DHCP
#include "lwip/dhcp.h"
#endif

#include <math.h>


/* *************************************************************************************** */
/* **************************************** GLOBAL *************************************** */
/* *************************************************************************************** */

/** pointer to the network interface */
static struct netif *netif_ptr;

/** active connection, if any */
static struct tcp_pcb *con;

/** number of currently acknowledged values */
static unsigned int num_ack = 0;

/**
 * According to Xilinx employees, this is an implementation for a usleep on a PPC.
 * The timing may be not exactly correct, but that doesn't matter here!
 *
 * @param useconds How many microseconds to wait (roughly)
 */
static inline void usleep(unsigned int useconds) {
  int i,j;
  for (j=0;j<useconds;j++)
    for (i=0;i<15;i++) asm("nop");
}


/* *************************************************************************************** */
/* ********************************* SENDING & RECEIVING ********************************* */
/* *************************************************************************************** */

static inline void set_unaligned ( int *target, int *data ) {
    int i;
    char *res;

    int datai = *data;
    res = (void*)target;
    for (i=3; i>=0; i--) {
         *(res++) = (char)((datai >> (i*8)) & 0xFF);
    }
}

static inline int get_unaligned ( int *data ) {
    unsigned int res, tmp;
    int i;
    char *byte;

    byte = (void*)data;
    res = 0;
    for (i=3; i>=0; i--) {
        // make sure only rightmost 8bit are processed
        tmp = (*(byte++)) & 0xFF;
        // shift the value to the correct position
        tmp <<= (i*8);
        // sum up the 32bit value
        res += tmp;
    }
    return res;
}


/** stores a received message */
static struct pbuf *msgFst = NULL, *msgCurSeg = NULL;
/** stores the position of the next word to read in the received message */
static unsigned short wordIndex = 0, rWordIndex = 0;

/**
 * Tries to free memory on the board by reading an in-going message.
 * Reading consumes the message from the in-going stack. If the message was
 * an acknowledgment, the acknowledged message is also removed, resulting in
 * even more freed memory.
 *
 * If no message is available, the procedure sleeps TIMEOUT times for 1 second
 * intervals and checks again.
 * If a timeout occurs, the board side driver will shut down.
 *
 * Debug messages from this method have to be sent over UART,
 * since memory for Ethernet could not be allocated...
 */
static int medium_free_memory() {
	// initialize attempt counter to four times timeout (since we check every 250 ms)
	int attempts = TIMEOUT * 4;

	for(attempts = TIMEOUT * 4; attempts > 0 ; attempts--) {
		// check for a message
		int rslt = medium_read();

		if(num_ack > 0) {
			num_ack = 0;
			return 0;
		}

		// if there was an application message, restart the attempt counter
		// (not sure if this is wise... the other message will probably result in memory freeing attempts as well)
		if(rslt > 0) {
			attempts = TIMEOUT * 4;
			continue;
		}

		// if there was no tcp packet, print a warning over uart
		// TODO can this even happen? Does xemac_inputf block until a packet arrives?
#if SEVERITY >= SEVERITY_WARN
		xil_printf("\nWARNING: No message. %d more attempts", attempts);
#endif

		// TODO select instead of busy waiting? this seems kinda inefficient ):
	    usleep(250000);
	}

	// give up, if the timeout has been reached
	xil_printf("\nERROR: Timeout while waiting for messages to free memory.");
	return 1;
}

/**
 * Enqueues data for sending over tcp by copying the provided values.
 *
 * Tries to free memory by consuming incoming messages if insufficient memory for copying is available.
 * Fails, if it cannot free a sufficient amount of memory.
 *
 * @param vals Pointer to array of values to be written.
 * @param size Number of int-sized values to be written.
 * @return 0 if successful, 1 otherwise.
 */
static inline int tcp_enque(int* vals, int size) {

    // return directly, if there is nothing to write...
    if(size == 0) return 0;

    // unalign values (not sure, if this is still required)
    int i;
    for(i = 0; i < size; i++)
        set_unaligned(vals+i, vals+i);


    err_t err;
    do {
        // write message header to tcp buffer (copying values)
        // Note, that the payload of the message indeed HAS to be copied,
        // since the message only points to the drivers out-going queue
        err = tcp_write(con, vals, size * sizeof(int), TCP_WRITE_FLAG_COPY);

        // Should not happen since we checked for the size earlier and it was sufficient...
        if(err == ERR_MEM) {
            // This can happen despite the send buffer check beforehand, since the send buffer
            // only checks for enough space to copy all values. However, tcp_write requires
            // more memory than that.
            // FREEING MEMORY BY READING HERE IS A VERY BAD IDEA
            // (may interleave debug messages in between header and values of data messages)
        	// If this happens in applications we need to copy the message to an array again and use that instead here... ):
            xil_printf("\nERROR: Could not send message due to memory shortage.");
            return 1;
        } else if(err != ERR_OK) {
            // Other errors directly kill the driver
            xil_printf("\nERROR: Could not write tcp message (%d)", err);
            return 1;
        } else {
            break;
        }
    } while(1);
    return 0;
}

int medium_send(struct Message *m) {
    // abort, if no connection was made so far
    if(con == NULL) {
        xil_printf("\nERROR: no connection detected");
        return 1;
    }

    // abort, if the connection is not in an established state
    if(con->state != ESTABLISHED) {
        xil_printf("\nERROR: connection not in an established state! (%d)", con->state);
        return 1;
    }

    // calculate total message size
    int totalSize = m->headerSize + m->payloadSize;

    // check if there is enough buffer space available
    while (tcp_sndbuf(con) <= (totalSize * sizeof(int))) {
        // if not, try to free memory
        if(medium_free_memory()) {
            xil_printf("\nERROR: Not enough space in tcp_sndbuf (failed to free more)");
            return 1;
        }
    }

    // enqueue header and payload
    if(tcp_enque(m->header,  m->headerSize))  return 1;
    // despite the send buffer check it is possible for
    // the the header write to work and the payload write to fail.
    // in this case, basically everything is broken, esp. when debugging is enabled.
    if(tcp_enque(m->payload, m->payloadSize)) return 1;

    // flush tcp buffer
    err_t err = tcp_output(con);
    if (err != ERR_OK) {
        xil_printf("\nERROR: Could not flush tcp buffer (%d)", err);
        return 1;
    }

    return 0;
}

/**
 * Read the next integer value from a received message.
 * This procedure is for the protocol interpreter in order to interpret one int after another.
 *
 * If there are still unconsumed values in the current pbuf, the first one is returned.
 * Otherwise, a new pbuf is requested beforehand. The driver will stall until values are received!
 *
 * @return the received integer value.
 */
int medium_recv_int() {
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
 * Reads a message from the medium and pushes it to the protocol interpreter.
 *
 * If there still is an unfinished pbuf, the first unread value of this pbuf will be
 * processed. Otherwise, the procedure tries to acquire a new pbuf.
 *
 * @return 1 if a message was available, 0 if no message was available.
 */
int medium_read() {
    // if no pbuf is cached, get the next one
    if(msgFst == NULL) xemacif_input(netif_ptr);
    // if there is a pbuf (now), decode the first int as header
    if(msgFst != NULL) {
        decode_header(medium_recv_int());
        return 1;
    } else return 0;
}


/* *************************************************************************************** */
/* ************************************** CALLBACKS ************************************** */
/* *************************************************************************************** */

/**
 * This procedure is called, whenever an acknowledgment is received.
 * It stores the number of acknowledged values and immediately returns.
 */
static err_t ack_callback (void *arg, struct tcp_pcb *tpcb, u16_t len) {
	num_ack = len;
	return ERR_OK;
}

/**
 * This procedure is called, whenever a packet is received.
 * It simply delegates the content of the package to the protocol decoder
 * and frees the memory after it has been processed
 */
static err_t recv_callback(void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err) {
    // do not read the packet if we are not in ESTABLISHED state
    if (!p) {
        tcp_close(tpcb);
        tcp_recv(tpcb, NULL);
        return ERR_OK;
    }

    // set message pointers, reset word indices
    msgFst    = p; msgCurSeg  = msgFst;
    wordIndex = 0; rWordIndex = 0;

//    while(wordIndex * 4 < msg->len)    decode_header(recv_int());

    // indicate that the packet has been received (this MIGHT be too early now...)
    tcp_recved(tpcb, p->tot_len);

    // free the received pbuf (this DEFINITELY is too early now...)
//    pbuf_free(p);

    return ERR_OK;
}

/**
 * This procedure is called, whenever a new connection is opened.
 * It subsequently sets up the callback method for packages received over this connection.
 */
static err_t accept_callback(void *arg, struct tcp_pcb *newpcb, err_t err) {
	// pass the a number used as connection id as argument to all callbacks
	static int connection = 1;
	tcp_arg(newpcb, (void*)connection);
    connection++;

    // use this connection to return results to
    con = newpcb;

    // bind callback procedures
    tcp_recv(newpcb, recv_callback);
    tcp_sent(newpcb, ack_callback);

    return ERR_OK;
}


/* *************************************************************************************** */
/* **************************************** SETUP **************************************** */
/* *************************************************************************************** */

static struct netif server_netif;
static struct tcp_pcb *pcb;

#if SEVERITY >= SEVERITY_INFO
/**
 * Prints a single IP address.
 * @param ip The IP address to be printed.
 */
static inline void print_ip (struct ip_addr *ip) {
    if(ip == NULL) xil_printf("0.0.0.0");
    else xil_printf("%d.%d.%d.%d", ip4_addr1(ip), ip4_addr2(ip), ip4_addr3(ip), ip4_addr4(ip));
}

/**
 * Prints the IP settings of the drivers network interface.
 * The settings are drawn out of the driver's netif pointer and
 * therefore match the settings, the driver has actually configured.
 */
static inline void print_ip_settings() {
	xil_printf("\nINFO:   Board IP : "); print_ip(&netif_ptr->ip_addr);
	xil_printf("\nINFO:   Netmask  : "); print_ip(&netif_ptr->netmask);
	xil_printf("\nINFO:   Gateway  : "); print_ip(&netif_ptr->gw);

}

/**
 * Prints the TCP settings of the board-side driver.
 * As of now, these only consist of the port used for driver communication.
 * The port is stored in the driver's constants.h file.
 */
static inline void print_tcp_settings() {
	xil_printf("\nINFO:   Port     : %d", PORT);
}
#endif /* SEVERITY */

#ifdef DHCP
/**
 * Acquires an IP over DHCP.
 * @return 0 if successful, an error code otherwise.
 */
static int medium_dhcp_config() {
	// Create a new DHCP client for this interface.
#if SEVERITY >= SEVERITY_INFO
    xil_printf("\nINFO: Starting DHCP client ...");
#endif /* SEVERITY */
    dhcp_start(netif_ptr);

    unsigned int dhcp_attempts = 0;
    while(1) {
    	// perform ip input
	    xemacif_input(netif_ptr);

    	// continue, if we have acquired an ip
        if(netif_ptr->ip_addr.addr) {
#if SEVERITY >= SEVERITY_INFO
        	xil_printf("\nFINE: Successfully configured IP settings via DHCP to ");
        	print_ip(&netif_ptr->ip_addr);
#endif /* SEVERITY */
        	break;
        }

        // stop after a number of attempts
        dhcp_attempts++;
        if(dhcp_attempts >= (DHCP_MAX_ATTEMPTS * 4)) {
        	xil_printf("\nERROR: DCHP request timed out after %d seconds", dhcp_attempts * 4);
	        return -2;
	    }

        // sleep and try again afterwards
	    usleep(DHCP_FINE_TIMER_MSECS * 250);
    }
    return 0;
}
#endif /* DHCP */

/**
 * Performs setup for tcp on transport layer.
 * Creates necessary data structures and binds user-specified port as well
 * as callback methods for in-going tcp connections.
 * @return 0 if successful, an error code otherwise.
 */
static int medium_setup_tcp() {

    // create new TCP PCB structure
    pcb = tcp_new();
    if (!pcb) {
        xil_printf("\nERROR: Could not create PCB. Out of Memory");
        return -3;
    }

    // bind to specified @port
    err_t err = tcp_bind(pcb, IP_ADDR_ANY, PORT);
    if (err != ERR_OK) {
        xil_printf("\nERROR: Unable to bind to port %d (%d)", PORT, err);
        return -4;
    }

    // we do not need any arguments to callback functions
    tcp_arg(pcb, NULL);

    // listen for connections
    pcb = tcp_listen(pcb);
    if (!pcb) {
        xil_printf("\nERROR: Out of memory while tcp_listen");
        return -5;
    }

    // specify callback to use for incoming connections
    tcp_accept(pcb, accept_callback);

    return 0;
}

/**
 * Performs IP setup on network layer.
 * Binds MAC and IP addresses as specified or acquires an
 * IP address over DHCP.
 * @returns 0 if successful, an error code otherwise.
 */
static int medium_setup_ip() {
#if SEVERITY >= SEVERITY_INFO
    xil_printf("\nINFO: Setting up Ethernet interface ...");
#endif

    // allocate some stack memory for ip addresses
    struct ip_addr ip, mask, gw;

    // construct the mac address of the board.
    unsigned char mac_ethernet_address[] = { MAC_1, MAC_2, MAC_3, MAC_4, MAC_5, MAC_6 };

    // assign netif pointer
    netif_ptr = &server_netif;

#ifndef DHCP
    // initialize IP addresses to be used
    IP4_ADDR(&ip,   IP_1,   IP_2,   IP_3,   IP_4);
    IP4_ADDR(&mask, MASK_1, MASK_2, MASK_3, MASK_4);
    IP4_ADDR(&gw,   GW_1,   GW_2,   GW_3,   GW_4);
#else
    IP4_ADDR(&ip  ,0,0,0,0);
    IP4_ADDR(&mask,0,0,0,0);
    IP4_ADDR(&gw  ,0,0,0,0);
#endif /* DHCP */

    lwip_init();

    // Add network interface to the netif_list, and set it as default
    // This fails completely (as in "does not terminate"), if there is no Ethernet cable attached to the board...
    if (!xemac_add(netif_ptr, &ip, &mask, &gw, mac_ethernet_address, PLATFORM_EMAC_BASEADDR)) {
        xil_printf("\nERROR: Could not add network interface");
        return -1;
    }
    netif_set_default(netif_ptr);

#if DHCP
    int rslt = medium_dhcp_config();
    if(rslt != 0) return rslt;
#endif /* DHCP */


    // enable interrupts?
    // TODO This is basically a platform-dependent call... (we're on a microblaze, but there is also an implementation for ppc and zynq, maybe more)
    // TODO This should already be handled by init_platform. We do not require the medium to set up interrupts again...
//    loopy_print("\nenable platform interrupts ...");
//    platform_enable_interrupts();
//    loopy_print("done");

    // specify that the network if is up
    netif_set_up(netif_ptr);

    return 0;
}

int init_medium() {
    // setup network interface & lwip
    int rslt = medium_setup_ip();
    if(rslt != 0) return rslt;

#if SEVERITY >= SEVERITY_INFO
    print_ip_settings();
#endif
    
    // setup transport layer
    rslt = medium_setup_tcp();
    if(rslt != 0) return rslt;

#if SEVERITY >= SEVERITY_INFO
    print_tcp_settings();
#endif

    return rslt;
}
