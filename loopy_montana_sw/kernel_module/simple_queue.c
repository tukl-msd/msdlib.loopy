#include <linux/slab.h>
#include "simple_queue.h"

simple_queue *init_simple_queue(void)
{
  simple_queue *q;

  q = kmalloc(sizeof(simple_queue), GFP_KERNEL);
  if (q == NULL) {
    printk(KERN_ERR "Not enough memory to allocate simple_queue");
    return NULL;
  }

  q->fst = NULL;
  q->lst = NULL;

  return q;
}

int empty_simple_queue(simple_queue *q)
{
  if (q->fst == NULL && q->lst == NULL) {
    return 1;
  } else {
    return 0;
  }
}

int push_simple_queue(simple_queue *q, int val)
{
  struct queue_entry *qe;

  qe = kmalloc(sizeof(struct queue_entry), GFP_KERNEL);
  if (qe == NULL) {
    printk(KERN_ERR "Not enough memory to allocate queue entry");
    return 0;
  }

  qe->val = val;

  if(empty_simple_queue(q)) {
    q->fst = qe;
    q->lst = qe;
  } else {
    qe->next = q->fst;
    q->fst->prev = qe;
    q->fst = qe;
  }
  return 1;
}


int pop_simple_queue(simple_queue *q, int *val)
{
  struct queue_entry *qe;

  if (empty_simple_queue(q)) {
    return 0;
  } else {
    qe = q->lst;
    *val = qe->val;
    if (q->fst == q->lst) {
      q->fst = NULL;
      q->lst = NULL;
    } else {
      q->lst = q->lst->prev;
    }
    kfree(qe);
    return 1;
  }
}

void free_simple_queue(simple_queue *q)
{
  struct queue_entry *tmp;
  struct queue_entry *cur;

  cur = q->fst;

  while (cur != NULL) {
    tmp = cur;
    cur = cur->next;
    kfree(tmp);
  }
}

