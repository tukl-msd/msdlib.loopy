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
#include "../api/portIn.h"
#include "../api/portOut.h"
#include "../api/gpio.h"
//#include "../linkedQueue.h"

/** Scheduling loop for the writer thread */
void scheduleWriter();
/** Scheduling loop for the reader thread */
void scheduleReader();
/**
 * Requests several values for a polling port from the board.
 * @param pid Port id of the requesting port.
 * @param count Number of requested values.
 */
void send_poll(unsigned char pid, unsigned int count);
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
 * @param val Values sent to the port.
 * @param size Number of values sent to the port
 */
void read(unsigned char pid, int val[], int size);

/**
 * Processes an incoming poll.
 * @param pid Target port id of the pol.
 */
void recv_poll(unsigned char pid);

/**
 * Process an incoming gpio value.
 * @param gid Identifier of the changed gpio component.
 * @param val The new value of the gpio component.
 */
void recv_gpio(unsigned char gid, unsigned char val);

// TODO move this to a non-generic file...
/** Instance pointer to the communication medium for this writer/reader. */
extern interface *intrfc;

/************************** LOCKS *************************/
/** Global writer lock. Use, whenever interacting with shared objects. */
extern std::mutex writer_mutex;
/** Notify this variable, when new values can be sent to the board. */
extern std::condition_variable can_write;
/** flag stating if the loops should terminate. */
extern bool is_active;

/********************* SHARED MEMORY *********************/
/** Pointer array of all in-going ports. */
extern abstractInPort   *inPorts[];
/** Pointer array of all out-going ports. */
extern abstractOutPort *outPorts[];

/** Pointers to the in-going gp queues */
extern gpi *gpis[];
/** Pointers to the out-going gp queues */
extern gpo *gpos[];

#endif /* IO_H_ */
