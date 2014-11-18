/*
 * @author Thomas Fischer
 * @since 25.06.2013
 */

#ifndef LOGGER_H_
#define LOGGER_H_

#include <iostream>
#include <fstream>

#include "constants.h"

/** severity enum */
enum severity { ERROR, WARN, INFO, FINE, FINER, FINEST };

/** This is a function pointer that takes a stream as input and returns the stream. */
typedef std::ostream& (*STRFUNC)(std::ostream&);

/**
 * The logger is used to log significant events occurring in the driver.
 * It is implemented as a stream - events should be printed using the shift operation.
 */
class logger : public std::ostream::basic_ostream {
private:
    std::ostream *stream_ptr;
    severity cur_sev, max_sev;
    std::string prefix;

    /**
     * Converts the current severity of the logger to a string.
     * @return A string representing the current severity of the logger.
     */
    std::string print_severity() {
        switch(cur_sev) {
        case ERROR:  return "ERROR  : ";
        case WARN:   return "WARNING: ";
        case INFO:   return "INFO   : ";
        case FINE:   return "FINE   : ";
        case FINER:  return "FINER  : ";
        case FINEST: return "FINEST : ";
        default:     return "UNKNOWN: ";
        }
    }

public:
    /**
     * Constructor of the logger.
     * @param stream_ptr Pointer to the stream the logger should write to.
     * @param max_severity The maximal severity of messages that should be logged.
     * @param prefix A string that is prefixed to each logged message.
     */
    logger(std::ostream *stream_ptr, severity max_severity, std::string prefix) : cur_sev(INFO) {
        this->stream_ptr = stream_ptr;
        this->max_sev    = max_severity;
        this->prefix     = prefix;
    }

    /**
     * Before each line, the debug severity should be streamed into the logger.
     * This results in the severity being logged as well and the current severity
     * of the logger to be changed to the provided level.
     * If the overall debug level of the driver is below the provided severity,
     * nothing will be forwarded to the wrapped stream.
     *
     * @param i The logger that should change the severity.
     * @param s The severity with which the following line(s) should be logged.
     * @return The modified logger (for chaining of stream operations).
     */
    friend logger& operator <<(logger &i, const severity s) {
        if(i.stream_ptr == NULL) return i;
        i.cur_sev = s;
        i << i.prefix << i.print_severity();
        return i;
    }

    friend logger& operator <<(logger &i, STRFUNC func) {
        if(i.stream_ptr == NULL) return i;
        if(i.cur_sev <= i.max_sev) func(*i.stream_ptr);
        return i;
    }
    // for some reason making this a friend as well breaks endl ):
    template<typename T>
    friend logger& operator <<(logger &i, const T t) {
        if(i.stream_ptr == NULL) return i;
        if(i.cur_sev <= i.max_sev) *i.stream_ptr << t;
        return i;
    }
};

/**
 * Logger instance.
 */
extern logger logger_host;
extern logger logger_board;

#endif /* LOGGER_H_ */


