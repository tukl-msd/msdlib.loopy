#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include "loopy_access.h"

struct loopy_t
{
  int fd;
};


loopy_t *loopy_open(void)
{
  loopy_t *loopy;
  char *device;

  loopy = (loopy_t *)malloc(sizeof(loopy_t));
  if (loopy == NULL) {
    printf("Cannot allocate memory for loopy\n");
    return NULL;
  }
  asprintf(&device, "/dev/%s", DEVICE_NAME);
  loopy->fd = open(device, O_RDWR | O_SYNC);
  if (loopy->fd < 0) {
    printf("Cannot open device '%s' (result: %d)\n", device, loopy->fd);
    printf("Error: %s\n", strerror(errno));
    free(loopy);
    return NULL;
  }

  return loopy;
}

void loopy_close(loopy_t *loopy)
{
  // Close the device file
  close(loopy->fd);
  free(loopy);
}

int loopy_write(loopy_t *loopy, int port, int value)
{
  loopy_chnl_io io;

  io.port = port;
  io.value = value;

  return ioctl(loopy->fd, IOCTL_WRITE, &io);
}

int loopy_read(loopy_t *loopy, int port, int *value)
{
  loopy_chnl_io io;
  int res;
  io.port = port;

  if ((res = ioctl(loopy->fd, IOCTL_READ, &io))) {
    return res;
  }
  *value = io.value;
  return 0;
}

