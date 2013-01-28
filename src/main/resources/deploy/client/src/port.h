/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

class port {
public:
	virtual ~port() {};

	virtual void setup() = 0;
};

class in : public port {
public:
	in() {}
	~in() {}

	bool writeData(int val);
	void setup();
};

class out : public port {
public:
	out() {}
	~out() {}

	bool readData(int val[]);
	void setup();
};

class dual : public in, public out {
public:
	dual() {}
	~dual() {}
	void setup();
};

#endif /* PORT_H_ */
