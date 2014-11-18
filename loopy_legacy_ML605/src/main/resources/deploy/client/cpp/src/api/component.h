/**
 * Describes the generic vhdl component / ip core.
 * @file
 * @see components.h for user-defined componentsm and a list of specific core instances within this board driver.
 * @see gpio.h for gpio components.
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef COMPONENT_H_
#define COMPONENT_H_

//#include "port.h"

/**
 * Generic superclass for board components.
 * A board component is either a user-defined IPCore or one of the supported GPIO devices.
 * @see components.h for a list of specific core instances within this board driver.
 * @author Thomas Fischer
 * @since 09.01.2013
 */
class component {
//protected:
//public:
	/**
	 * Generic constructor for a board component.
	 */
//	component();
	/**
	 * Generic destructor for a board component.
	 */
//	~component();
};

#endif /* COMPONENT_H_ */
