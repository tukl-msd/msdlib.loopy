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

package de.hopp.generator.model.unparser;

import static de.hopp.generator.model.cpp.CPP.CONSTANT;
import static de.hopp.generator.model.cpp.CPP.PRIVATE;
import static de.hopp.generator.model.cpp.CPP.STATIC;
import katja.common.NE;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.cpp.*;

/**
 * plain C unparser. Generates C code out of the given model.
 * Generated code references header file, HUnparser has to be invoked as well.
 * Note, that some constructs cannot be unparsed and will lead to exceptions.
 *
 * @author Thomas Fischer
 */
// TODO const modifier? (does it even make sense as modifier??)
public class CUnparser extends HUnparser {

    /**
     * Create a MFile unparser
     * @param buffer the buffer to unparse into
     * @param name file name where the buffer should be unparsed to
     */
    public CUnparser(StringBuffer buffer, String name) {
        super(buffer, name);
    }

    @Override
    public void visit(MFileInFile file) throws InvalidConstruct {

        // import the header file
        buffer.append("#include \"" + name + ".h\"\n");

        // gather all imports
        MIncludes importLines = gatherImports(file);

        importLines = filter(importLines, PRIVATE());

        if(!importLines.isEmpty()) {

            MInclude[] includes = importLines.toArray();
            sort(includes);

            for(final MInclude include : includes)
                buffer.append(include.Switch(new MInclude.Switch<String, NE>() {
                    public String CaseMBracketInclude(MBracketInclude term) {
                        return "\n#include <"  + term.name() + '>';
                    }
                    public String CaseMQuoteInclude(MQuoteInclude term) {
                        return "\n#include \""  + term.name() + '\"';
                    }
                    public String CaseMForwardDecl(MForwardDecl term) {
                        return "\n" + term.name() + ";";
                    }
                }));
        }

        // unparse components
        visit(file.dirs());
        visit(file.structs());
        visit(file.enums());
        visit(file.attributes());
        visit(file.procedures());
        visit(file.classes());
    }

    @Override
    public void visit(MPreProcDirsInFile dirs) throws InvalidConstruct {
        dirs= filter(dirs, PRIVATE());
        if(dirs.size() > 0) {
            // append comment
            buffer.append("\n// definitions of " + name + "");

            // unparse the contained structs
            for(MPreProcDirInFile dir : dirs) visit(dir);
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MAttributesInFile attributes) throws InvalidConstruct {
        if(attributes.size() > 0) {
            super.visit(filter(attributes, PRIVATE()));
            super.visit(attributes.removeAll(filter(attributes, PRIVATE()).term()));
        }
    }

    @Override
    public void visit(MMethodsInFile methods) throws InvalidConstruct {
        if(methods.parent() instanceof MClassInFile
                && methods.parent().modifiers().term().contains(PRIVATE())) {
            for(MMethodInFile method : filter(methods, PRIVATE()))
                super.visit(method);
            for(MMethodInFile method : methods.removeAll(filter(methods, PRIVATE()).term()))
                super.visit(method);
        } else {
            if(methods.size() > 0) {
                super.visit(filter(methods, PRIVATE()));
                super.visit(methods.removeAll(filter(methods, PRIVATE()).term()));
            }
        }
    }

    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        // In plain C no classes are allowed. So this has to be empty.
        if(!classes.isEmpty())
            throw new InvalidConstruct("encountered a class in C unparser");
    }

    @Override
    public void visit(MStructInFile struct) throws InvalidConstruct {
        visit(struct.doc());
        buffer.append("struct " + struct.name().term() + " {\n");
        buffer.indent();

        // unparse contained attributes
        visit(struct.attributes());

        buffer.unindent();
        buffer.append("}\n");
    }

    @Override
    public void visit(MEnumInFile mEnum) throws InvalidConstruct {
        visit(mEnum.doc());
        buffer.append("enum " + mEnum.name().term() + " { ");
        if(mEnum.size() > 0) {
            for(StringInFile value : mEnum.values()) buffer.append(value.term());
            buffer.append(", ");
        }
        buffer.append(" };\n");
    }

    @Override
    public void visit(MAttributeInFile attribute) throws InvalidConstruct {

        // start a new line for the attribute
        buffer.append('\n');

        // append documentation
        visit(attribute.doc());

        // set the type declaration to the attribute name
        typeDecl = attribute.name().term();

        // "private" globals should always be declare "static"
        if(attribute.parent().parent() instanceof MFileInFile)
            if(attribute.modifiers().term().contains(PRIVATE())) buffer.append("static ");
        // for attributes of classes, static has a different semantic and should not be set automatically
        else if(attribute.modifiers().term().contains(STATIC())) buffer.append("static ");

        // attributes can be constant
        if(attribute.modifiers().term().contains(CONSTANT())) buffer.append("const ");

        // unparse the type
        visit(attribute.type());

        // for class attributes, the initial value is set in the header
        visit(attribute.initial());

        // append colon
        buffer.append(";");
    }

    @Override
    public void visit(MProcedureInFile proc) throws InvalidConstruct {

        buffer.append('\n');

        // append documentation
        visit(proc.doc());

        // append keywords if appropriate
        if(proc.modifiers().term().contains(CONSTANT())) buffer.append("const ");
//        if(proc.parent().parent() instanceof MFileInFile
//                && proc.modifiers().term().contains(PRIVATE()))
//            // "private" functions should always be declared static
//            buffer.append("static ");
        // NO, they SHOULDN'T.
        //Private now only means that there is no header entry,
        //not that it can't be called from anywhere else (forward defs ftw...)
        else if(proc.modifiers().term().contains(STATIC())) buffer.append("static ");

        // unparse the return type
        visit(proc.returnType());

        // append the identifier of the method
        buffer.append(qualifiedName(proc));

        // unparse the parameter list
        visit(proc.parameter());

        // unparse the method body
        visit(proc.body());
    }


    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        // should never go here
        visit(mclass.doc());
        visit(mclass.structs());
        visit(mclass.enums());
        visit(mclass.attributes());
        visit(mclass.methods());
        visit(mclass.nested());
    }

//    @Override
//    public void visit(MCodeFragmentInFile codefragment) throws InvalidConstruct {
//        if(! codefragment.part().term().equals(""))
//            buffer.append(" = " + codefragment.part().term());
//    }

    @Override
    public void visit(MInitListInFile list) throws InvalidConstruct {
        throw new InvalidConstruct("encountered class initialisation list in C unparser");
    }

    @Override
    public void visit(MCodeInFile code) throws InvalidConstruct {
        if(code.lines().size() > 0) {
            buffer.append(" {\n");
            buffer.indent();
            for(StringInFile line : code.lines()) buffer.append(line.term() + "\n");
            buffer.unindent();
            buffer.append("}");
        } else buffer.append(" { }");
        buffer.append("\n");
    }

//    @Override
//    public void visit(MVoidInFile mvoid) throws InvalidConstruct {
//        throw new InvalidConstruct("encountered void procedure in C unparser");
//    }
}