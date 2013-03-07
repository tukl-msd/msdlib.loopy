/**
 * Client version for the generic linked queue used by both,
 * host-side and board-side driver.
 * This version is templated and threadsafe.
 * That means, all values stored within the queue will have the same
 * type. Locks prevent parallel access to the queue.
 * In addition, pointers handed out by the queue are not RAW pointers,
 * but shared pointers, relieving the user of the necessity to manually
 * free the memory they are pointing to.
 * @file
 * @author Thomas Fischer
 * @since 21.02.2013
 */

#ifndef LINKEDQUEUE_H_
#define LINKEDQUEUE_H_

#include <memory>
#include <condition_variable>
#include <mutex>

template<class T>
class LinkedQueue;

/**
 * Template for nodes of the linked queue.
 * Contains the stored value as well as a a pointer to the next element in the queue.
 */
template <class T>
class Node {
friend class LinkedQueue<T>;
private:
	/** Pointer to the next node in the queue */
	Node<T> *next;
	/** Pointer to the value stored at this node */
	std::shared_ptr<T> value;
public:
	/**
	 * Instantiates a node with the provided value.
	 * @param val Value, which this node points to.
	 */
	Node(std::shared_ptr<T> val) : next(NULL), value(val){ } // TODO pointer to value?
	~Node() {
		// delete the next node (if any)
		delete next;
		// reset the pointer (will automatically delete value if no more pointers exist)
		value.reset();
	}
};

/**
 * Template for a linked queue.
 * A linked queue is a mix of a linked list and a fifo queue.
 * Elements can be appended only at the back and only removed from the front.
 * However, it is possible to peek at all elements, starting with the first
 * and using the next pointer.
 */
template<class T>
class LinkedQueue {
private:
	// variables for locking
	std::recursive_mutex mutex;
	std::condition_variable_any is_empty;
	std::condition_variable_any is_not_empty;

	// variables for actual functionality
	/** Pointer to the first node in the queue */
	Node<T> *first;
	/** Pointer to the last node in the queue */
	Node<T> *last;
	/** Number of nodes stored in the queue */
	unsigned int nodeCount;
public:
	/**
	 * Instantiates a LinkedQueue without any nodes.
	 */
	LinkedQueue() : first(NULL), last(NULL), nodeCount(0) { }
	~LinkedQueue() { this->clear(); }

	/**
	 * Clears all values from the queue.
	 * Does however NOT free the memory reserved for the queue header itself.
	 */
	void clear();

	/**
	 * Checks if the queue is empty.
	 * @return true, if empty, false otherwise
	 */
	bool empty();

	/**
	 * Checks the size of the queue.
	 * @return The size of the queue.
	 */
	int size();

	/**
	 * Read the first value of the queue without removing it.
	 * @return Shared pointer to the first element of the queue.
	 */
	std::shared_ptr<T> peek();

	/**
	 * Read the n-th value of the queue without removing any values.
	 * @param n The index of the value that should be peeked at.
	 * @return Shared pointer to the n-th element of the queue if the
	 *         queue is large enough, NULL, if no element with this index
	 *         exists (i.e. n >= this.size())
	 */
	std::shared_ptr<T> peek(unsigned int n);

	/**
	 * Takes the first element from the queue and returns its value.
	 * Waits, if the queue is empty.
	 * ALWAYS check the size, before taking elements!
	 * @return Shared pointer to the first element of the queue.
	 */
	std::shared_ptr<T> take();

	/**
	 * Puts an element into the queue.
	 * The element is appended at the back of the queue.
	 * @param val Shared pointer to the element to append.
	 */
	void put(std::shared_ptr<T> val);
};

template<class T>
void LinkedQueue<T>::clear() {
	/** if the queue already is empty, do nothing */
	if(empty()) return;

	/** otherwise, acquire the lock */
	std::lock_guard<std::recursive_mutex> lock(mutex);

	/**remove all elements */
//	first->clear();
	delete first;

	/** reset pointers and nodeCount */
	first = NULL;
	last  = NULL;
	nodeCount  = 0;
}

template<class T>
bool LinkedQueue<T>::empty() {
	return nodeCount == 0;
}

template<class T>
int LinkedQueue<T>::size() {
	return nodeCount;
}

template<class T>
std::shared_ptr<T> LinkedQueue<T>::peek() {
	/** wait, if the queue is empty */
	/**if(nodeCount == 0) //fail */
	if(nodeCount == 0) return NULL;

	/** returns null on an empty queue! */
	return first->value;
}

template<class T>
std::shared_ptr<T> LinkedQueue<T>::peek(unsigned int n) {
	/** if the queue doesn't contain enough elements, return null */
	if(nodeCount <= n) return NULL;

	unsigned int i = n;
	Node<T> *node = first;

	while (i > 0) {
		node = node->next;
		i--;
	}

	return node->value;
}

template<class T>
std::shared_ptr<T> LinkedQueue<T>::take() {
	/** first of all, acquire the lock */
	std::unique_lock<std::recursive_mutex> lock(mutex);

	/** wait, if the queue is empty */
	if(nodeCount == 0) is_not_empty.wait(lock);

	/** otherwise get first node */
	Node<T> *tmp = first;

	// duplicate the shared pointer to its value
	std::shared_ptr<T> val = tmp->value;

	/** remove the element from the queue and from the heap */
	first = tmp->next;
	nodeCount--;

	tmp->next = NULL;
	delete(tmp);

	/** return the pointer to the value */
	return val;
}

template<class T>
void LinkedQueue<T>::put(std::shared_ptr<T> val) {
	/** first of all, acquire the lock */
	std::lock_guard<std::recursive_mutex> lock(mutex);

	/** allocate a new node */
	 Node<T> *elem = new Node<T>(val);

	/** if the queue was empty, use this as first */
	if(empty()) first = elem;
	/** otherwise append it to the last element */
	else last->next = elem;

	/** set new last pointer and nodeCount */
	last = elem;
	nodeCount++;

	/** notify threads waiting on values */
	is_not_empty.notify_all();
}

#endif /* LINKEDQUEUE_H_ */
