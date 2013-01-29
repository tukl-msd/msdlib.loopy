/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef COMPONENT_H_
#define COMPONENT_H_

#include "interface.h"
#include "port.h"

/**
 * Generic superclass for board components.
 * A board component is either a user-defined IPCore or one of the supported GPIO devices.
 */
class component {
protected:
	/**
	 * The communication medium, this components board is attached to.
	 */
	interface *intrfc;
public:
	/**
	 * Constructor of the generic board component.
	 * This doesn't do anything and in fact should be virtual, but C++ requires a definition anyways...
	 * @param intrfc The communication medium, this components board is attached to.
	 */
	component(interface *intrfc);
	/**
	 * Destructor of the generic board component.
	 * Unregisters the component from the communication medium.
	 * Since the generic component has no ports, no further cleanup is required.
	 */
	~component();
};

#endif /* COMPONENT_H_ */
