package flow;

import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Main.Helper;

import java.util.*;

public class ConstantProp implements Flow.Analysis {

    private ConstantPropTable[] in, out;
    private ConstantPropTable entry, exit;
    private TransferFunction transferfn = new TransferFunction();

    public void preprocess(ControlFlowGraph cfg) {
        System.out.println("Method: " + cfg.getMethod().getName().toString());
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new ConstantPropTable[max];
        out = new ConstantPropTable[max];
        qit = new QuadIterator(cfg);

        ConstantPropTable.reset();

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            ConstantPropTable.register("R" + i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                ConstantPropTable.register(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                ConstantPropTable.register(use.getRegister().toString());
            }
        }

        entry = new ConstantPropTable();
        exit = new ConstantPropTable();
        transferfn.val = new ConstantPropTable();
        for (int i = 0; i < in.length; i++) {
            in[i] = new ConstantPropTable();
            out[i] = new ConstantPropTable();
        }

        for (int i = 0; i < numargs; i++) {
            entry.setNAC("R" + i);
        }
        System.out.println("Initialization completed.");
    }

    public void postprocess(ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i = 0; i < in.length; i++) {
            System.out.println(i + " in:  " + in[i].toString());
            System.out.println(i + " out: " + out[i].toString());
        }
        System.out.println("exit: " + exit.toString());
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() {
        return true;
    }

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    /* Routines for interacting with dataflow values. */

    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }

    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }

    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }

    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }

    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject newTempVar() {
        return new ConstantPropTable();
    }

    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        Helper.runPass(q, transferfn);
        out[q.getID()].copy(transferfn.val);
    }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    public static class SingleCP implements Flow.DataflowObject {
        private int state;
        private int constant;

        public SingleCP() {
            setUndef();
        }

        public void setToTop() {
            setUndef();
        }

        public void setToBottom() {
            setNAC();
        }

        public void meetWith(Flow.DataflowObject o) {
            SingleCP a = (SingleCP) o;
            if (a.state == 0)
                return;
            if (state == 0) {
                state = a.state;
                constant = a.constant;
                return;
            }
            if (state == 2 || a.state == 2) {
                setNAC();
                return;
            }
            /* otherwise, both are constants */
            if (constant == a.constant) {
                return;
            }
            setNAC();
        }

        public void copy(Flow.DataflowObject o) {
            SingleCP a = (SingleCP) o;
            state = a.state;
            constant = a.constant;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SingleCP) {
                SingleCP a = (SingleCP) o;
                if (state != 1) {
                    return a.state == state;
                } else {
                    return a.state == state && a.constant == constant;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return state;
        }

        @Override
        public String toString() {
            switch (state) {
                case 0:
                    return "undef";
                case 1:
                    return String.valueOf(constant);
                case 2:
                    return "NAC";
                default:
                    break;
            }
            return ("<invalid state " + state + ">");
        }

        public void setUndef() {
            state = 0;
        }

        public void setNAC() {
            state = 2;
        }

        public boolean isUndef() {
            return state == 0;
        }

        public boolean isConst() {
            return state == 1;
        }

        public boolean isNAC() {
            return state == 2;
        }

        public int getConst() {
            return constant;
        }

        public void setConst(int val) {
            state = 1;
            constant = val;
        }
    }

    public static class ConstantPropTable implements Flow.DataflowObject {
        /* 'core' is used to keep track of which variables we need to
         * track */
        private static Set<String> core = new HashSet<String>();
        private SortedMap<String, SingleCP> map;

        public ConstantPropTable() {
            map = new TreeMap<String, SingleCP>();
            for (String key : core) {
                map.put(key, new SingleCP());
            }
        }

        public static void reset() {
            core.clear();
        }

        public static void register(String key) {
            core.add(key);
        }

        public void setToTop() {
            for (SingleCP lattice : map.values()) {
                lattice.setToTop();
            }
        }

        public void setToBottom() {
            for (SingleCP lattice : map.values()) {
                lattice.setToBottom();
            }
        }

        public void meetWith(Flow.DataflowObject o) {
            ConstantPropTable a = (ConstantPropTable) o;
            for (Map.Entry<String, SingleCP> e : a.map.entrySet()) {
                SingleCP mine = map.get(e.getKey());
                mine.meetWith(e.getValue());
            }
        }

        public void copy(Flow.DataflowObject o) {
            ConstantPropTable a = (ConstantPropTable) o;
            for (Map.Entry<String, SingleCP> e : a.map.entrySet()) {
                SingleCP mine = map.get(e.getKey());
                mine.copy(e.getValue());
            }
        }

        @Override
        public String toString() {
            return map.toString();
        }

        public SingleCP get(String key) {
            return map.get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ConstantPropTable) {
                return map.equals(((ConstantPropTable) o).map);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        public void setUndef(String key) {
            get(key).setUndef();
        }

        public void setConst(String key, int val) {
            get(key).setConst(val);
        }

        public void setNAC(String key) {
            get(key).setNAC();
        }

        public void transfer(String key, String src) {
            get(key).copy(get(src));
        }
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        ConstantPropTable val;

        @Override
        public void visitMove(Quad q) {
            Operand op = Operator.Move.getSrc(q);
            String key = Operator.Move.getDest(q).getRegister().toString();

            if (isUndef(op)) {
                val.setUndef(key);
            } else if (isConst(op)) {
                val.setConst(key, getConst(op));
            } else {
                val.setNAC(key);
            }
        }

        @Override
        public void visitBinary(Quad q) {
            Operand op1 = Operator.Binary.getSrc1(q);
            Operand op2 = Operator.Binary.getSrc2(q);
            String key = Operator.Binary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

            if (opr == Operator.Binary.ADD_I.INSTANCE) {
                if (isNAC(op1) || isNAC(op2)) {
                    val.setNAC(key);
                } else if (isUndef(op1) || isUndef(op2)) {
                    val.setUndef(key);
                } else { // both must be constant!
                    val.setConst(key, getConst(op1) + getConst(op2));
                }
            } else {
                val.setNAC(key);
            }
        }

        @Override
        public void visitUnary(Quad q) {
            Operand op = Operator.Unary.getSrc(q);
            String key = Operator.Unary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

            if (opr == Operator.Unary.NEG_I.INSTANCE) {
                if (isUndef(op)) {
                    val.setUndef(key);
                } else if (isConst(op)) {
                    val.setConst(key, -getConst(op));
                } else {
                    val.setNAC(key);
                }
            } else {
                val.setNAC(key);
            }
        }

        @Override
        public void visitALoad(Quad q) {
            String key = Operator.ALoad.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitALength(Quad q) {
            String key = Operator.ALength.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitGetstatic(Quad q) {
            String key = Operator.Getstatic.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitGetfield(Quad q) {
            String key = Operator.Getfield.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitInstanceOf(Quad q) {
            String key = Operator.InstanceOf.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitNew(Quad q) {
            String key = Operator.New.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitNewArray(Quad q) {
            String key = Operator.NewArray.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitInvoke(Quad q) {
            RegisterOperand op = Operator.Invoke.getDest(q);
            if (op != null) {
                String key = op.getRegister().toString();
                val.setNAC(key);
            }
        }

        @Override
        public void visitJsr(Quad q) {
            String key = Operator.Jsr.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitCheckCast(Quad q) {
            String key = Operator.CheckCast.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        private boolean isUndef(Operand op) {
            return (op instanceof RegisterOperand &&
                    val.get(((RegisterOperand) op).getRegister().toString()).isUndef());
        }

        private boolean isConst(Operand op) {
            return (op instanceof IConstOperand) ||
                    (op instanceof RegisterOperand &&
                            val.get(((RegisterOperand) op).getRegister().toString()).isConst());
        }

        private boolean isNAC(Operand op) {
            return (op instanceof RegisterOperand &&
                    val.get(((RegisterOperand) op).getRegister().toString()).isNAC());
        }

        private int getConst(Operand op) {
            if (op instanceof IConstOperand) {
                return ((IConstOperand) op).getValue();
            }
            if (op instanceof RegisterOperand) {
                SingleCP o = val.get(((RegisterOperand) op).getRegister().toString());
                if (o.state == 1)
                    return o.getConst();
            }
            throw new IllegalArgumentException("Tried to getConst a non-Const!");
        }
    }
}
