/**
 * Describes generic components.
 * @file
 * @author: Thomas Fischer
 * @since: 28.01.2013
 */

#ifndef GPIO_H_
#define GPIO_H_

// pointers
#include <memory>

// locking
#include <atomic>
#include <mutex>
#include <condition_variable>

// other datatyps
#include "component.h"

// standard library
#include <unistd.h>
#include <stdio.h>
#include <math.h>

#include <bitset>

// other datatypes
#include "../constants.h"
#include "../logger.h"

/** Global writer lock. Use, whenever interacting with shared objects. */
extern std::mutex writer_mutex;
/** Notify this variable, when new values can be sent to the board. */
extern std::condition_variable can_write;

class abstract_gpo;
class abstract_gpi;

/** Pointers to the general purpose output components */
extern abstract_gpo *gpos[];
/** Pointers to the general purpose input components */
extern abstract_gpi *gpis[];

/**
 * Abstract representation of a generic general purpose
 * input or output component.
 *
 * This abstract component only contains a state,
 * represented as atomic integer.
 */
class gpio : public component {
protected:
	/** Current state of the gpio component. */
	std::atomic<int> state;
};

/**
 * Abstract general purpose input component.
 * Has no bitwidth and no corresponding methods but provides
 * several private attributes thereby simplifying the template.
 */
class abstract_gpi : public gpio {
friend void recv_gpio(unsigned char gid, unsigned char val);
protected:
    /** Identifier of the gpi component. */
    unsigned char gpi_id;

    /** mutex of the gpi component. */
    std::mutex gpi_mutex;
    /** condition variable, which is set if the state of the gpi component changes. */
    std::condition_variable has_changed;

    /**
     * Read the current state of a gpi component.
     * @return Integer value representing the current state of the component.
     */
    unsigned char readStateInternal() {
        return state;
    }

public:
    /**
     * Constructor for the abstract gpi component.
     * @param gpi_id Identifier used for this component.
     */
    abstract_gpi(unsigned char gpi_id) : gpi_id(gpi_id) {
        gpis[gpi_id] = this;
    };
    virtual ~abstract_gpi() {};
    /** Blocks, until the state of the gpi component changes. */
    void waitForChange() {
        // acquire the gpo lock
        std::unique_lock<std::mutex> lock (gpi_mutex);

        // wait for the state to change
        has_changed.wait(lock);
    }
};

/**
 * Abstract representation of a generic gpi component.
 * A gpi component is a component that is used as input device for the board,
 * but is not part of the host-side driver.
 * Consequently, it can only be read from the driver.
 */
template<int width>
class gpi : public abstract_gpi {
public:
    /**
     * Constructor for the generic gpi component.
     * @param gpi_id Identifier used for this component.
     */
    gpi(unsigned char gpi_id) : abstract_gpi(gpi_id) { }
    virtual ~gpi() {};

    /**
     * Reads the current state of a gpo component.
     * @return The state of the component represented by a bitset.
     */
    std::bitset<width> readState() {
        return std::bitset<width>(readStateInternal());
    }
};

/**
 * Abstract general purpose output component.
 * Has no bitwidth and no corresponding methods but provides
 * several private attributes thereby simplifying the template.
 */
class abstract_gpo : public gpio {
friend void scheduleWriter();
protected:
    /** Identifier of the gpo component. */
    unsigned char gpo_id;

    /**
     * Sets the gpo state using a single integer value.
     * @param state The new gpo state represented by a single integer value.
     *              The value has to be in the interval [0;255].
     */
    void writeStateInternal(int state) {
        // acquire writer lock
        std::unique_lock<std::mutex> lock(writer_mutex);

        // write the new state atomically (yay)
        this->state = state;
        // notify (doesn't matter, if it was written before... then we just notified twice. woohoo
        can_write.notify_one();
    }

public:
    /**
     * Constructor for the generic gpo component.
     * @param gpo_id Identifier used for this component.
     */
    abstract_gpo(unsigned char gpo_id) : gpo_id(gpo_id) {
        gpos[gpo_id] = this;
    }
    virtual ~abstract_gpo() {};
};

/**
 * Abstract representation of a generic gpo component.
 * A gpo component is a component that is used as output device for the board,
 * but is not part of the host-side driver.
 * Consequently, it can only be written to from the driver.
 */
template<int width>
class gpo : public abstract_gpo {
protected:
    /** Minimal value for the GPO test application. */
//    #define MIN_VALUE std::bitset<width>(3).to_ulong()
    /** Maximal value for the GPO test application. */
//    #define MAX_VALUE std::bitset<width>(3 << (width-2)).to_ulong()
#define MIN_VALUE 3
#define MAX_VALUE 192

    /**
     * Sets the next GPO state out of the current GPO state and a direction. The next
     * GPO state is considered to be the current state shifted one position into direction
     * if possible, otherwise shifted in the opposite direction.
     *
     * @param direction The current direction, into which the GPO state should be shifted.
     *        0 will shift to the right, 1 will shift to the left (I guess)
     * @param state Pointer to the current GPO state. The state will be changed by
     *        this procedure
     * @return the direction for the next step of GPO shifting.
     */
    bool next(bool direction, int &state) {
        if(direction) {
            if(state >= MAX_VALUE) return next(!direction, state);
            state = state * 2;
        } else {
            if(state <= MIN_VALUE) return next(!direction, state);
            state = state / 2;
        }
        return direction;
    }
public:
    /**
     * Constructor for the generic gpo component.
     * @param gpo_id Identifier used for this component.
     */
	gpo(unsigned char gpo_id) : abstract_gpo(gpo_id) { }
	virtual ~gpo() {};

    /**
     * Writes the state of a gpo component.
     * @param state The new state of the component represented by a bitset.
     */
	void writeState(std::bitset<width> state) {
	    writeStateInternal(state.to_ulong());
	}

	/**
     * Starts the test application for this gpo component.
     *
     * The test application will write several different values to the component.
     * For example, for a LED component, this should result into a moving pattern of enabled LEDs.
     */
    void test() {
        logger_host << INFO << "running loopy GPO test for GPO component " << gpo_id << std::endl;
        bool direction = false;
        int state = MIN_VALUE;

        int i = 0;
        while(i < 13) {
            writeStateInternal(state);
            direction = next(direction, state);
            i++;
            usleep(175000);
        }

        logger_host << INFO << "finished GPO test for GPO component " << gpo_id << std::endl;
    }
};

#endif /* GPIO_H_ */
