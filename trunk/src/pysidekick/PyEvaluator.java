package pysidekick;


import jep.Jep;
import jep.JepException;

import java.util.*;

public class PyEvaluator {

    static class JepEval {
        public final Jep jep;
        public final int indentation;

        JepEval(Jep jep, int indentation) {
            this.jep = jep;
            this.indentation = indentation;
        }
    }

    private static int identOf(CharSequence s) {
        int ident = 0;
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ')
                ident++;
            else
                break;
        }
        return ident;
    }

    public static String expressionVarType(Set<String> importStatements, String expression) {
        String type = "None";
        try ( Jep jep = new Jep(true, "C:\\Python34\\Scripts") ) {
            for (String importStatement : importStatements ) {
                jep.eval(importStatement);
            }
            jep.eval("temp_type = type(" + expression + ")");
            jep.eval("temp_module = temp_type.__module__ if temp_type else None");
            jep.eval("temp_name = temp_type.__name__ if temp_type else None");

            String module = (String) jep.getValue("temp_module");
            String name = (String) jep.getValue("temp_name");
            if ( "None".equals(name) ) {
                type = (String) jep.getValue("temp_type");
            } else if ("None".equals(module) ) {
                type = name;
            } else {
                if ( "builtins".equals(module) ) {
                    type = name;
                } else {
                    type = module + "." + name;
                }
            }
        } catch (JepException e) {
            e.printStackTrace();
        }
        return type;
    }

    public static JepEval addBlock(String pythonBlock) throws JepException {
        String[] pythonStatements = pythonBlock.split("\\r?\\n");
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        int ident = 0;
        for(String pythonStatement : pythonStatements ) {
            try {
                if ( pythonStatement.matches("\\s+") ) continue;
                jep.eval(pythonStatement);
                ident = identOf(pythonStatement);
            } catch (JepException e) {
                e.printStackTrace();
            }
        }
        return new JepEval(jep, ident);
    }

    public static List<String> typeNames(String namespace) {
        List<String> names = Collections.EMPTY_LIST;
        try ( Jep jep = new Jep(false, "C:\\Python34\\Scripts") ){
            int lastDotIndex = namespace.lastIndexOf('.');
            if ( lastDotIndex > -1 ) {
                String toImport = namespace.substring(0, lastDotIndex);
                jep.eval("import " + toImport);
            }
            jep.eval("temp_names = dir(" + namespace + ")");
            names = (List<String>) jep.getValue("temp_names");
        } catch (JepException e) {
            e.printStackTrace();
        }
        Collections.sort(names);
        return names;
    }

    public static List<String> importableModules() {
        List<String> modules = Collections.EMPTY_LIST;
        List<String> builtins = Collections.EMPTY_LIST;
        try (Jep jep = new Jep(false, "C:\\Python34\\Scripts") ) {
            jep.eval("from pkgutil import iter_modules");
            jep.eval("import sys");
            jep.eval("temp_modules = [m[1] for m in iter_modules()]");
            modules = (List<String>) jep.getValue("temp_modules");
            jep.eval("bm = sys.builtin_module_names");
            builtins = (List<String>) jep.getValue("bm");
            modules.addAll(builtins);
        } catch(JepException e) {
            e.printStackTrace();
        }
        Collections.sort(modules);
        return modules;
    }

    public static Set<String> builtinModules() throws JepException {
        Jep jep = new Jep(true, "C:\\Python34\\Scripts");
        jep.eval("import sys");
        jep.eval("modules = sys.builtin_module_names");
        Set<String> modules = new HashSet<String>((Collection<? extends String>) jep.getValue("modules"));
        jep.close();
        return modules;
    }

    public static List<String> moduleNames(String pythonModule) {
        List<String> names = Collections.EMPTY_LIST;
        try {
            Jep jep = new Jep(false, "C:\\Python34\\Scripts");
            jep.eval("import " + pythonModule);
            jep.eval("names = dir(" + pythonModule + ")");
            names = (List<String>) jep.getValue("names");
            jep.close();
        } catch (JepException e) {
            e.printStackTrace();
        }
        Collections.sort(names);
        return names;
    }

    private static void indent(StringBuilder builder, int indentation) {
        for(int i = 0; i < indentation; i++) {
            builder.append(' ');
        }
    }

    public static String moduleLevelVarType(JepEval jepEval, String variableName) throws JepException {
        StringBuilder ps = new StringBuilder();
        indent(ps, jepEval.indentation);
        ps.append("temp_type = type(" + variableName + ")");
        String type = "None";
        Jep jep = jepEval.jep;
        try {
            jep.eval(ps.toString());
            jep.eval(null);
            jep.eval("temp_module = temp_type.__module__ if temp_type else None");
            jep.eval("temp_name = temp_type.__name__ if temp_type else None");
            String module = (String) jep.getValue("temp_module");
            String name = (String) jep.getValue("temp_name");
            if ( "None".equals(name) ) {
                type = (String) jep.getValue("temp_type");
            } else if ("None".equals(module) ) {
                type = name;
            } else {
                if ( "builtins".equals(module) ) {
                    type = name;
                } else {
                    type = module + "." + name;
                }
            }
            jep.close();
        } catch (JepException e) {
            e.printStackTrace();
        }
        return type;
    }

}
