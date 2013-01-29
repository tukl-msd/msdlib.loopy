/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "component.h"


component::component(interface *intrfc) : intrfc(intrfc) {
//	intrfc->setup();
}

component::~component() {
	// TODO do not delete, but unregister, decrementing ref counter!
	delete intrfc;
}

