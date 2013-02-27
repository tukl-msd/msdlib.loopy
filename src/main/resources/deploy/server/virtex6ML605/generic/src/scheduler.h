/**
 * A primitive scheduler.
 * Writes n times on all input ports, read n times from all output ports, then tries to read from the medium.
 * n is defined by the user, per default the maximal number of values in the Microblaze queue.
 * More values cannot be stored, and consequently cannot be read or written.
 * @file
 * @author Thomas Fischer
 * @since 18.02.2013
 */
#ifndef SCHEDULER_H_
#define SCHEDULER_H_

/**
 * Starts the scheduling loop.
 * Will dod the following things in this loop:
 * -write values from Microblaze input queue to hardware input queue for each input stream
 * -write values from hardware output queue to Microblaze output queue for each output stream
 * -write values from medium to Microblaze input queue
 * -write values from Microblaze output queue to medium
 */
void schedule();

#endif /* SCHEDULER_H_ */
