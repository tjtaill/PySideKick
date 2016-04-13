package pysidekick;

import jep.Jep;

public class JepTest {
    public static void main(String[] args) throws Exception {
        Jep jep = new Jep();
        jep.eval("import sys");
        jep.eval("names = dir(sys)");
        Object ret = jep.getValue("names");
        System.out.println(ret);
    }
}
