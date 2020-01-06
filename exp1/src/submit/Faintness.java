package submit;

// some useful things to import. add any additional imports you need.

import joeq.Compiler.Quad.*;
import flow.Flow;
import flow.Flow.DataflowObject;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import java.util.TreeSet;

/**
 * Skeleton class for implementing a faint variable analysis
 * using the Flow.Analysis interface.
 */
public class Faintness implements Flow.Analysis {

  /**
   * Class for the dataflow objects in the Faintness analysis.
   * You are free to change this class or move it to another file.
   */
  public static class VarSet implements DataflowObject {
    private TreeSet<String> vars = new TreeSet<String>(); // locations of def
    private static TreeSet<String> universalSet;
    /**
     * Methods from the DataflowObject interface.
     * See Flow.java for the meaning of these methods.
     * These need to be filled in.
     */
    public void setToTop() {
      throw new UnsupportedOperationException();
    }

    public void setToBottom() {
      throw new UnsupportedOperationException();
    }

    public void meetWith(DataflowObject o) {
      vars.addAll(((VarSet) o).vars);
    }

    public void copy(DataflowObject o) {
      vars = new TreeSet<String>(((VarSet) o).vars);
    }

    /**
     * toString() method for the dataflow objects which is used
     * by postprocess() below.  The format of this method must
     * be of the form "[ID0, ID1, ID2, ...]", where each ID is
     * the identifier of a quad defining some register, and the
     * list of IDs must be sorted.  See src/test/Test.rd.out
     * for example output of the analysis.  The output format of
     * your reaching definitions analysis must match this exactly.
     */
    @Override
    public String toString() {
      TreeSet<String> tmp = new TreeSet<String>(universalSet);
      tmp.removeAll(vars);
      return tmp.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof VarSet && vars.equals(((VarSet) o).vars);
    }

    @Override
    public int hashCode() {
      return vars.hashCode();
    }
  }

  /**
   * Dataflow objects for the interior and entry/exit points
   * of the CFG. in[ID] and out[ID] store the entry and exit
   * state for the input and output of the quad with identifier ID.
   * <p>
   * You are free to modify these fields, just make sure to
   * preserve the data printed by postprocess(), which relies on these.
   */
  private VarSet[] in, out;
  private VarSet entry, exit;

  /**
   * This method initializes the datflow framework.
   *
   * @param cfg The control flow graph we are going to process.
   */
  public void preprocess(ControlFlowGraph cfg) {
    // this line must come first.
    System.out.println("Method: " + cfg.getMethod().getName().toString());

    // get the amount of space we need to allocate for the in/out arrays.
    QuadIterator qit = new QuadIterator(cfg);
    int max = 0;
    while (qit.hasNext()) {
      int id = qit.next().getID();
      if (id > max)
        max = id;
    }
    max += 1;

    // allocate the in and out arrays.
    in = new VarSet[max];
    out = new VarSet[max];
    qit = new QuadIterator(cfg);

    TreeSet<String> s = new TreeSet<String>();
    VarSet.universalSet = s;

    /* Arguments are always there. */
    int numargs = cfg.getMethod().getParamTypes().length;
    for (int i = 0; i < numargs; i++) {
      s.add("R"+i);
    }

    while (qit.hasNext()) {
      Quad q = qit.next();
      for (RegisterOperand def : q.getDefinedRegisters()) {
        s.add(def.getRegister().toString());
      }
      for (RegisterOperand use : q.getUsedRegisters()) {
        s.add(use.getRegister().toString());
      }
    }

    // initialize the contents of in and out.
    qit = new QuadIterator(cfg);
    while (qit.hasNext()) {
      int id = qit.next().getID();
      in[id] = new VarSet();
      out[id] = new VarSet();
    }

    // initialize the entry and exit points.
    entry = new VarSet();
    exit = new VarSet();

    System.out.println("Initialization completed.");
  }

  /**
   * This method is called after the fixpoint is reached.
   * It must print out the dataflow objects associated with
   * the entry, exit, and all interior points of the CFG.
   * Unless you modify in, out, entry, or exit you shouldn't
   * need to change this method.
   *
   * @param cfg Unused.
   */
  public void postprocess(ControlFlowGraph cfg) {
    System.out.println("entry: " + entry.toString());
    for (int i = 1; i < in.length; i++) {
      if (in[i] != null) {
        System.out.println(i + " in:  " + in[i].toString());
        System.out.println(i + " out: " + out[i].toString());
      }
    }
    System.out.println("exit: " + exit.toString());
  }

  /**
   * Other methods from the Flow.Analysis interface.
   * See Flow.java for the meaning of these methods.
   * These need to be filled in.
   */
  public boolean isForward() {
    return false;
  }

  public DataflowObject getEntry() {
    return entry;
  }

  public DataflowObject getExit() {
    return exit;
  }

  public void setEntry(DataflowObject value) {
    entry = (VarSet) value;
  }

  public void setExit(DataflowObject value) {
    exit = (VarSet) value;
  }

  public DataflowObject getIn(Quad q) {
    return in[q.getID()];
  }

  public DataflowObject getOut(Quad q) {
    return out[q.getID()];
  }

  public void setIn(Quad q, DataflowObject value) {
    in[q.getID()] = (VarSet) value;
  }

  public void setOut(Quad q, DataflowObject value) {
    out[q.getID()] = (VarSet) value;
  }

  public DataflowObject newTempVar() {
    return new VarSet();
  }

  public void processQuad(Quad q) {
    int id = q.getID();
    TreeSet<String> tmp = new TreeSet<String>(out[id].vars);
    boolean notFaint = true;
    if (q.getOperator() instanceof Operator.Move) {
      String def = Operator.Move.getDest(q).getRegister().toString();
      notFaint = tmp.contains(def);
    } else if (q.getOperator() instanceof Operator.Binary) {
      String def = Operator.Binary.getDest(q).getRegister().toString();
      notFaint = tmp.contains(def);
    }
    for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
      tmp.remove(def.getRegister().toString());
    }
    if (notFaint) {
      for (Operand.RegisterOperand use : q.getUsedRegisters()) {
        tmp.add(use.getRegister().toString());
      }
    }
    in[id] = new VarSet();
    in[id].vars = tmp;
  }
}
