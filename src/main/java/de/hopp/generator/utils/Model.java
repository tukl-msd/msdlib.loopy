package de.hopp.generator.utils;

import de.hopp.generator.model.MAttribute;
import de.hopp.generator.model.MClass;
import de.hopp.generator.model.MCode;
import de.hopp.generator.model.MConstr;
import de.hopp.generator.model.MDefinition;
import de.hopp.generator.model.MDestr;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MMemberInit;
import de.hopp.generator.model.MMethod;
import de.hopp.generator.model.MProcedure;

public class Model {
    
    public static MFile add(MFile file, MClass c) {
        return file.replaceClasses(file.classes().add(c));
    }
    public static MFile add(MFile file, MAttribute a) {
        return file.replaceAttributes(file.attributes().add(a));
    }
    public static MFile add(MFile file, MDefinition d) {
        return file.replaceDefs(file.defs().add(d));
    }
    public static MFile add(MFile file, MProcedure p) {
        return file.replaceProcedures(file.procedures().add(p));
    }
    public static MClass add(MClass c, MAttribute a) {
        return c.replaceAttributes(c.attributes().add(a));
    }
    public static MClass add(MClass c, MMethod m) {
        return c.replaceMethods(c.methods().add(m));
    }
    public static MConstr addInit( MConstr c, MMemberInit i ) {
        return c.replaceInit(c.init().replaceVals(c.init().vals().add(i)));
    }
    public static MProcedure addLines(MProcedure m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    public static MConstr addLines(MConstr m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    public static MDestr addLines(MDestr m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    public static MFile addDoc(MFile file, String line) {
        return file.replaceDoc(file.doc().replaceDoc(file.doc().doc().add(line)));
    }
    
}
