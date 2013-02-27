/**
 * @author Thomas Fischer
 * @since 18.02.2013
 */

#include "scheduler.h"
#include <stdlib.h>
#include "constants.h"
#include "queueUntyped.h"
#include "io.h"

void medium_read();
int axi_write(int value, int target);
int axi_read(int *value, int target);

void reset_queues() {
	// TODO set some reset flag
	int i;
	for(i = 0; i <  IN_STREAM_COUNT; i++) clear( inQueue[i]);
	for(i = 0; i < OUT_STREAM_COUNT; i++) clear(outQueue[i]);
	// TODO does this guarantee, that no more values will be written to the MB queues??
}

/**
 * Convenience procedure for writing to a queue.
 * @param pid Port identifier
 * @param val Value to be written
 */
inline void writeQueue(int pid, int val) {
	put(outQueue[pid], val);
}

void schedule() {
	while(1) {
		unsigned char i;
		unsigned int j;

		// receive a package from the interface (or all?)
		// esp stores data packages in mb queue
		medium_read();

		// write data from mb queue to hw queue (if possible)
		for(i = 0; i < IN_STREAM_COUNT; i++) {
			for(j = 0; j < ITERATION_COUNT; j++) {
				// go to next port if the sw queue is empty
				if(inQueue[i]->size == 0) break;

				// try to write, skip if the hw queue is full
				if(axi_write(peek(outQueue[i]), i)) break;

				// remove the read value from the queue
				take(outQueue[i]);

				// if the queue was full beforehand, poll
				if(inQueue[i]->size == SW_QUEUE_SIZE - 1) send_poll(i);
			}
		}

		// read data from hw queue (if available) and cache in mb queue
		// flush sw queue, if it's full or the hw queue is empty
		for(i = 0; i < OUT_STREAM_COUNT; i++) {
			for(j = 0; j < ITERATION_COUNT; j++) {
				// flush, if the sw queue is full
				if(outQueue[i]->size >= SW_QUEUE_SIZE) flush_queue(i);

				int val;
				// try to read
				if(!axi_read(&val, i)) {
					// if successful, store the read value in the sw queue
					writeQueue(i, val);
				} else {
					// otherwise, break read iteration
					break;
				}
			}
			// flush sw queuewrite_all(&outQueue[i]);
			flush_queue(i);
		}
	}
}
