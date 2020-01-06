package flow;

import joeq.Main.Helper;
import joeq.Class.*;
import joeq.Compiler.Quad.*;

public abstract class Flow {

    public static interface DataflowObject {
        void setToTop();
        void setToBottom();
        void meetWith (DataflowObject o);
        void copy (DataflowObject o);

        /* Also, freshly constructed objects should be Top, equals
         * must be looser than object identity, and toString should
         * return things in a form that's repeatable across runs.  Use
         * SortedSets and SortedMaps instead of the normal kinds.
         */
    }

    public static interface Analysis {

        /* Analysis-specific customization.  You can use these to
         * precompute values or output results, if you wish. */

        void preprocess (ControlFlowGraph cfg);
        void postprocess (ControlFlowGraph cfg);

        /* Is this a forward dataflow analysis? */
        boolean isForward ();

        /* Routines for interacting with dataflow values.  You may
         * assume that the quad passed in is part of the relevant
         * CFG. */

        /**
         * Returns the entry value
         **/
        DataflowObject getEntry();
        /**
         * Returns the exit value
         **/
        DataflowObject getExit();
        /**
         * Sets the entry value
         **/
        void setEntry(DataflowObject value);
        /**
         * Sets the exit value
         **/
        void setExit(DataflowObject value);
        /**
         * Returns the IN value of a quad
         **/
        DataflowObject getIn(Quad q);
        /**
         * Returns the OUT value of a quad
         **/
        DataflowObject getOut(Quad q);
        /**
         * Sets the IN value of a quad
         **/
        void setIn(Quad q, DataflowObject value);
        /**
         * Sets the OUT value of a quad
         **/
        void setOut(Quad q, DataflowObject value);

        /**
         * Returns a new DataflowObject of the same type
         **/
        DataflowObject newTempVar();

        /**
         * Actually performs the transfer operation on the given
         * quad.
         **/
        void processQuad(Quad q);
    }

    public static interface Solver extends ControlFlowGraphVisitor {
        void visitCFG(ControlFlowGraph cfg);
        void registerAnalysis(Analysis a);
    }

    public static void main(String[] args) {
        PrimordialClassLoader.loader.addToClasspath("lib/rt.jar");

        String usage = "USAGE: Flow solver-class analysis-class [test-class]+";
        if (args.length < 3) {
            System.out.println(usage);
            return;
        }

        String solver_name = args[0];
        String analysis_name = args[1];

        // get an instance of the solver class.
        Solver solver;
        try {
            Object solver_obj = Class.forName(solver_name).newInstance();
            solver = (Solver) solver_obj;
        } catch (Exception ex) {
            System.out.println("ERROR: Could not load class '" + solver_name +
                "' as Solver: " + ex.toString());
            System.out.println(usage);
            return;
        }

        // get an instance of the analysis class.
        Analysis analysis;
        try {
            Object analysis_obj = Class.forName(analysis_name).newInstance();
            analysis = (Analysis) analysis_obj;
        } catch (Exception ex) {
            System.out.println("ERROR: Could not load class '" + analysis_name +
                "' as Analysis: " + ex.toString());
            System.out.println(usage);
            return;
        }

        // get the classes we will be visiting.
        jq_Class[] classes = new jq_Class[args.length - 2];
        for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i+2]);

        // register the analysis with the solver.
        solver.registerAnalysis(analysis);

        // visit each of the specified classes with the solver.
        for (int i=0; i < classes.length; i++) {
            System.out.println("Now analyzing " + classes[i].getName());
            Helper.runPass(classes[i], solver);
        }
    }
}
