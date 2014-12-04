#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/init.h>

#include <linux/kernel.h>
#include <linux/slab.h>
#include <linux/fs.h>
#include <linux/errno.h>
#include <linux/types.h>
#include <linux/cdev.h>

#include <asm/uaccess.h>

#include "loopy.h"
#include "simple_queue.h"

MODULE_LICENSE("Dual BSD/GPL");


static int loopy_major = LOOPY_MAJOR;
static int loopy_minor = 0;
static int loopy_max_ports = LOOPY_MAX_PORTS;

module_param(loopy_major, int, S_IRUGO);
module_param(loopy_max_ports, int, S_IRUGO);

/**
 * dummy "echo" implementation; replace with DMA access
 */
static simple_queue **queues;

static struct cdev *my_cdev;


/**
 * dummy "echo" implementation; replace with DMA access
 */
static int port_recv(int port, int *value)
{
  if (!pop_simple_queue(queues[port], value)) {
    return -EINVAL;
  } else {
    return 0;
  }
}

/**
 * dummy "echo" implementation; replace with DMA access
 */
static int port_send(int port, int value)
{
  if (!push_simple_queue(queues[port], value)) {
    return -EINVAL;
  } else {
    return 0;
  }
}


/**
 * handle ioctl events. Returns 0 on success, negative error code on error
 */
static long loopy_ioctl(struct file *filp, unsigned int ioctlnum, unsigned long ioctlparam)
{
  loopy_chnl_io io;
  int res;
  switch (ioctlnum) {
    case IOCTL_READ:
      if((res = copy_from_user(&io, (void *)ioctlparam, sizeof(loopy_chnl_io)))) {
        printk(KERN_ERR "loopy: cannot read ioctl user parameter.\n");
        return res;
      }
      if (io.port < 0 || io.port >= loopy_max_ports) {
        return 0;
      }
      res = port_recv(io.port, &io.value);
      if(res < 0) {
        printk(KERN_ERR "loopy: cannot receive value from port %d.\n", io.port);
        return res;
      }

      if((res = copy_to_user((void *)ioctlparam, &io, sizeof(loopy_chnl_io)))) {
        printk(KERN_ERR "loopy: cannot write ioctl user parameter.\n");
      }
      return res;
    case IOCTL_WRITE:
      if((res = copy_from_user(&io, (void *)ioctlparam, sizeof(loopy_chnl_io)))) {
        printk(KERN_ERR "loopy: cannot read ioctl user parameter.\n");
        return res;
      }
      return port_send(io.port, io.value);
      return 0;
    default:
      return -ENOTTY; /* not successfull */
  }
}

static const struct file_operations loopy_fops = {
  .owner = THIS_MODULE,
  .unlocked_ioctl = loopy_ioctl,
};


static void free_queues(void)
{
  int i;

  if (queues != NULL) {
    for (i = 0; i < loopy_max_ports; i++) {
      if(queues[i] != NULL) {
        free_simple_queue(queues[i]);
      }
    }
  }
}

static int __init loopy_init(void)
{
  int result, i;
  dev_t dev = 0;

  if (loopy_major) {
    dev = MKDEV(loopy_major, loopy_minor);
    result = register_chrdev_region(dev, 1, "loopy");
  } else {
    result = alloc_chrdev_region(&dev, loopy_minor, 1, "loopy");
    loopy_major = MAJOR(dev);
  }

  if (result < 0) {
    printk(KERN_WARNING "loopy: can't get major %d\n", loopy_major);
    return result;
  }

  my_cdev = cdev_alloc();
  my_cdev->owner = THIS_MODULE;
  my_cdev->ops = &loopy_fops;
  result = cdev_add(my_cdev, dev, 1);

  if (result < 0) {
    printk(KERN_WARNING "loopy: can't allocate cdev");
    return result;
  }

  queues = kmalloc(loopy_max_ports * sizeof(simple_queue *), GFP_KERNEL);
  if (queues == NULL) {
    printk(KERN_ERR "loopy: could not allocate enough memory for queues");
    goto fail;
  }

  for (i = 0; i < loopy_max_ports; i++) {
    queues[i] = init_simple_queue();
    if (queues[i] == NULL) {
      printk(KERN_ERR "loopy: could not allocate enough memory for queues");
      goto fail;
    }
  }

  return 0; /* success */

  fail:
  free_queues();
  return -EINVAL;
}

static void __exit loopy_exit(void)
{
  dev_t devno = MKDEV(loopy_major, loopy_minor);

  cdev_del(my_cdev);
  unregister_chrdev_region(devno, 1);

  free_queues();
}

module_init(loopy_init);
module_exit(loopy_exit);

