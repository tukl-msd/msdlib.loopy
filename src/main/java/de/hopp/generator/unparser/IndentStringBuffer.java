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

package de.hopp.generator.unparser;

/**
 * Wraps a StringBuffer with the ability to automatically insert tabs after newlines,
 * according to the current tabsize
 */
class IndentStringBuffer {

    // the current tab count, the string used for creating the indentation and the max indent level so far
    private int count = 0;
    private String tabs = "";
    private int max = 0;
    private static final String tab = "    ";

    // the buffer which backs this buffer up
    private StringBuffer buffer;

    // if it's the start of a new line at the moment
    private boolean newLine = true;

    // initialize the tabString used for indentation
    IndentStringBuffer(StringBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Indent the following output by one tabsize more. Has to increase the blank string to support the new
     * tabsize if necessary.
     */
    public void indent() {

        // increase indent count
        count++;

        // as long as the indent count is greater than the max, increase the blank buffer
        while(count > max) {
            tabs += tab;
            max++;
        }
    }

    /**
     * Unindent the following output by one tabsize.
     */
    public void unindent() {

        // if greater than zero, decrease the tab count
        if(count > 0) count--;
    }

    /**
     * Append this text to the output. Indentation is done automatically
     * @param text the text to output
     */
    void append(String text) {

        // is it the first part of the text, i.e. without newline before?
        boolean first = true;

        // go through all parts of the text
        for(String part : text.split("\n", -1)) {

            // if it's not the first part we have a newline before
            if(first) first = false;
            else endLine();

            // add the text part
            addPart(part);
        }
    }

    /**
     * Append this character to the output.
     * @param text the character to output.
     */
    void append(char text) {

        // if new line symbol: end the line
        if(text == '\n') endLine();

        // otherwise add the text
        else addPart(Character.toString(text));
    }

    /**
     * End the current line and remember the next text part has to be indented
     */
    private void endLine() {

        // end the current line
        buffer.append('\n');

        // mark the beginning of a new line, to be indented if text follows
        newLine = true;
    }

    /**
     * Add a text part, if it's the first of the line than indent
     * @param part the text part to add
     */
    private void addPart(String part) {

        // if the part is empty we discard it; all other parts trigger the indentation and can't be undone or
        // changed in the current line
        if(part.length() == 0) return;

        // if it's the first text in the line, we indent the line
        if(newLine) {
            buffer.append(tabs.substring(0, tab.length() * count));
            newLine = false;
        }

        // add the text
        buffer.append(part);
    }
}
