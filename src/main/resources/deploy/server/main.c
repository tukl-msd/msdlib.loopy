/*
 * main.c
 *
 *  Created on: 01.02.2013
 *      Author: thomas
 */

//#include "medium/medium.h"
#include "platform.h"
#include "components/components.h"

void init_medium();
void start_application();

int main() {
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
