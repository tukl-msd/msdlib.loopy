/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef COMPONENT_H_
#define COMPONENT_H_

#include "interface.h"
#include "port.h"

class component {
private:
	// connection interface
	interface *intrfc;

	// port pointer array
	int portCount;
	port *ports[];

public:
	void setup();
	~component();
};

#endif /* COMPONENT_H_ */
