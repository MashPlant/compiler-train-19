package submit;

// some useful things to import. add any additional imports you need.

import joeq.Compiler.Quad.*;
import flow.Flow;
import flow.Flow.DataflowObject;

import java.util.Collection;
import java.util.HashSet;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

  protected Flow.Analysis analysis;

  /**
   * Sets the analysis.  When visitCFG is called, it will
   * perform this analysis on a given CFG.
   *
   * @param analyzer The analysis to run
   */
  public void registerAnalysis(Flow.Analysis analyzer) {
    this.analysis = analyzer;
  }

  /**
   * Runs the solver over a given control flow graph.  Prior
   * to calling this, an analysis must be registered using
   * registerAnalysis
   *
   * @param cfg The control flow graph to analyze.
   */
  public void visitCFG(ControlFlowGraph cfg) {
    // this needs to come first.
    analysis.preprocess(cfg);

    Quad entryQuad = null;
    HashSet<Quad> exitQuads = new HashSet<Quad>();
    for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
      Quad q = it.next();
      if (entryQuad == null) {
        entryQuad = q;
      }
      if (it.successors1().contains(null)) {
        exitQuads.add(q);
      }
    }

    if (analysis.isForward()) {
      for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
        Quad q = it.next();
        analysis.setOut(q, analysis.newTempVar()); // this is top, according to the documentation of DataflowObject
      }

      boolean changed;
      DataflowObject entry = analysis.getEntry();
      do {
        changed = false;
        for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
          Quad q = it.next();
          DataflowObject newIn = meetOut(it.predecessors1(), entry);
          analysis.setIn(q, newIn);
          DataflowObject oldOut = analysis.getOut(q);
          analysis.processQuad(q);
          changed |= !oldOut.equals(analysis.getOut(q));
        }
      } while (changed);
      // the value of exit node doesn't affect the forward dataflow, so it doesn't have to be in the loop
      // it is okay to set it' value after the loop finishes
      DataflowObject newIn = meetOut(exitQuads, analysis.getEntry());
      analysis.setExit(newIn);
    } else {
      for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
        Quad q = it.next();
        analysis.setIn(q, analysis.newTempVar());
      }

      boolean changed;
      DataflowObject exit = analysis.getExit();
      do {
        changed = false;
        for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
          Quad q = it.next();
          DataflowObject newOut = meetIn(it.successors1(), exit);
          analysis.setOut(q, newOut);
          DataflowObject oldIn = analysis.getIn(q);
          analysis.processQuad(q);
          changed |= !oldIn.equals(analysis.getIn(q));
        }
      } while (changed);
      DataflowObject newOut = analysis.getIn(entryQuad);
      analysis.setEntry(newOut);
    }

    // this needs to come last.
    analysis.postprocess(cfg);
  }

  /*
  // `entry` is the fallback value when a predecessor is null
  private DataflowObject meetOut(Collection<Quad> pred, DataflowObject entry) {
    DataflowObject ret = analysis.newTempVar();
    for (Quad s : pred) {
      ret.meetWith(s == null ? entry : analysis.getOut(s));
    }
    return ret;
  }

  private DataflowObject meetIn(Collection<Quad> succ, DataflowObject exit) {
    DataflowObject ret = analysis.newTempVar();
    for (Quad s : succ) {
      ret.meetWith(s == null ? exit : analysis.getIn(s));
    }
    return ret;
  }
  */
    // `entry` is the fallback value when a predecessor is null
  private DataflowObject meetOut(Collection<Quad> pred, DataflowObject entry) {
    DataflowObject ret = analysis.newTempVar();
    boolean first = true;
    for (Quad s : pred) {
      DataflowObject rhs = s == null ? entry : analysis.getOut(s);
      if (first) {
        ret.copy(rhs);
        first = false;
      } else {
        ret.meetWith(rhs);
      }
    }
    return ret;
  }

  private DataflowObject meetIn(Collection<Quad> succ, DataflowObject exit) {
    DataflowObject ret = analysis.newTempVar();
    boolean first = true;
    for (Quad s : succ) {
      DataflowObject rhs = s == null ? exit : analysis.getIn(s);
      if (first) {
        ret.copy(rhs);
        first = false;
      } else {
        ret.meetWith(rhs);
      }
    }
    return ret;
  }
}
