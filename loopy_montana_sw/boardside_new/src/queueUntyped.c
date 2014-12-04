/*
 * queueUntyped.c
 * @author Thomas Fischer
 * @since 20.02.2013
 */

#include "queueUntyped.h"
#include <stdlib.h>

/**
 * Allocates memory for a new element, initialises its values and returns its pointer.
 * @param val pointer to the new element.
 * @return Pointer to the new element.
 */
static struct QueueNode* createElem(int val) {
	// allocate a new QueueNode
	struct QueueNode *elem = malloc(sizeof(struct QueueNode));

	// set next pointer and and value
	elem->next  = NULL;
	elem->value = val;

	// return the pointer
	return elem;
}

/**
 * Removes an element and all its successors.
 * Frees memory of both, the node and its value.
 * @param elem Element to remove.
 */
static void clearElems(struct QueueNode *elem) {
	if(elem->next != NULL) clearElems(elem->next);
//	free(elem->value);
	free(elem);
}

int hasElems(struct Queue *queue) {
	return queue->size > 0;
}

struct Queue* createQueue(int cap) {
	// allocate a new IntQueue
	struct Queue *q = malloc(sizeof(struct Queue));

	// set pointers and size to defaults
	q->first = NULL;
	q->last  = NULL;
	q->size  = 0;
	q->cap   = cap;

	// return the pointer
	return q;
}

int put(struct Queue *queue, int val) {
	// if the queue has reached its capacity, abort
	if(queue->size >= queue->cap) return 0;

	// allocate a new QueueNode
	struct QueueNode *elem = createElem(val);

	// if the queue was empty, use this as first
	if(queue->first == NULL) queue->first = elem;
	// otherwise append it to the last element
	else queue->last->next = elem;

	// set new last pointer and size
	queue->last = elem;
	queue->size++;

	return 1;
}

int take(struct Queue *queue) {
	// fail, if the queue is empty
	if(queue->size == 0) return 0; //TODO not a good error handling!

	// otherwise get the first element and its value
	QueueNode *first = queue->first;
	int val = first->value;

	// remove the element from the queue and the heap
	queue->first = queue->first->next;
	queue->size--;
//	free(first->value);
	free(first);

	// return the value
	return val;
}

void clear(struct Queue *queue) {
	// if the queue already is empty, do nothing
	if(queue->size == 0) return;

	// otherwise remove all elements
	clearElems(queue->first);

	// reset pointers and size
	queue->first = NULL;
	queue->last  = NULL;
	queue->size  = 0;
}

int peek(struct Queue *queue) {
	return queue->first->value;
}
