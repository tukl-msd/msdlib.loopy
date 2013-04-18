/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

#include "scheduler.h"
#include <stdlib.h>
#include "constants.h"
#include "queueUntyped.h"
#include "io.h"

#include "medium/message.h"

void medium_read();
int axi_write(int value, int target);
int axi_read(int *value, int target);

void schedule() {

	while(1) {
		unsigned int pid;
		unsigned int i;

		// receive a package from the interface (or all?)
		// esp stores data packages in mb queue
		medium_read();

		// write data from mb queue to hw queue (if possible)
		for(pid = 0; pid < IN_STREAM_COUNT; pid++) {
			for(i = 0; i < ITERATION_COUNT; i++) {
				// go to next port if the sw queue is empty
				if(inQueue[pid]->size == 0) break;

				// try to write, skip if the hw queue is full
				if(axi_write(peek(inQueue[pid]), pid)) {
					if(DEBUG) xil_printf("\nfailed to write to AXI stream");
					break;
				}

				// remove the read value from the queue
				take(inQueue[pid]);

				// if the queue was full beforehand, poll
				if(inQueue[pid]->size == inQueue[pid]->cap - 1) send_poll(pid);
			}
		}

		// read data from hw queue (if available) and cache in mb queue
		// flush sw queue, if it's full or the hw queue is empty
		for(pid = 0; pid < OUT_STREAM_COUNT; pid++) {
			for(i = 0; i < ITERATION_COUNT; i++) {
				// break, if the sw queue is full
				if(outQueueSize == outQueueCap[pid]) {
					if(DEBUG) xil_printf("queue full");
					break;
				}

				int val = 0;

				// break, if there is no value
				if(axi_read(&val, pid)) break;

				// otherwise store the value in the sw queue
				outQueue[outQueueSize] = val;
				outQueueSize++;
			}
			// flush sw queue
			flush_queue(pid);
			outQueueSize = 0;
		}
	}
}
