/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "component.h"


component::component(interface *intrfc) : intrfc(intrfc) {
	intrfc->incRef();
}

component::~component() {
	intrfc->decRef();
}

