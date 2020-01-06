package submit;

import joeq.Compiler.Quad.*;
import flow.Flow;
import flow.Flow.DataflowObject;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import java.util.*;

public class LivenessOpt implements Flow.Analysis {
  public static class VarSet implements DataflowObject {
    private HashSet<String> vars = new HashSet<String>();

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
      vars = new HashSet<String>(((VarSet) o).vars);
    }

    @Override
    public String toString() {
      return vars.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof VarSet && vars.equals(((VarSet) o).vars);
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }
  }

  private VarSet[] in, out;
  private VarSet entry, exit;

  public void preprocess(ControlFlowGraph cfg) {
    QuadIterator qit = new QuadIterator(cfg);
    int max = 0;
    while (qit.hasNext()) {
      int id = qit.next().getID();
      if (id > max)
        max = id;
    }
    max += 1;

    in = new VarSet[max];
    out = new VarSet[max];

    qit = new QuadIterator(cfg);
    while (qit.hasNext()) {
      int id = qit.next().getID();
      in[id] = new VarSet();
      out[id] = new VarSet();
    }

    entry = new VarSet();
    exit = new VarSet();
  }

  public void postprocess(ControlFlowGraph cfg) {
    QuadIterator qit = new QuadIterator(cfg);
        out:
    while (qit.hasNext()) {
      Quad q = qit.next();
      q.getOperator();
      HashSet<String> out = this.out[q.getID()].vars;
      if (q.getOperator().hasSideEffects()) {
        continue;
      }
      for (RegisterOperand def : q.getDefinedRegisters()) {
        if (out.contains(def.getRegister().toString())) {
          continue out;
        }
      }
      qit.remove();
    }
  }

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
    HashSet<String> tmp = new HashSet<String>(out[id].vars);
    for (RegisterOperand def : q.getDefinedRegisters()) {
      tmp.remove(def.getRegister().toString());
    }
    for (RegisterOperand use : q.getUsedRegisters()) {
      tmp.add(use.getRegister().toString());
    }
    in[id] = new VarSet();
    in[id].vars = tmp;
  }
}
