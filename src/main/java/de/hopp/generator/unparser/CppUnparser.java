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

import static de.hopp.generator.model.Model.*;

import de.hopp.generator.model.*;

import katja.common.NE;

/**
 * C++ unparser. Generates C++ code out of the given model.
 * Everything specifiable using the model will be unparsed.
 * @author Thomas Fischer
 */
public class CppUnparser extends CUnparser {

    // the buffer to fill with this unparsing

    /**
     * Create a MFile unparser
     * @param buffer the buffer to unparse into
     */
    public CppUnparser(StringBuffer buffer, String name) {
        super(buffer, name);
    }
}

