/**
 * Describes the writer and reader threads as well as types and
 * utility functions used by these.
 * @file
 * @author Thomas Fischer
 * @since 18.02.2013
 */

#ifndef IO_H_
#define IO_H_

// pointer
#include <memory>

// locking
#include <atomic>
#include <mutex>
#include <condition_variable>

// data types
#include <vector>

#include "interface.h"
//#include "state.h"
#include "../api/port.h"
//#include "../linkedQueue.h"

/** An Exception that marks a failed write operation on a port. */
class writeException {};
/** An Exception that marks a failed read operation on a port.  */
class readException {};

/** Scheduling loop for the writer thread */
void scheduleWriter();
/** Scheduling loop for the reader thread */
void scheduleReader();
/**
 * Processes a server acknowledgment.
 * Removes the number of acknowledged values from the queue and
 * updates states accordingly.
 * @param pid Port id of the acknowledging port.
 * @param count Number of values acknowledged.
 */
void acknowledge(unsigned char pid, unsigned int count);
/**
 * Processes an incoming data package.
 * Adds the data either to the value queue of the target port or
 * to the first task of the task queue (if a task exists).
 * @param pid Target port id of the value.
 * @param val Value sent to the port.
 */
void read(int pid, int val);

// TODO move this to a non-generic file...
/** Instance pointer to the communication medium for this writer/reader. */
extern interface *intrfc;

/********************* LOCKS ********************/
/** Global writer lock. Use, whenever interacting with shared objects. */
extern std::mutex writer_mutex;
/** Notify this variable, when new values can be sent to the board. */
extern std::condition_variable can_write;
/** flag stating if the loops should terminate. */
extern bool is_active;

/********************* SHARED MEMORY *********************/
/** Pointer array of all in-going ports. */
extern in   *inPorts[];
/** Pointer array of all out-going ports. */
extern out *outPorts[];

/** Pointers to the in-going gp queues */
extern std::shared_ptr<std::atomic<int>>    gpi[];
/** Pointers to the out-going port queues */
extern std::shared_ptr<std::atomic<int>>    gpo[];


#endif /* IO_H_ */
