/**
 * Defines all exceptions, that can be thrown by the driver.
 * Possible causes for exceptions are network errors
 * and usage errors (i.e. illegal values).
 * @file
 * @author Thomas Fischer
 * @since 22.03.2013
 */

#ifndef EXCEPTIONS_H_
#define EXCEPTIONS_H_

#include <exception>
#include <stdexcept>
#include <string>

/**
 * Exception for errors during protocol decoding.
 * These can be for example unknown protocol versions,
 * values that are out of the range reserved for certain
 * protocol fields, or unassigned IDs for this specific
 * driver instance.
 * Several, but not all, of this causes are internal, i.e.,
 * the user should not be able to provoke such an error
 * when only using the defined api.
 */
class protocolException : public std::runtime_error {
public:
	/**
	 * Constructor for protocol exceptions occurring in the driver.
	 * @param message Description of what went wrong.
	 */
	protocolException(std::string message) : runtime_error(message) { };
};

/**
 * Exception for errors during communication over the network.
 * This includes errors during setup or teardown of the connection
 * as well as errors during sending or receiving values, but not
 * reception of invalid messages.
 */
class mediumException : public std::runtime_error {
public:
	/**
	 * Constructor for medium exceptions occurring in the driver.
	 * @param message Description of what went wrong.
	 */
	mediumException(std::string message) : runtime_error(message) { };
};

#endif /* EXCEPTIONS_H_ */
