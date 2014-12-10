/**
 * Defines several constants used by the server.
 * This includes medium-specific configuration.
 * @file
 */
#ifndef CONSTANTS_H_
#define CONSTANTS_H_

#include <stdio.h>
// definitions of constants
/** Severity value of errors. */
#define SEVERITY_ERROR 0
/** Severity value of warnings. */
#define SEVERITY_WARN 1
/** Severity value of info messages. */
#define SEVERITY_INFO 2
/** Severity value of debug messages. */
#define SEVERITY_FINE 3
/** Severity value of finer debug messages. */
#define SEVERITY_FINER 4
/** Severity value of finest debug messages. */
#define SEVERITY_FINEST 5
/** Indicates, if additional messages should be logged on the console. */
#define SEVERITY 2
/** With the chosen debug level, errors will be reported to the host driver over Ethernet. */
#define log_error(...) printf("\n"); printf(__VA_ARGS__)
/** With the chosen debug level, warnings will be reported to the host driver over Ethernet. */
#define log_warn(...) printf("\n"); printf(__VA_ARGS__)
/** With the chosen debug level, info messages will be reported to the host driver over Ethernet. */
#define log_info(...) printf("\n"); printf(__VA_ARGS__)
/** With the chosen debug level, fine info messages will not be reported to the host driver. */
#define log_fine(...)
/** With the chosen debug level, finer info messages will not be reported to the host driver. */
#define log_finer(...)
/** With the chosen debug level, finest info messages will not be reported to the host driver. */
#define log_finest(...)
/** Maximal size of out-going software queues. */
#define MAX_OUT_SW_QUEUE_SIZE 1024
/** Denotes protocol version, that should be used for sending messages. */
#define PROTO_VERSION 1
/** The checksum of the project file this board side driver was generated from */
#define CHECKSUM "6c6687528f5a2a9f16f23a19b05f9066"
/** The checksum of the project file this board side driver was generated from */
#define CHECKSUM1 0x6c668752
/** The checksum of the project file this board side driver was generated from */
#define CHECKSUM2 0x8f5a2a9f
/** The checksum of the project file this board side driver was generated from */
#define CHECKSUM3 0x16f23a19
/** The checksum of the project file this board side driver was generated from */
#define CHECKSUM4 0xb05f9066
/** The first  8 bits of the MAC address of this board. */
#define MAC_1 0x00
/** The second 8 bits of the MAC address of this board. */
#define MAC_2 0x0a
/** The third  8 bits of the MAC address of this board. */
#define MAC_3 0x35
/** The fourth 8 bits of the MAC address of this board. */
#define MAC_4 0x02
/** The fifth  8 bits of the MAC address of this board. */
#define MAC_5 0x31
/** The sixth  8 bits of the MAC address of this board. */
#define MAC_6 0x95
/** DHCP flag */
#define DHCP 1
/** DHCP timeout (in seconds) */
#define DHCP_MAX_ATTEMPTS 10
/** The port for this boards TCP-connection. */
#define PORT 8844
/** Reception timeout for an attempt to free memory. */
#define TIMEOUT 2
/** Number of in-going stream interfaces. */
#define IN_STREAM_COUNT 1
/** Number of out-going stream interfaces. */
#define OUT_STREAM_COUNT 1
/** Number of gpi components */
#define gpi_count 0
/** Number of gpo components */
#define gpo_count 0

#endif /* CONSTANTS_H_ */
