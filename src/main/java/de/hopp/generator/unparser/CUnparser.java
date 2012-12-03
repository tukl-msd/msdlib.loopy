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

import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;

/**
 * plain C unparser. Generates C code out of the given model.
 * Generated code references header file, HUnparser has to be invoked as well.
 * Note, that some constructs cannot be unparsed and will lead to exceptions.
 * 
 * @author Thomas Fischer
 */
// TODO const modifier? (does it even make sense as modifier??)
public class CUnparser extends HUnparser {

//    // the buffer to fill with this unparsing
//    protected IndentStringBuffer buffer;
//    protected String typeIdent;
//    protected String name;
    /**
     * Create a MFile unparser
     * @param buffer the buffer to unparse into
     */
    public CUnparser(StringBuffer buffer, String name) {
//        this.cbuffer = new IndentStringBuffer(buffer);
//        this.typeIdent = new String();
//        this.name = name;
        super(buffer, name);
    }

    @Override
    public void visit(MFileInFile file) throws InvalidConstruct {

        // import the header file
        buffer.append("#include <" + name + " .h>\n");
        
        // unparse components
        visit(file.structs());
        visit(file.enums());
        visit(file.attributes());
        visit(file.methods());
        visit(file.classes());
    }

    @Override
    public void visit(MStructsInFile structs) throws InvalidConstruct {
        if(structs.size() > 0) {
            buffer.append("\n// structs of " + name + "\n");
            for(MStructInFile struct : structs) visit(struct);
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MEnumsInFile enums) throws InvalidConstruct {
        if(enums.size() > 0) {
            buffer.append("\n// enums of " + name);
            for(MEnumInFile mEnum : enums) {
                buffer.append("\n\n");
                visit(mEnum);
            }
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MAttributesInFile attributes) throws InvalidConstruct {
        if(attributes.size() > 0) {
            buffer.append("\n// fields of " + name);
            for(MAttributeInFile attribute : attributes) {
                buffer.append("\n\n");
                visit(attribute);
            }
            buffer.append('\n');
        }
    }

    @Override
    public void visit(MMethodsInFile methods) throws InvalidConstruct {
        if(methods.size() > 0) {
            buffer.append("\n// procedures of " + name);
            for(MMethodInFile attribute : methods) {
                buffer.append("\n\n");
                visit(attribute);
            }
            buffer.append('\n');
        }  
    }

    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        // In plain C no classes are allowed. So this has to be empty.
        if(!classes.isEmpty())
            throw new InvalidConstruct("encountered a class in C unparser");
    }
    
    // TODO this is how it is done by paddy... maybe apply this as well?
//    public void visit(MClassesInFile classes) {
//
//        // if no class at all return
//        if(classes.isEmpty()) return;
//
//        // we go through the classes using the rsib method
//        for(MClassInFile mclass = classes.first(); mclass != null; mclass = mclass.rsib()) {
//
//            // generate the class
//            visit(mclass);
//
//            // if it wasn't the last class we need a seperating line
//            if(mclass.rsib() != null) buffer.append("\n");
//        }
//    }

    @Override
    public void visit(MStructInFile struct) throws InvalidConstruct {
        buffer.append("struct " + struct.name().term() + " {\n");
        buffer.indent();
        
        // unparse components
        visit(struct.structs());
        visit(struct.enums());
        visit(struct.attributes());
        visit(struct.methods());
        
        buffer.unindent();
        buffer.append("}\n");
    }

    @Override
    public void visit(MEnumInFile mEnum) throws InvalidConstruct {
        buffer.append("enum " + mEnum.name().term() + " { ");
        if(mEnum.size() > 0) {
            for(StringInFile value : mEnum.values()) buffer.append(value.term());
            buffer.append(", ");
        }
        buffer.append(" };\n");
    }

    @Override
    public void visit(MAttributeInFile attribute) throws InvalidConstruct {
        buffer.append("\n\n");
        if(attribute.modifiers().term().contains(CONSTANT())) buffer.append(" const");
        typeIdent = attribute.name().term();
        visit(attribute.type());
        visit(attribute.initial());
        buffer.append(";\n");
    }

//    @Override
//    public void visit(MMethodInFile method) throws InvalidConstruct {
//        visit(method.returnType());
//        buffer.append(method.name().term());
//        visit(method.parameter());
//        visit(method.body());
//    }

    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {
        // should never go here
        throw new InvalidConstruct("encountered class in C unparser");
    }

    @Override
    public void visit(MModifiersInFile modifiers) throws InvalidConstruct {
        // should never go here
    }

    @Override
    public void visit(MCodeFragmentInFile codefragment) throws InvalidConstruct {
        if(! codefragment.part().term().equals(""))
            buffer.append(" = " + codefragment.part().term());
    }

    @Override
    public void visit(MParametersInFile parameters) throws InvalidConstruct {
        buffer.append(" (");
        for(MParameterInFile parameter : parameters) {
            visit(parameter);
            if(! parameter.equals(parameters.last())) buffer.append(',');
        }
        buffer.append(" ) ");
    }

    @Override
    public void visit(MCodeInFile code) throws InvalidConstruct {
        if(code.lines().size() > 0) {
            buffer.append(" {\n");
            buffer.indent();
            for(StringInFile line : code.lines()) buffer.append(line.term() + "\n");
            buffer.unindent();
            buffer.append("}");
        } else buffer.append("{ }");
    }

    @Override
    public void visit(MTypeInFile type) throws InvalidConstruct {
        buffer.append(type.name().term() + " " + typeIdent);
//        buffer.append(type.name() + " " + typeIdent);
        typeIdent = new String();
    }

    @Override
    public void visit(MArrayTypeInFile arrayType) throws InvalidConstruct {
        typeIdent = "(" + typeIdent + ") [" + arrayType.length().term() + "]";
        visit(arrayType.type());
//        buffer.append('(');
//        visit(arrayType.type());
//        buffer.append("[" + "])");
    }

    @Override
    public void visit(MPointerTypeInFile pointerType) throws InvalidConstruct {
//        typeIdent = "(*" + typeIdent + ")";
        typeIdent = "*" + typeIdent;
        visit(pointerType.type());
//        buffer.append("*");
    }

    @Override
    public void visit(MConstPointerTypeInFile constPointerType) throws InvalidConstruct {
//        typeIdent = "(*const" + typeIdent + ")";
        typeIdent = "*const" + typeIdent;
        visit(constPointerType.type());
//        buffer.append("*const");
    }

    @Override
    public void visit(MNoneInFile none) throws InvalidConstruct {
        // this is used to simulate constructors in C++
        // Consequently, it should never be used in C
        throw new InvalidConstruct("encountered constructor method in C unparser");        
    }

    @Override
    public void visit(MVoidInFile mvoid) throws InvalidConstruct {
        throw new InvalidConstruct("encountered void procedure in C unparser");
    }

    @Override
    public void visit(MTypesInFile types) throws InvalidConstruct {
        for(MTypeInFile type : types) {
            visit(type);
            if(! type.equals(types.last())) {
                buffer.append(',');
            }
        }
    }

    @Override
    public void visit(MParameterInFile parameter) throws InvalidConstruct {
        buffer.append(' ');
        typeIdent = parameter.name().term();
        if(parameter.refType().term() instanceof CONSTREF) buffer.append("const ");
        if(parameter.refType().term() instanceof CONSTREF || 
           parameter.refType().term() instanceof REFERENCE) typeIdent = "&" + typeIdent ;
        visit(parameter.type());
    }
}