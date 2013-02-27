/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

#include "scheduler.h"
#include <stdlib.h>
#include "constants.h"
#include "queueUntyped.h"

/** Microblaze  input queues */ Queue  inQueue [ IN_STREAM_COUNT];
/** Microblaze output queues */ Queue outQueue [OUT_STREAM_COUNT];

void medium_read();
int axi_write(int value, int target);
int axi_read(int *value, int target);
void medium_axi_write(struct Queue *q, unsigned int size);

void reset_queues() {
	// TODO set some reset flag
	int i;
	for(i = 0; i <  IN_STREAM_COUNT; i++) clear( &inQueue[i]);
	for(i = 0; i < OUT_STREAM_COUNT; i++) clear(&outQueue[i]);
	// TODO does this guarantee, that no more values will be written to the MB queues??
}

/**
 * Convenience procedure for reading from a queue.
 * @param pid Port identifier
 * @return The read value
 */
inline int readQueue(int pid) {
	return *(int*)take(&inQueue[pid]);
}

/**
 * Convenience procedure for writing to a queue.
 * @param pid Port identifier
 * @param val Value to be written
 */
inline void writeQueue(int pid, int val) {
	put(&outQueue[pid], &val);
}

void schedule() {
	while(1) {
		unsigned int i, j;

		// receive a package from the interface (or all?)
		// esp stores data packages in mb queue
		medium_read();

		// write data from mb queue to hw queue (if possible)
		for(i = 0; i < IN_STREAM_COUNT; i++) {
			for(j = 0; j < ITERATION_COUNT; j++) {
				// skip to next point if the sw queue is empty
				if(inQueue[i].size == 0) break;
				// try to write, skip if the hw queue is full
				if(!axi_write(readQueue(i), i)) break;
			}
		}

		// read data from hw queue (if available) and cache in mb queue
		// TODO merge this step with the next one,
		//      i.e. do not buffer for each component individually, but use one
		//      buffer for all components and directly send the message here
		//      if this buffer is full or the hw queue is empty
		for(i = 0; i < OUT_STREAM_COUNT; i++) {
			for(j = 0; j < ITERATION_COUNT; j++) {
				// skip to next point, if the sw queue is full
				if(outQueue[i].size >= SW_QUEUE_SIZE) break;
				int val;
				// try to read, skip if the hw queue is empty
				if(!axi_read(&val, i)) break;
				writeQueue(i, val);
			}
		}

		// TODO write outgoing packages to the medium
//		for(i = 0; i < OUT_STREAM_COUNT; i++) {
//			for(j = 0; j < ITERATION_COUNT; j++) {
//				medium_axi_write(&outQueue[i], i);
//			}
//		}
		// medium_write? problem: write from all queues?
		// does it even make sense to split server side by components?
		// kind of - less application headers, i guess... (considering smaller round robin reads or multiple cycles per computation)
		// wouldn't it be more useful to have a single outgoing queue which stores values and targets or something??
		// --> more complex "elem" --> generics in C?
	}
}
