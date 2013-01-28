/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "component.h"

/**
 * setup interface and ports
 */
void component::setup() {
	// setup interface
	intrfc->setup();

	// setup ports
	int j;
	for(j = 0; j < portCount; j++)
		ports[j]->setup();
}

component::~component() {
	delete intrfc;
	int j;
	for(j = 0; j < portCount; j++)
		delete ports[j];
}

int main(void){
	ethernet a("131.246.92.144", 8844);
	// ethernet a("192.168.1.10", 8844);
	interface *b = &a;
	b->test();
}
