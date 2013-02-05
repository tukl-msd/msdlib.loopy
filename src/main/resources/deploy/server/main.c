/*
 * @file
 * @author Thomas Fischer
 * @since 01.02.2013
 */

//#include "medium/medium.h"
//#include "platform.h"
//#include "components/components.h"

// forward definitions
void init_platform();
void init_medium();
void init_components();
void start_application();
void cleanup_platform();

// main method
int main() {
	// initialise the platform. This also sets up the interrupt controller
	init_platform();

	// initialise Ethernet
	init_medium();

	// initialise all components
	init_components();

	/* start the application (web server, rxtest, txtest, etc..) */
	// Note, that this contains the listening loop (which is basically an infinite loop)
	start_application();

	/* never reached */
	cleanup_platform();

	return 0;
}
