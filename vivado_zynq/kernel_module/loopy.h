#ifndef _LOOPY_H_
#define _LOOPY_H_

#include <linux/ioctl.h>

#define DEVICE_NAME "loopy"

#ifndef LOOPY_MAJOR
#define LOOPY_MAJOR 0
#endif

#ifndef LOOPY_MAX_PORTS
#define LOOPY_MAX_PORTS 16
#endif


#define LOOPY_IOC_MAGIC  'l'

struct loopy_chnl_io
{
  int port;
  int value;
};
typedef struct loopy_chnl_io loopy_chnl_io;

#define IOCTL_READ    _IOWR(LOOPY_IOC_MAGIC, 1, loopy_chnl_io *)
#define IOCTL_WRITE   _IOW(LOOPY_IOC_MAGIC, 2, loopy_chnl_io *)


#endif /* _LOOPY_H_ */
