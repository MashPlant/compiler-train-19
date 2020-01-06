package examples;

import joeq.Compiler.Quad.*;
import joeq.Main.Helper;
import joeq.Class.*;
import java.util.Iterator;
class PrintQuads {

    public static void main(String[] args) {
        PrimordialClassLoader.loader.addToClasspath("lib/rt.jar");

        jq_Class[] classes = new jq_Class[args.length];
        for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i]);

        for (int i=0; i < classes.length; i++) {
            System.out.println("Class: "+classes[i].getName());
            Helper.runPass(classes[i], new PrintCFG());
        }
    }
}
