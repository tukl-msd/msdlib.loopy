package de.hopp.generator.utils;

import de.hopp.generator.model.*;

public class Model {

    public static MFile add(MFile file, MClass c) {
        return file.replaceClasses(file.classes().add(c));
    }
    public static MFile add(MFile file, MAttribute a) {
        return file.replaceAttributes(file.attributes().add(a));
    }
    public static MFile add(MFile file, MPreProcDir dir) {
        return file.replaceDirs(file.dirs().add(dir));
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
    public static MConstr addParam( MConstr c, MParameter p) {
        return c.replaceParameter(c.parameter().add(p));
    }
    public static MConstr addInit( MConstr c, MMemberInit i ) {
        return c.replaceInit(c.init().add(i));
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

    public static MInitList add(MInitList init, String param) {
        return init.replaceParams(init.params().add(param));
    }
    public static MInitList add(MInitList init, MInitList list) {
        return init.replaceParams(init.params().addAll(list.params()))
                   .replaceNeeded(init.needed().addAll(list.needed()));
    }
}
