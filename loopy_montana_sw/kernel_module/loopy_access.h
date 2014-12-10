#ifndef LOOPY_ACCESS_H
#define LOOPY_ACCESS_H

#include "loopy.h"

struct loopy_t;
typedef struct loopy_t loopy_t;


loopy_t *loopy_open(void);

void loopy_close(loopy_t *loopy);

int loopy_write(loopy_t *loopy, int port, int value);

int loopy_read(loopy_t *loopy, int port, int *value);

#endif

