/*
 * @author Thomas Fischer
 * @since 16.04.2013
 */
#include "setup.h"
#include "../io/io.h"

#include <thread>

using namespace std;

thread *writerThread;
thread *readerThread;

void startup() {
	writerThread = new thread(scheduleWriter);
	readerThread = new thread(scheduleReader);
}

void shutdownWriteLoop() {
	// acquire writer lock
	unique_lock<mutex> lock(writer_mutex);

	// flag as inactive
	is_active = false;

	// notify writer (one last time)
	can_write.notify_one();
}

void shutdown() {
	printf("\nkilling I/O threads");

	shutdownWriteLoop();
	writerThread->join(); writerThread = NULL;
	readerThread->join(); readerThread = NULL;
}



