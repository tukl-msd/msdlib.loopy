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

/** typedef to support */
typedef std::ostream& (*STRFUNC)(std::ostream&);

/**
 * The logger is used to log significant events occurring in the driver.
 * It is implemented as a stream - events should be printed using the shift operation.
 */
class _logger : public ostream::basic_ostream {
private:
    ostream *stream_ptr;
    severity sev;

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
    _logger(ostream *stream_ptr) : sev(INFO) {
        this->stream_ptr = stream_ptr;
    }

    /**
     * Before each line, the debug severity should be streamed into the logger.
     * This results in the severity being logged as well and the current severity
     * of the logger to be changed to the provided level.
     * If the overall debug level of the driver is below the provided severity,
     * nothing will be forwarded to the wrapped stream.
     */
    friend _logger& operator <<(_logger &i, const severity s) {
        if(i.stream_ptr == NULL) return i;
        i.sev = s;
        i << i.print_severity();
        return i;
    }
    friend _logger& operator <<(_logger &i, STRFUNC func) {
        if(i.stream_ptr == NULL) return i;
        if(i.sev <= current_severity) func(*i.stream_ptr);
        return i;
    }
    template<typename T>
    _logger& operator <<(const T t) {
        if(stream_ptr == NULL) return *this;
        if(sev <= current_severity) *stream_ptr << t;
        return *this;
    }
};

/**
 * Logger instance.
 */
extern _logger logger;

#endif /* LOGGER_H_ */

