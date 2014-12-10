#include "scheduler.h"

int axi_read ( int *val, int target );
int axi_write ( int val, int target );
int medium_read();
#include "constants.h"
#include "io.h"
#include "queueUntyped.h"
// procedures of scheduler
/**
 * Starts the scheduling loop.
 * The scheduling loop performs the following actions in each iteration:
 *  - read and process messages from the medium
 *  - write values from Microblaze input queue to hardware input queue for each input stream
 *  - write values from hardware output queue to the medium (caches several values before sending)
 */
void schedule ( ) {
    unsigned int pid;
    unsigned int i;

    while(1) {
        // receive all available packages from the interface
        // esp stores data packages in sw queue
        printf("\nreading from ethernet...");
        while(medium_read()) {
          printf("\nfinished reading next package");
        }

        // write data from sw queue to hw queue (if possible)
        printf("\nwriting data to DMA controller...");
        for(pid = 0; pid < IN_STREAM_COUNT; pid++) {
            printf("\ntransfering port %d", pid);
            for(i = 0; i < inQueue[pid]->cap; i++) {
                printf("\nwriting package %d", i);
                // go to next port if the sw queue is empty
                if(inQueue[pid]->size == 0) break;

                // try to write, skip if the hw queue is full
                if(axi_write(peek(inQueue[pid]), pid)) {
                      log_fine("failed to write to AXI stream");
                    break;
                }

                // remove the read value from the queue
                take(inQueue[pid]);

                // if the queue was full beforehand, poll
                if(inQueue[pid]->size == inQueue[pid]->cap - 1) send_poll(pid);
            }
        }

        // read data from hw queue (if available) and cache in sw queue
        // flush sw queue, if it's full or the hw queue is empty
        printf("\nreading data from DMA controller...");
        for(pid = 0; pid < OUT_STREAM_COUNT; pid++) {
            printf("\nreading data for port %d", pid);
            for(i = 0; i < outQueueCap[pid] && ((!isPolling[pid]) || pollCount[pid] > 0); i++) {
                printf("\nreading package %d", i);
                // try to read, break if if fails
                if(axi_read(&outQueue[outQueueSize], pid)) break;

                // otherwise increment the queue size counter
                outQueueSize++;

                // decrement the poll counter (if the port was polling)
                if(isPolling[pid]) pollCount[pid]--;
            }
            // flush sw queue
            if(flush_queue(pid)) {
                // in this case, sending failed. Terminate (reasons have already been printed)
                printf("terminating...\n");
                return;
            }
            outQueueSize = 0;
        }
    }
}

