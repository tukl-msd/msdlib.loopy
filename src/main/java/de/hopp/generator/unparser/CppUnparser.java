package de.hopp.generator.unparser;

import static de.hopp.generator.model.Model.*;

import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;

import katja.common.NE;

/**
 * C++ unparser. Generates C++ code out of the given model.
 * Everything specifiable using the model will be unparsed.
 * @author Thomas Fischer
 */
public class CppUnparser extends CUnparser {

    /**
     * Create a MFile unparser
     * @param buffer the buffer to unparse into
     */
    public CppUnparser(StringBuffer buffer, String name) {
        super(buffer, name);
    }
    
//    @Override
//    protected String qualifiedName(MMethodInFile method) {
//        if(method.parent().parent() instanceof MFileInFile)
//            return method.name().term();
//        else if(method.parent().parent() instanceof MClassInFile)
//            return qualifiedName((MClassInFile)method.parent().parent()) + "::" + method.name().term();
//        throw new RuntimeException();
//    }
//    
//    @Override
//    protected String qualifiedName(MClassInFile mclass) {
//        if(mclass.parent().parent() instanceof MFileInFile)
//            return mclass.name().term();
//        else if(mclass.parent().parent() instanceof MClassInFile)
//            return qualifiedName((MClassInFile)mclass.parent().parent()) + "::" + mclass.name().term();
//        throw new RuntimeException();
//    }
    
    @Override
    public void visit(MClassesInFile classes) throws InvalidConstruct {
        if(classes.size() > 0) {
            for(MClassInFile mclass : classes) {
                visit(mclass);
            }
            buffer.append('\n');
        }
    }
    
    @Override
    public void visit(MClassInFile mclass) throws InvalidConstruct {

        visit(mclass.structs());
        visit(mclass.enums());
        visit(mclass.attributes());
        visit(mclass.methods());
        visit(mclass.nested());
        
    }
}
