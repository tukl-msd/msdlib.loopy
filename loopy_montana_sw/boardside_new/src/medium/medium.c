/*
 * @author Thomas Fischer
 * @author Mathias Weber
 * @since 01.02.2013
 * @modified 24.10.2014
 */

#include "medium.h"

#include "protocol/protocol.h"

#include "../constants.h"
#include "../queueUntyped.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <math.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <errno.h>


/* *************************************************************************************** */
/* **************************************** GLOBAL *************************************** */
/* *************************************************************************************** */

#define BACKLOG 5

/** pointer to the network interface */
static int sockfd = -1;

/** active connection, if any */
static int con = -1;

/* *************************************************************************************** */
/* ********************************* SENDING & RECEIVING ********************************* */
/* *************************************************************************************** */


//void setnonblocking(int sock)
//{
//  int opts;
//
//  opts = fcntl(sock, F_GETFL);
//  if (opts < 0) {
//    perror("fcntl(F_GETFL)");
//    exit(EXIT_FAILURE);
//  }
//  opts = (opts | O_NONBLOCK);
//  if (fcntl(sock, F_SETFL, opts) < 0) {
//    perror("fcntl(F_SETFL)");
//    exit(EXIT_FAILURE);
//  }
//  return;
//}



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
// TODO do we need this still?
//static int medium_free_memory() {
//	// initialize attempt counter to four times timeout (since we check every 250 ms)
//	int attempts = TIMEOUT * 4;
//
//	for(attempts = TIMEOUT * 4; attempts > 0 ; attempts--) {
//		// check for a message
//		int rslt = medium_read();
//
//		if(num_ack > 0) {
//			num_ack = 0;
//			return 0;
//		}
//
//		// if there was an application message, restart the attempt counter
//		// (not sure if this is wise... the other message will probably result in memory freeing attempts as well)
//		if(rslt > 0) {
//			attempts = TIMEOUT * 4;
//			continue;
//		}
//
//		// if there was no tcp packet, print a warning over uart
//		// TODO can this even happen? Does xemac_inputf block until a packet arrives?
//#if SEVERITY >= SEVERITY_WARN
//		xil_printf("\nWARNING: No message. %d more attempts", attempts);
//#endif
//
//    usleep(250000);
//	}
//
//	// give up, if the timeout has been reached
//	xil_printf("\nERROR: Timeout while waiting for messages to free memory.");
//	return 1;
//}


static inline void convert_to_network_order(uint32_t *vals, int size)
{
  int i;
  for (i = 0; i < size; i++) {
    vals[i] = htonl(vals[i]);
  }
}

//static inline void convert_from_network_order(uint32_t *vals, int size)
//{
//  int i;
//  for (i = 0; i < size; i++) {
//    vals[i] = ntohl(vals[i]);
//  }
//}

int medium_send(struct Message *m) {
  struct msghdr msg;
  struct iovec vec[2];

  // abort, if no connection was made so far
  if(con == -1) {
    printf("ERROR: no connection detected\n");
    return 1;
  }

  // calculate total message size
  int totalSize = m->headerSize + m->payloadSize;

  //// check if there is enough buffer space available
  //while (tcp_sndbuf(con) <= (totalSize * sizeof(int))) {
  //    // if not, try to free memory
  //    if(medium_free_memory()) {
  //        xil_printf("\nERROR: Not enough space in tcp_sndbuf (failed to free more)");
  //        return 1;
  //    }
  //}

  convert_to_network_order((uint32_t *)m->header, m->headerSize);
  vec[0].iov_base = m->header;
  vec[0].iov_len = m->headerSize;
  convert_to_network_order((uint32_t *)m->payload, m->payloadSize);
  vec[1].iov_base = m->payload;
  vec[1].iov_len = m->payloadSize;
  memset(&msg, 0, sizeof(struct msghdr));
  msg.msg_iov = vec;
  msg.msg_iovlen = totalSize;

  if (sendmsg(con, &msg, 0) == -1) {
    printf("ERROR: Could not send message\n");
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
uint32_t medium_recv_int() {
  uint32_t val;

  if (recv(con, &val, sizeof(uint32_t), 0) < 0) {
    perror("recv");
    exit(EXIT_FAILURE);
  }

  return ntohl(val);
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
  uint32_t val;
  if(recv(con, &val, sizeof(uint32_t), MSG_DONTWAIT) < 0) {
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
      return 0;
    } else {
      perror("revc: nonblocking");
      exit(EXIT_FAILURE);
    }
  }
  decode_header(val);

  return 1;
}


/* *************************************************************************************** */
/* **************************************** SETUP **************************************** */
/* *************************************************************************************** */

#if SEVERITY >= SEVERITY_INFO
/**
 * Prints the TCP settings of the board-side driver.
 * As of now, these only consist of the port used for driver communication.
 * The port is stored in the driver's constants.h file.
 */
static inline void print_tcp_settings() {
	printf("INFO:   Port     : %d\n", PORT);
}
#endif /* SEVERITY */

/**
 * Performs setup for tcp on transport layer.
 * Creates necessary data structures and binds user-specified port as well
 * as callback methods for in-going tcp connections.
 * @return 0 if successful, an error code otherwise.
 */
static int medium_setup_tcp() {
  struct addrinfo hints, *servinfo, *p;
  struct sockaddr_storage their_addr;
  socklen_t sin_size;
  int yes = 1;
  int rv;

  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;
  hints.ai_flags = AI_PASSIVE; // use my IP

  if ((rv = getaddrinfo(NULL, "8844", &hints, &servinfo)) != 0) { //TODO change to use constant for port (type of PORT wrong)
    fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
    return -1;
  }

  // loopy through all the results and bind to the first we can
  for (p = servinfo; p != NULL; p = p->ai_next) {
    if ((sockfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol)) == -1) {
      perror("server: socket");
      continue;
    }

    if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1) {
      perror("setsockopt");
      exit(1);
    }

    if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
      close(sockfd);
      perror("server: bind:");
      continue;
    }

    break;
  }

  if (p == NULL) {
    fprintf(stderr, "server: failed to bind\n");
    return -2;
  }

  freeaddrinfo(servinfo); // all done with this structure

  if (listen(sockfd, BACKLOG) == -1) {
    perror("listen");
    exit(1);
  }

  // TODO improve (enable to close and reestablish connections
  sin_size = sizeof(their_addr);
  con = accept(sockfd, (struct sockaddr *)&their_addr, &sin_size);
  if (con == -1) {
    perror("accept");
    exit(EXIT_FAILURE);
  }
  return 0;
}

int init_medium() {
  int rslt;
  // setup transport layer
  rslt = medium_setup_tcp();
  if(rslt != 0) return rslt;

#if SEVERITY >= SEVERITY_INFO
  print_tcp_settings();
#endif

  return rslt;
}
