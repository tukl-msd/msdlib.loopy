#ifndef SIMPLE_QUEUE_H
#define SIMPLE_QUEUE_H

struct queue_entry {
  int val;
  struct queue_entry *next;
  struct queue_entry *prev;
};

struct simple_queue {
  struct queue_entry *fst;
  struct queue_entry *lst;
};
typedef struct simple_queue simple_queue;


simple_queue *init_simple_queue(void);

/**
 * push a value to the head; return 1 on success, 0 on error
 */
int push_simple_queue(simple_queue *q, int val);

/**
 * pop a value from the tail; returns 1 on success, 0 on error
 */
int pop_simple_queue(simple_queue *q, int *val);

/**
 * returns 1 if the queue is empty
 */
int empty_simple_queue(simple_queue *q);

/**
 * frees the resources associated with the given queue
 */
void free_simple_queue(simple_queue *q);

#endif
