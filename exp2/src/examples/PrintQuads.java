package examples;

import joeq.Class.jq_Class;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Main.Helper;

public class PrintQuads {
    public static void main(String[] args) {
        jq_Class[] classes = new jq_Class[args.length];
        for (int i = 0; i < classes.length; i++)
            classes[i] = (jq_Class) Helper.load(args[i]);
        for (jq_Class clazz : classes)
            printClass(clazz);
    }

    public static void printClass(jq_Class clazz) {
        System.out.println("Class: " + clazz.getName());
        Helper.runPass(clazz, new PrintCFG());
    }
}
