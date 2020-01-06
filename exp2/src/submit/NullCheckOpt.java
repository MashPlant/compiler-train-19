package submit;

import joeq.Compiler.Quad.*;
import flow.Flow;
import flow.Flow.*;

import java.util.*;

public class NullCheckOpt implements Flow.Analysis {
  public static class VarSet implements DataflowObject {
    private HashSet<String> checked;

    VarSet(HashSet<String> checked) {
      this.checked = new HashSet<String>(checked);
    }

    VarSet() {
      this.checked = new HashSet<String>();
    }

    public void setToTop() {
      throw new UnsupportedOperationException();
    }

    public void setToBottom() {
      throw new UnsupportedOperationException();
    }

    public void meetWith(DataflowObject o) {
      checked.retainAll(((VarSet) o).checked);
    }

    public void copy(DataflowObject o) {
      checked = new HashSet<String>(((VarSet) o).checked);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof VarSet && checked.equals(((VarSet) o).checked);
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }
  }

  private TreeSet<Integer> redundant = new TreeSet<Integer>();
  private ArrayList[] nonnullByIf;
  private VarSet[] in, out;
  private VarSet entry, exit;
  private boolean doOpt, doExt;
  private boolean isStatic;

  NullCheckOpt(boolean doOpt, boolean doExt) {
    this.doOpt = doOpt;
    this.doExt = doExt;
  }

  public void preprocess(ControlFlowGraph cfg) {
    isStatic = cfg.getMethod().isStatic();
    redundant.clear();

    QuadIterator qit = new QuadIterator(cfg);
    int max = 0;
    while (qit.hasNext()) {
      int id = qit.next().getID();
      if (id > max)
        max = id;
    }
    max += 1;

    HashSet<String> universal = new HashSet<String>();
    int numargs = cfg.getMethod().getParamTypes().length;
    for (int i = 0; i < numargs; i++) {
      universal.add("R" + i);
    }

    nonnullByIf = new ArrayList[max];
    for (int i = 0; i < max; ++i) {
      nonnullByIf[i] = new ArrayList();
    }

    qit = new QuadIterator(cfg);
    while (qit.hasNext()) {
      Quad q = qit.next();
      for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
        universal.add(def.getRegister().toString());
      }
      for (Operand.RegisterOperand use : q.getUsedRegisters()) {
        universal.add(use.getRegister().toString());
      }
      Operator op = q.getOperator();
      if (op instanceof Operator.IntIfCmp.IFCMP_A) {
        List<Operand.RegisterOperand> use = q.getUsedRegisters();
        if ((Operand.Util.isNullConstant(Operator.IntIfCmp.getSrc1(q)) || Operand.Util.isNullConstant(Operator.IntIfCmp.getSrc2(q)))
            && !use.isEmpty()) {
          String reg = use.get(0).getRegister().toString();
          boolean eq = Operator.IntIfCmp.getCond(q).getCondition() == 0;
          BasicBlock fail = qit.getCurrentBasicBlock().getFallthroughSuccessor();
          BasicBlock success = Operator.IntIfCmp.getTarget(q).getTarget();
          BasicBlock nonnull = eq ? fail : success;
          nonnullByIf[nonnull.getQuad(0).getID()].add(reg);
        }
      }
    }

    in = new VarSet[max];
    out = new VarSet[max];

    qit = new QuadIterator(cfg);
    while (qit.hasNext()) {
      int id = qit.next().getID();
      in[id] = new VarSet();
      out[id] = new VarSet(universal);
    }

    entry = new VarSet();
    exit = new VarSet();
  }

  public void postprocess(ControlFlowGraph cfg) {
    if (doOpt) {
      QuadIterator iter = new QuadIterator(cfg);
      while (iter.hasNext()) {
        if (this.redundant.contains(iter.next().getID())) {
          iter.remove();
        }
      }
    } else {
      System.out.print(cfg.getMethod().getName());
      for (int id : redundant) {
        System.out.print(" " + id);
      }
      System.out.println();
    }
  }

  public boolean isForward() {
    return true;
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
    Operator op = q.getOperator();
    List<Operand.RegisterOperand> use = q.getUsedRegisters(), def = q.getDefinedRegisters();
    HashSet<String> tmp = new HashSet<String>(in[id].checked);
    for (Operand.RegisterOperand r : def) {
      tmp.remove(r.getRegister().toString());
    }
    if (doExt) {
      if (!isStatic) {
        tmp.add("R0"); // this
      }
      if (op instanceof Operator.New || op instanceof Operator.NewArray) {
        tmp.add(def.get(0).getRegister().toString());
      }
      if (op instanceof Operator.Move && !use.isEmpty()) {
        if (tmp.contains(use.get(0).getRegister().toString())) {
          tmp.add(def.get(0).getRegister().toString());
        }
      }
      tmp.addAll(nonnullByIf[id]);
    }
    if (op instanceof Operator.NullCheck) {
      String reg = use.get(0).getRegister().toString();
      if (tmp.contains(reg)) {
        redundant.add(id);
      } else {
        redundant.remove(id);
      }
      tmp.add(reg);
    }
    out[id] = new VarSet();
    out[id].checked = tmp;
  }
}