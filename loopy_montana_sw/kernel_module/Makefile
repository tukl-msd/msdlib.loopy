MYCC=gcc
MYCFLAGS=-I.
MYDEPS=loopy_access.h
MYOBJ=test.o loopy_access.o

loopy-objs := main.o simple_queue.o
obj-m := loopy.o

%.o: %.c $(MYDEPS)
	$(MYCC) -c -o $@ $< $(MYCFLAGS)

test: $(MYOBJ)
	$(MYCC) -o $@ $^ $(MYCFLAGS)


KERNELDIR ?= /lib/modules/$(shell uname -r)/build
PWD       := $(shell pwd)

modules:
	$(MAKE) -C $(KERNELDIR) M=$(PWD) modules

clean:
	rm -rf *.o *~ .depend .*.cmd *.ko *.mod.c .tmp_versions


