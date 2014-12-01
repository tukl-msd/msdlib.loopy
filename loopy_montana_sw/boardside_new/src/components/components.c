#include "components.h"

#include <unistd.h>
#include <fcntl.h>
#include "../constants.h"
#include "../io.h"


//TODO (MW) currently fixed at number of devices registered by the Milano module
#define DMA_NUM 4

// procedures of components
/**
 * Initialises all components on this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium this board is attached with.
 */
void init_components ( ) {
    inQueue[0] = createQueue(8);
    outQueueCap[0] = 1024;
    isPolling[0] = 1;
    pollCount[0] = 64;
}

/**
 * Resets all components in this board.
 * This includes gpio components and user-defined IPCores,
 * but not the communication medium, this board is attached with.
 */
void reset_components ( ) { }

/**
 * Write a value to an AXI stream.
 * @param val Value to be written to the stream.
 * @param target Target stream identifier.
 */
int axi_write ( int val, int target ) {
  int fd;
  char buffer[20];
  int cx;

  if (target < DMA_NUM) {
    cx = snprintf(buffer, 20, "/dev/axi_dma_%d", target);
    fd = open(buffer, O_WRONLY);
    write(fd, &val, sizeof(int));
    close(fd);
    return 1;
  } else {
    fprintf(stderr, "ERROR: target larger than maximum available DMA port: %d", target);
    return 0;
  }
}

/**
 * Read a value from an AXI stream.
 * @param val Pointer to the memory area, where the read value will be stored.
 * @param target Target stream identifier.
 */
int axi_read ( int *val, int target ) {
  int fd;
  char buffer[20];
  int cx;

  if (target < DMA_NUM) {
    cx = snprintf(buffer, 20, "/dev/axi_dma_%d", target);
    fd = open(buffer, O_RDONLY);
    read(fd, val, sizeof(int));
    close(fd);
    return 1;
  } else {
    fprintf(stderr, "ERROR: target larger than maximum available DMA port: %d", target);
    *val = 0;
    return 1;
  }
}

