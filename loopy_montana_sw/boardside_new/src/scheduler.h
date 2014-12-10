/**
 * A primitive scheduler.
 * Reads values from the medium, shifts values between Microblaze and VHDL components,
 * and writes results back to the medium.
 * @file
 * @author Thomas Fischer
 * @since 18.02.2013
 */
#ifndef SCHEDULER_H_
#define SCHEDULER_H_

// procedures of scheduler
/**
 * Starts the scheduling loop.
 * The scheduling loop performs the following actions in each iteration:
 *  - read and process messages from the medium
 *  - write values from Microblaze input queue to hardware input queue for each input stream
 *  - write values from hardware output queue to the medium (caches several values before sending)
 */
void schedule ( );

#endif /* SCHEDULER_H_ */