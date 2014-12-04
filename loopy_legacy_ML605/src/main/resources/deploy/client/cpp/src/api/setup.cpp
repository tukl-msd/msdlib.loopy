/*
 * @author Thomas Fischer
 * @author Mathias Weber
 * @since 16.04.2013
 */

#include "setup.h"
#include "../logger.h"
#include "../io/io.h"

#include <thread>

std::thread *writerThread;
std::thread *readerThread;

/**
 * Starts writer and reader threads
 */
static void startThreads() {
	writerThread = new std::thread(scheduleWriter);
	readerThread = new std::thread(scheduleReader);
}

#ifdef IP
void startup() {
	intrfc = new ethernet(IP, PORT);
	startThreads();
}
#else
void startup(std::string ip) {
	intrfc = new ethernet(ip.c_str(), PORT);
	startThreads();
}
#endif

void shutdownWriteLoop() {
	// acquire writer lock
    std::unique_lock<std::mutex> lock(writer_mutex);

	// flag as inactive
	is_active = false;

	// notify writer (one last time)
	can_write.notify_one();
}

void shutdown() {
	logger_host << "killing I/O threads" << std::endl;

	shutdownWriteLoop();
	writerThread->join(); writerThread = NULL;
	readerThread->join(); readerThread = NULL;
}

void check_checksum() {
    send_checksum_request();
    // TODO maybe wait for the checksum check to complete?
}




