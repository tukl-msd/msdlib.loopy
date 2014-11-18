/**
 * Implementation of a linked queue.
 * @file
 * @author Thomas Fischer
 * @since: 20.02.2013
 */

#ifndef QUEUEUNTYPED_H_
#define QUEUEUNTYPED_H_

/**
 * Structure for nodes of the linked queue.
 * Contains the stored value as well as a a pointer to the next element in the queue.
 */
typedef struct QueueNode {
	/** Pointer to the next node in the queue */
	struct QueueNode *next;
	/** The value stored at this node */
	int value;
} QueueNode;

/**
 * Structure for a linked queue.
 * A linked queue is a mix of a linked list and a fifo queue.
 * Elements can be appended only at the back and only removed from the front.
 * However, it is possible to peek at all elements, starting with the first
 * and using the next pointer.
 */
typedef struct Queue {
	/** Pointer to the first node in the queue */
	struct QueueNode *first;
	/** Pointer to the last node in the queue */
	struct QueueNode *last;
	/** Number of nodes currently stored in the queue */
	unsigned int size;
	/** Capacity of the queue (maximum number of nodes) */
	unsigned int cap;
} Queue;

/**
 * Allocates memory for an empty queue and returns the pointer.
 * @param cap Maximal capacity of the queue.
 * @return Pointer to a new, empty queue.
 */
Queue* createQueue(int cap);

/**
 * Puts an element into a queue.
 * The element is appended at the back of the queue.
 * @param queue The queue, to which the element should be appended.
 * @param val The element to append.
 * @return 1 if successful, 0 otherwise (i.e. capacity reached)
 */
int put(struct Queue *queue, int val);

/**
 * Checks if a queue contains eny elements or is empty.
 * @param queue The queue to be checked.
 * @return true if the queue contains elements, false otherwise.
 */
int hasElems(Queue *queue);

/**
 * Takes the first element from a queue and returns its value.
 * ALWAYS check the size, before taking elements!
 * @param queue The queue, from which the element should be taken.
 * @return Value of the first element.
 */
int take(struct Queue *queue);

/**
 * Clears all values from the queue.
 * Does however NOT remove free the memory reserved for the queue header itself.
 * @param queue The queue that should be cleared of elements.
 */
void clear(struct Queue *queue);

/**
 * Read the first value of the queue without removing it.
 * @param queue The queue which should be read.
 * @return The first value of the queue.
 */
int peek(struct Queue *queue);



#endif /* QUEUEUNTYPED_H_ */
