/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#include "component.h"


int main(void){
	ethernet a("131.246.92.144", 8844);
	// ethernet a("192.168.1.10", 8844);
	interface *b = &a;
	b->test();
}
