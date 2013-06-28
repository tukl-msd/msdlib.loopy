/*
 * @author Thomas Fischer
 * @since 01.02.2013
 */

#include "medium.h"

#include "xparameters.h"
#include "netif/xadapter.h"

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
struct tcp_pcb *con;

/**
 * According to Xilinx employees, this is an implementation for a usleep on a PPC.
 * Hopefully, this also applies to Microblaze.
 *
 * The timing may be not exactly correct, but that doesn't matter here!
 *
 * @param useconds How many microseconds to wait (roughly)
 */
static void usleep(unsigned int useconds) {
  int i,j;
  for (j=0;j<useconds;j++)
    for (i=0;i<15;i++) asm("nop");
}

#if SEVERITY >= SEVERITY_FINEST
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
static void print_ip_settings(struct ip_addr *ip, struct ip_addr *mask, struct ip_addr *gw) {
    print_ip("  Board IP : ", ip);
    xil_printf(":%d", PORT);
    print_ip("\n  Netmask  : ", mask);
    print_ip("\n  Gateway  : ", gw);
//    print_ip("\n  Host IP  : ", host);
    xil_printf(":%d\n", PORT);

}
#endif

static inline void set_unaligned ( int *target, int *data ) {
    int offset, i;
    char *byte, *res;

    offset = ((int)target) % 4;
    if (offset != 0) {
        byte = (void*)data;
        res = (void*)target;
        for (i=0; i<4; i++) *(res++) = ((*(byte++)) & 0xFF);
    } else *target = *data;
}

static inline int get_unaligned ( int *data ) {
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

/**
 * Read the next integer value from a received message.
 *
 * If there are still unconsumed values in the current pbuf, the first one is returned.
 * Otherwise, a new pbuf is requested beforehand. The driver will stall until values are received!
 *
 * @return the received integer value.
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
 * Reads a message from the medium and pushes it to the procol interpreter.
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
        decode_header(recv_int());
        return 1;
    } else return 0;
}

/**
 * Tries to free memory on the board by reading an in-going message.
 * Reading consumes the message from the in-going stack. If the message was
 * an acknowledgment, the acknowledged message is also removed, resulting in
 * even more freed memory.
 *
 * If no message is available, the procedure sleeps TIMEOUT times for 1 second
 * intervalls and checks again.
 * If a timeout occurs, the board side driver will shut down.
 *
 * Debug messages from this method have to be sent over UART,
 * since memory for Ethernet could not be allocated...
 */
static int medium_freeMemory() {
#if SEVERITY >= SEVERITY_WARN
    xil_printf("\nWARNING: Out of memory - trying to free memory with reading...");
#endif
    // try to read a message, which should free memory

    int attempts = TIMEOUT * 4;
    while(medium_read() == 0) {
        // in this case, there was no message to consume ):
#if SEVERITY >= SEVERITY_WARN
        xil_printf("\nWARNING: No message. %d more attempts", attempts);
#endif

        if(attempts == 0) {
            // if the timeout has been reached, give up
            xil_printf("\nERROR: Timeout while waiting for messages to free memory.");
            return 1;
        }
        // if the timeout has not been reached, decrement and wait for some time
        attempts --;

        // TODO select instead of busy waiting? this seems kinda inefficient ):
        usleep(250000);
    }
    return 0;
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
            // again, not enough memory. try to free...
        	// FREEING MEMORY BY READING HERE IS A VERY BAD IDEA
        	//it can result in application acks and polls to be interleaved with data message headers and payloads
        	//i.e. send header, not enough space for payload --> recv some messages and send acks --> send palyoad
            if(medium_freeMemory()) {
                xil_printf("\nERROR: Could not send message due to memory shortage.");
                return 1;
            }
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
    // TODO setup an individual connection to the host for data (or check if it's still connected)
//  err = tcp_connect(data_pcb, &host, 8848, test);
//  if (err != ERR_OK) xil_printf("Err: %d\r\n", err);

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
        if(medium_freeMemory()) {
            xil_printf("\nERROR: Not enough space in tcp_sndbuf (failed to free more)");
            return 1;
        }
    }

    // enqueue header and payload
    if(tcp_enque(m->header,  m->headerSize))  return 1;
    if(tcp_enque(m->payload, m->payloadSize)) return 1;

    // flush tcp buffer
    err_t err = tcp_output(con);
    if (err != ERR_OK) {
        xil_printf("\nERROR: Could not flush tcp buffer (%d)", err);
        return 1;
    }

    return 0;
//  tcp_sent(data_pcb, test2);
//  loopy_print("\nclosing connection");

    // close the data connection? (since no ack is required here)
//  err = tcp_close(data_pcb);
//  if (err != ERR_OK) xil_printf("Err: %d\r\n", err);
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

#if SEVERITY >= SEVERITY_INFO
    xil_printf("Starting server application ...");
#endif

    err_t err;

    // create new TCP PCB structure
    pcb = tcp_new();
    if (!pcb) {
        xil_printf("\nERROR: Could not create PCB. Out of Memory");
        return -1;
    }

    // bind to specified @port
    err = tcp_bind(pcb, IP_ADDR_ANY, PORT);
    if (err != ERR_OK) {
        xil_printf("\nERROR: Unable to bind to port %d (%d)", PORT, err);
        return -2;
    }

    // we do not need any arguments to callback functions
    tcp_arg(pcb, NULL);

    // listen for connections
    pcb = tcp_listen(pcb);
    if (!pcb) {
        xil_printf("\nERROR: Out of memory while tcp_listen");
        return -3;
    }

    // specify callback to use for incoming connections
    tcp_accept(pcb, accept_callback);
#if SEVERITY >= SEVERITY_INFO
    xil_printf(" done");
#endif

    // receive and process packets
//    while (1) {
//        xemacif_input(netif_ptr);
//    }

    return 0;
}

void init_medium() {
#if SEVERITY >= SEVERITY_INFO
    xil_printf("\nSetting up Ethernet interface ...");
#endif

    // the mac address of the board. this should be unique per board
    unsigned char mac_ethernet_address[] = { MAC_1, MAC_2, MAC_3, MAC_4, MAC_5, MAC_6 };

    netif_ptr = &server_netif;

    // initialize IP addresses to be used
    IP4_ADDR(&ip,   IP_1,   IP_2,   IP_3,   IP_4);
    IP4_ADDR(&mask, MASK_1, MASK_2, MASK_3, MASK_4);
    IP4_ADDR(&gw,   GW_1,   GW_2,   GW_3,   GW_4);

//    IP4_ADDR(&host, 192, 168, 1, 23);

#if SEVERITY >= SEVERITY_INFO
    print_ip_settings(&ip, &mask, &gw);
#endif

    lwip_init();

    // This fails completely (as in "does not terminate"), if there is no Ethernet cable attached to the board...
    // Reason? Somewhere in here is the link speed auto-negotiation
      // Add network interface to the netif_list, and set it as default
    if (!xemac_add(netif_ptr, &ip, &mask, &gw, mac_ethernet_address, XPAR_ETHERNET_LITE_BASEADDR)) {
        xil_printf("\nERROR: Could not add network interface");
//        return -1;
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
//    loopy_print("\nenable platform interrupts ...");
//    platform_enable_interrupts();
//    loopy_print("done");

    // specify that the network if is up
    netif_set_up(netif_ptr);
}

