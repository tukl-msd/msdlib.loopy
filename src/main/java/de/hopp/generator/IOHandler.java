/*
 * Katja
 * Copyright (C) 2003-2009 see README file for authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.hopp.generator;

/**
 * User: jmg
 * Date: 01.12.2004
 * Time: 09:05:07
 */
public class IOHandler {

    // the configuration, which holds possible IOHandler and the flags that are used
    public Configuration config;

    /**
     * Create a new IO object, using given configuration
     * @param config the configuration to use
     */
    public IOHandler(Configuration config) {
        this.config = config;
    }

    /**
     * Outputs a line, if not in quiet mode. Output goes either to a IOHandler or standard output.
     * @param line the line to print
     */
    public void println(String line) {

        // if in quiet mode, do nothing
        if(config.QUIET()) return;

        // print line
        System.out.println(line);
    }


    /**
     * Ends a line, if not in quiet mode. Output goes either to a IOHandler or standard output.
     */
    public void println() {

        // if in quiet mode, do nothing
        if(config.QUIET()) return;

        // print line
        System.out.println();
    }

    /**
     * Outputs a line part, if not in quiet mode. Output goes either to a IOHandler or standard output.
     * @param line the line part to print
     */
    public void print(String line) {

        // if in quiet mode, do nothing
        if(config.QUIET()) return;

        // print line
        System.out.print(line);
    }

    /**
     * Outputs a line only if debugging is enabled. Output goes either to a IOHandler or standard output.
     * @param line the line to print
     */
    public void debug(String line) {

        // if debug is not enabled, do nothing
        if(!config.DEBUG()) return;

        // print line
        System.out.println("DEBUG: " + line);
    }

    /**
     * Outputs an error, regardless of flags. Output goes either to a IOHandler or standard error output.
     * @param line the line to print
     */
    public void error(String line) {

        // print error
        System.out.println("ERROR: " + line);
    }
    
    /**
     * Outputs a warning  only if debugging is enabled. Output goes either to a IOHandler or standard output.
     * @param line the line to print
     */
    public void warning(String line) {

        // reuse of other method
        println("WARNING: " + line);
    }
    
    public void verbose(String line) {
        
        // if the config is not verbose, do nothing
        if(!config.VERBOSE()) return;
        
        // print line
        System.out.println(line);
    }

    /**
     * Prints the copyright information to IOHanlder or standard output
     */
    public void showCopyright() {
        println("------------------------------------------------------------------------------");
//        println("Katja " + Version.VERSION + ":" + Version.getSvnVersion() + " (" + BRANCH + ")");
//        println(" " + DATE);
        println("HOPP Driver Generator version " + Main.version);
        println("Copyright (C) 2012-2013 Software Technology Group");
        println("                        University of Kaiserslautern, Germany");
        println("");
        println("This program comes with ABSOLUTELY NO WARRANTY.");
        println("This is free software, and you are welcome to redistribute it");
        println("under certain conditions.");
        println("------------------------------------------------------------------------------");
    }

    /**
     * Outputs a line with EOL character
     * @param line the line to print
     */
    public void outln(String line) {

        // use standard output
        System.out.println(line);
    }

    /**
     * Outputs an EOL character
     */
    public void outln() {

        // use standard output
        System.out.println();
    }

    /**
     * Outputs a line part
     * @param line the line part to print
     */
    public void out(String line) {

        // use standard output
        System.out.print(line);
    }
}
