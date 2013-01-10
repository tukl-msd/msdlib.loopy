/**
 * @author Thomas Fischer
 * @since 09.01.2013
 */

#ifndef PORT_H_
#define PORT_H_

class por {
public:
	virtual ~por() = 0;

	virtual void setup() = 0;
};

class in : public por {
public:
	in() {}
	~in() {}

	bool writeData(int val);
};

class out : public por {
public:
	out() {}
	~out() {}

	bool readData(int val[]);
};

class dual : public in, public out {
public:
	dual() {}
	~dual() {}
};

#endif /* PORT_H_ */
