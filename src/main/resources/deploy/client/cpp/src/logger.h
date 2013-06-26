/*
 * @author Thomas Fischer
 * @since 25.06.2013
 */

#ifndef LOGGER_H_
#define LOGGER_H_

#include <iostream>

#include "constants.h"

using namespace std;

/** severity enum */
enum severity { CRIT, ERROR, WARN, INFO, FINE, FINER, FINEST };

/** This is a function pointer that takes a stream as input and returns the stream. */
typedef std::ostream& (*STRFUNC)(std::ostream&);

/**
 * The logger is used to log significant events occurring in the driver.
 * It is implemented as a stream - events should be printed using the shift operation.
 */
class logger : public ostream::basic_ostream {
private:
    ostream *stream_ptr;
    severity sev;
    string prefix;

    /**
     * Converts the current severity of the logger to a string.
     * @return A string representing the current severity of the logger.
     */
    string print_severity() {
        switch(sev) {
        case CRIT:   return "CRITICAL: ";
        case ERROR:  return "ERROR   : ";
        case WARN:   return "WARNING : ";
        case INFO:   return "INFO    : ";
        case FINE:   return "FINE    : ";
        case FINER:  return "FINER   : ";
        case FINEST: return "FINEST  : ";
        default:     return "UNKNOWN : ";
        }
    }

public:
    logger(ostream *stream_ptr, string prefix) : sev(INFO) {
        this->stream_ptr = stream_ptr;
        this->prefix     = prefix;
    }

    /**
     * Before each line, the debug severity should be streamed into the logger.
     * This results in the severity being logged as well and the current severity
     * of the logger to be changed to the provided level.
     * If the overall debug level of the driver is below the provided severity,
     * nothing will be forwarded to the wrapped stream.
     */
    friend logger& operator <<(logger &i, const severity s) {
        if(i.stream_ptr == NULL) return i;
        i.sev = s;
        i << i.prefix << i.print_severity();
        return i;
    }
    friend logger& operator <<(logger &i, STRFUNC func) {
        if(i.stream_ptr == NULL) return i;
        if(i.sev <= current_severity) func(*i.stream_ptr);
        return i;
    }
    // for some reason making this a friend as well breaks endl ):
    template<typename T>
    friend logger& operator <<(logger &i, const T t) {
        if(i.stream_ptr == NULL) return i;
        if(i.sev <= current_severity) *i.stream_ptr << t;
        return i;
    }
};

/**
 * Logger instance.
 */
extern logger logger_host;
extern logger logger_board;

#endif /* LOGGER_H_ */

