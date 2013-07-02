/**
 * Describes utility methods reading a file in as a bitset.
 *
 * @file
 * @author Thomas Fischer
 * @since 29.05.2013
 */

#ifndef UTILS_H_
#define UTILS_H_

#include <stdlib.h>
#include <stdarg.h>
#include <stdio.h>

#include <vector>
#include <bitset>
#include <string>

#include <iostream>
#include <sstream>
#include <fstream>

#include <stdexcept>

#include <math.h>

using namespace std;

// basic issues:
// - size restricted ):
// - skips characters after numbers without errors...
template <class T>
static bool from_string(T& t, const std::string& s, std::ios_base& (*f)(std::ios_base&)) {
    std::istringstream iss(s);
    return !(iss >> f >> t).fail();
}

/**
 * Read in a file as a bitset vector.
 *
 * Note, that despite returning a bitset of values, internally a long long is used
 * for the conversion. Consequently, greater values cannot be read in.
 *
 * A value is parsed until an invalid character (i.e. neither a part of the number,
 * nor the separation character) is encountered. The remainder of such a value is ignored
 * until the next instance of the separation character.
 * If a value is empty (i.e. no characters) or starts with an invalid character,
 * it is ignored.
 *
 * @param file Path of the file to be read in.
 * @param delim Separation character between single values in the file.
 * @param f Function formatting values values to be read in (cf ios_base.h).
 * @tparam width Target bitwidth for the resulting bitset.
 * @return Transformed bitset in the target width.
 * @throws invalid_argument If the provided file does not exist.
 */
template <int width>
vector<bitset<width> > read_file(const char *file, const char delim, ios_base& (*f)(ios_base&)) {
    FILE *file_ptr;
    char *line_ptr = NULL;
    size_t len;
    vector<bitset<width> > values;

    file_ptr = fopen(file, "r");

    if(NULL == file_ptr) throw invalid_argument("file not found"); // throw an error

    while((getdelim(&line_ptr, &len, delim, file_ptr)) != -1) {
        string line = string(line_ptr);
        long long value;
        if(from_string<long long>(value, line_ptr, f)) // ignore bad lines
            values.push_back(bitset<width> (value));   // push back value to vector
    }

    fclose(file_ptr);
//    free(file_ptr); //??
    free(line_ptr);

    return values;
}

/**
 * Write a bitset vector to a file.
 *
 * Note, that despite writing a bitset of values, internally a long long is used
 * for the conversion. Consequently, greater values than cannot be written.
 *
 * @param file Path of the file to be written to.
 * @param delim Separation character between single values in the file.
 * @param f Function formatting values values to be written in (cf ios_base.h).
 * @param vals Source vector to be written.
 * @tparam width Bitwidth of the source bitset vector.
 */
template <int width>
void write_file(const char *file, const char delim, ios_base& (*f)(ios_base&), vector<bitset<width> > vals) {
    ofstream ofs(file, ofstream::out | ofstream::app);

    for(unsigned int i = 0; i < vals.size(); i++) {
        ofs << f << vals.at(i).to_ullong();  // add number in provided format
        if(i < vals.size()-1) ofs << delim;  // add delimiter
    }

    // append new line at the end
    ofs << "\n";

    ofs.close();
}

#endif /* UTILS_H_ */
