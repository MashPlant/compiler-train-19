package examples;

import joeq.Compiler.Quad.*;
import joeq.Main.Helper;
import joeq.Class.*;

class CountQuads {

    public static class QuadCounter extends QuadVisitor.EmptyVisitor {
        public int count = 0;

        @Override
        public void visitQuad(Quad q) {
            count++;
        }
    }

    public static void main(String[] args) {
        PrimordialClassLoader.loader.addToClasspath("lib/rt.jar");
        
        jq_Class[] classes = new jq_Class[args.length];
        for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i]);

        for (int i=0; i < classes.length; i++) {
            System.out.println("Class: "+classes[i].getName());
            QuadCounter qc = new QuadCounter();
            Helper.runPass(classes[i], qc);
            System.out.println(classes[i].getName() + " has " +
                qc.count + " quads");
        }
    }
}
