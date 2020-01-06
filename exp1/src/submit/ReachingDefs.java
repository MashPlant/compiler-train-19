package submit;

// some useful things to import. add any additional imports you need.

import joeq.Compiler.Quad.*;
import flow.Flow;
import flow.Flow.DataflowObject;

import java.util.*;

/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class ReachingDefs implements Flow.Analysis {

  /**
   * Class for the dataflow objects in the ReachingDefs analysis.
   * You are free to change this class or move it to another file.
   */
  public static class DefSet implements DataflowObject {
    private TreeSet<Integer> defs = new TreeSet<Integer>(); // locations of def

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
      defs.addAll(((DefSet) o).defs);
    }

    public void copy(DataflowObject o) {
      defs = new TreeSet<Integer>(((DefSet) o).defs);
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
      return defs.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof DefSet && defs.equals(((DefSet) o).defs);
    }

    @Override
    public int hashCode() {
      return defs.hashCode();
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
  private DefSet[] in, out;
  private DefSet entry, exit;

  private HashMap<String, TreeSet<Integer>> defLocations = new HashMap<String, TreeSet<Integer>>();

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
    in = new DefSet[max];
    out = new DefSet[max];

    // initialize the contents of in and out.
    qit = new QuadIterator(cfg);
    while (qit.hasNext()) {
      int id = qit.next().getID();
      in[id] = new DefSet();
      out[id] = new DefSet();
    }

    // initialize the entry and exit points.
    entry = new DefSet();
    exit = new DefSet();

    for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
      Quad q = it.next();
      for (Operand.RegisterOperand r : q.getDefinedRegisters()) {
        String name = r.getRegister().toString();
        // I miss rust's `entry`
        if (!defLocations.containsKey(name)) {
          defLocations.put(name, new TreeSet<Integer>());
        }
        defLocations.get(name).add(q.getID());
      }
    }
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
    for (int i = 0; i < in.length; i++) {
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
    return true;
  }

  // I don't think protective copy is necessary
  // as long as the implementation in MySolver doesn't depend on this
  public DataflowObject getEntry() {
    return entry;
  }

  public DataflowObject getExit() {
    return exit;
  }

  public void setEntry(DataflowObject value) {
    entry = (DefSet) value;
  }

  public void setExit(DataflowObject value) {
    exit = (DefSet) value;
  }

  public DataflowObject getIn(Quad q) {
    return in[q.getID()];
  }

  public DataflowObject getOut(Quad q) {
    return out[q.getID()];
  }

  public void setIn(Quad q, DataflowObject value) {
    in[q.getID()] = (DefSet) value;
  }

  public void setOut(Quad q, DataflowObject value) {
    out[q.getID()] = (DefSet) value;
  }

  public DataflowObject newTempVar() {
    return new DefSet();
  }

  // should avoid aliasing here
  public void processQuad(Quad q) {
    int id = q.getID();
    TreeSet<Integer> tmp = new TreeSet<Integer>(in[id].defs);
    for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
      tmp.removeAll(defLocations.get(def.getRegister().toString()));
    }
    if (!q.getDefinedRegisters().isEmpty()) {
      tmp.add(q.getID());
    }
    out[id] = new DefSet();
    out[id].defs = tmp;
  }
}
