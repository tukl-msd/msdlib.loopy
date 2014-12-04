#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "loopy_access.h"

int main(int argc, char **argv)
{
  loopy_t *loopy;
  int r;

  loopy = loopy_open();
  if (loopy == NULL) {
    printf("Cannot initialize loopy\n");
    return 1;
  }

  if (loopy_write(loopy, 0, 42) < 0) {
    printf("Error writing to loopy device\n");
    return 1;
  }

  if (loopy_read(loopy, 0, &r) < 0) {
    printf("Error reading from loopy device\n");
    return 1;
  }

  printf("Result from loopy: %d\n", r);

  return 0;
}
