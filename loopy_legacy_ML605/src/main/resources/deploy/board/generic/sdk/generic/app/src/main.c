/**
 * Main of the board-side application.
 *
 * Initialises the boards components and starts the scheduling loop.
 *
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

// forward declarations
/** Xilinx platform-initalisation */
void init_platform();
/** initialise the communication medium */
int  init_medium();
void init_components();
void init_queue();

void schedule();

void reset_components();
void reset_queues();

/** Xilinx platform-cleanup */
void cleanup_platform();

/**
 * Resets all components and corresponding queues.
 */
void reset() {
    reset_components();
    reset_queues();
}

/**
 * Initialises components and starts scheduling loop.
 * @return 0.
 */
int main() {
	// initialise the platform. This also sets up the interrupt controller
	init_platform();

	// initialise all components
	init_components();

	// perform medium-specific initialization
	if(init_medium() < 0) return 1;

	// start the scheduler
	schedule();

	// cleanup after failure
	cleanup_platform();

	return 0;
}

