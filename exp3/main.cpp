
#include <cstdint>
#include <queue>
#include <string>
#include <unordered_map>

#include <llvm/IR/CFG.h>
#include <llvm/IR/InstVisitor.h>
#include <llvm/IRReader/IRReader.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/raw_ostream.h>
#include <z3++.h>

using u8 = unsigned char;
using u32 = unsigned;
using i8 = char;
using i32 = int;

using namespace llvm;
using namespace z3;

namespace {

// Get unique name of a LLVM node. Applicable to BasicBlock and Instruction.
std::string getName(const Value &v) {
  if (!v.getName().empty())
    return v.getName().str();

  std::string s;
  raw_string_ostream os(s);

  v.printAsOperand(os, false);
  return s;
}

// Check
void checkAndReport(z3::solver &solver, const GetElementPtrInst &gep) {
  std::string name = getName(gep);
  std::cout << "Checking with assertions:" << std::endl
            << solver.assertions() << std::endl;
  if (solver.check() == z3::sat)
    std::cout << "GEP " << name << " is potentially out of bound." << std::endl
              << "Model causing out of bound:" << std::endl
              << solver.get_model() << std::endl;
  else
    std::cout << "GEP " << name << " is safe." << std::endl;
}
} // namespace

struct FuncInfo {
  func_decl func;
  sort_vector params;
  expr_vector args;
  std::string name;
};

class Z3Walker {
  std::unordered_map<BasicBlock *, std::vector<std::pair<BasicBlock *, z3::expr>>> cond_map;
  z3::context ctx;
  z3::solver solver;
  std::unordered_map<Function *, FuncInfo> funcs; // note: destruct order

  z3::expr mk_dst(Instruction &inst, u32 size, FuncInfo &info) {
    func_decl f = ctx.function((info.name + '$' + inst.getName().str()).c_str(), info.params, ctx.bv_sort(size));
    return f(info.args);
  }

  z3::expr mk_operand(Value *v, u32 size, FuncInfo &info) {
    if (ConstantInt *ci = dyn_cast<ConstantInt>(v)) {
      return ctx.bv_val(ci->getSExtValue(), size);
    } else {
      func_decl f = ctx.function((info.name + '$' + v->getName().str()).c_str(), info.params, ctx.bv_sort(size));
      return f(info.args);
    }
  }

  Z3Walker() : ctx(), solver(ctx) {}

  void visitModule(Module &m) {
    for (Function &f : m) {
      std::string name = f.getName().str();
      FunctionType *ty = dyn_cast<FunctionType>(f.getType()->getPointerElementType());
      sort_vector params(ctx);
      expr_vector args(ctx);
      for (Type *p : ty->params()) {
        if (IntegerType *it = dyn_cast<IntegerType>(p)) {
          params.push_back(ctx.bv_sort(it->getBitWidth()));
        } else {
          std::cerr << "only support integers as function parameter type" << std::endl;
          return;
        }
      }
      if (IntegerType *it = dyn_cast<IntegerType>(ty->getReturnType())) {
        func_decl func = ctx.function(name.c_str(), params, ctx.bv_sort(it->getBitWidth()));
        for (Argument &arg : f.args()) {
          args.push_back(ctx.bv_const((name + '$' + arg.getName().str()).c_str(), arg.getType()->getIntegerBitWidth()));
        }
        funcs.insert({&f, FuncInfo{func, params, args, name}});
      } else {
        std::cerr << "only support integers as function return type" << std::endl;
        return;
      }
    }
    std::queue<BasicBlock *> q;
    std::unordered_map<BasicBlock *, u32> in_degs;
    for (Function &f : m) {
      FuncInfo &info = funcs.find(&f)->second;
      in_degs.clear();
      cond_map[&f.getEntryBlock()] = {{nullptr, ctx.bool_val(true)}};
      for (BasicBlock &b : f) {
        in_degs[&b] = pred_size(&b);
      }
      q.push(&f.getEntryBlock());
      while (!q.empty()) {
        BasicBlock *b = q.front();
        q.pop();
        pass1(*b, info);
        for (BasicBlock *succ : successors(b)) {
          if (--in_degs[succ] == 0) {
            q.push(succ);
          }
        }
      }
    }
    for (Function &f : m) {
      FuncInfo &info = funcs.find(&f)->second;
      for (BasicBlock &b : f) {
        pass2(b, info);
      }
    }
  }

  void pass1(BasicBlock &b, FuncInfo &info) {
    z3::expr enter_cond = ctx.bool_val(false);
    for (std::pair<BasicBlock *, z3::expr> &p : cond_map[&b]) {
      enter_cond = enter_cond || p.second;
    }
    for (Instruction &inst : b) {
      if (BinaryOperator *bin = dyn_cast<BinaryOperator>(&inst)) {
        u32 size = inst.getType()->getIntegerBitWidth();
        z3::expr dst = mk_dst(inst, size, info);
        z3::expr l = mk_operand(bin->getOperand(0), size, info), r = mk_operand(bin->getOperand(1), size, info);
        switch (bin->getOpcode()) {
        case Instruction::Add:
          solver.add(forall(info.args, l + r == dst));
          if (bin->hasNoSignedWrap()) {
            solver.add(forall(info.args, implies(r <= 0, dst <= l)));
            solver.add(forall(info.args, implies(r > 0, dst > l)));
          }
          break;
        case Instruction::Sub:
          solver.add(forall(info.args, l - r == dst));
          if (bin->hasNoSignedWrap()) {
            solver.add(forall(info.args, implies(r <= 0, dst >= l)));
            solver.add(forall(info.args, implies(r > 0, dst < l)));
          }
          break;
        case Instruction::Mul:
          solver.add(forall(info.args, l / r == dst));
          break;
        case Instruction::Shl:
          solver.add(forall(info.args, shl(l, r) == dst));
          break;
        case Instruction::LShr:
          solver.add(forall(info.args, lshr(l, r) == dst));
          break;
        case Instruction::AShr:
          solver.add(forall(info.args, ashr(l, r) == dst));
          break;
        case Instruction::And:
          solver.add(forall(info.args, (l & r) == dst));
          break;
        case Instruction::Or:
          solver.add(forall(info.args, (l | r) == dst));
          break;
        case Instruction::Xor:
          solver.add(forall(info.args, (l ^ r) == dst));
          break;
        default:
          break;
        }
      } else if (ICmpInst *icmp = dyn_cast<ICmpInst>(&inst)) {
        z3::expr dst = mk_dst(inst, 1, info);
        u32 size = icmp->getOperand(0)->getType()->getIntegerBitWidth();
        z3::expr l = mk_operand(icmp->getOperand(0), size, info), r = mk_operand(icmp->getOperand(1), size, info);
        ICmpInst::Predicate op = icmp->getPredicate();
        switch (op) {
        case ICmpInst::ICMP_EQ:
          solver.add(forall(info.args, (l == r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_NE:
          solver.add(forall(info.args, (l != r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_UGT:
          solver.add(forall(info.args, ugt(l, r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_UGE:
          solver.add(forall(info.args, uge(l, r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_ULT:
          solver.add(forall(info.args, ult(l, r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_ULE:
          solver.add(forall(info.args, ule(l, r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_SGT:
          solver.add(forall(info.args, (l > r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_SGE:
          solver.add(forall(info.args, (l >= r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_SLT:
          solver.add(forall(info.args, (l < r) == (dst != 0)));
          break;
        case ICmpInst::ICMP_SLE:
          solver.add(forall(info.args, (l <= r) == (dst != 0)));
          break;
        default:
          break;
        }
      } else if (BranchInst *br = dyn_cast<BranchInst>(&inst)) {
        if (br->isUnconditional()) {
          cond_map[br->getSuccessor(0)].push_back({&b, enter_cond});
        } else {
          z3::expr cond = mk_operand(br->getCondition(), 1, info);
          cond_map[br->getSuccessor(0)].push_back({&b, enter_cond && cond != 0}); // if
          cond_map[br->getSuccessor(1)].push_back({&b, enter_cond && cond == 0}); // else
        }
      } else if (PHINode *phi = dyn_cast<PHINode>(&inst)) {
        std::vector<std::pair<BasicBlock *, z3::expr>> enter_conds = cond_map[&b];
        u32 size = inst.getType()->getIntegerBitWidth();
        z3::expr dst = mk_dst(inst, size, info);
        u32 idx = 0;
        for (Value *v : phi->incoming_values()) {
          BasicBlock *prev = phi->getIncomingBlock(idx++);
          for (std::pair<BasicBlock *, z3::expr> &p : enter_conds) {
            if (p.first == prev) {
              solver.add(forall(info.args, implies(p.second, dst == mk_operand(v, size, info))));
              break;
            }
          }
        }
      } else if (isa<ZExtInst>(&inst) || isa<SExtInst>(&inst)) {
        u32 dst_size = inst.getType()->getIntegerBitWidth();
        Value *src = inst.getOperand(0);
        u32 src_size = src->getType()->getIntegerBitWidth();
        z3::expr dst = mk_dst(inst, dst_size, info);
        solver.add(forall(info.args, dst == (isa<ZExtInst>(&inst) ? zext : sext)(mk_operand(src, src_size, info), dst_size - src_size)));
      } else if (CallInst *call = dyn_cast<CallInst>(&inst)) {
        z3::expr dst = mk_dst(inst, inst.getType()->getIntegerBitWidth(), info);
        z3::func_decl &callee = funcs.find(call->getCalledFunction())->second.func;
        expr_vector args(ctx);
        for (Value *arg : call->args()) {
          args.push_back(mk_operand(arg, arg->getType()->getIntegerBitWidth(), info));
        }
        solver.add(forall(info.args, dst == callee(args)));
      } else if (ReturnInst *ret = dyn_cast<ReturnInst>(&inst)) {
        Value *v = ret->getReturnValue();
        u32 size = v->getType()->getIntegerBitWidth();
        solver.add(forall(info.args, implies(enter_cond, mk_operand(v, size, info) == info.func(info.args))));
      }
    }
  }

  void pass2(BasicBlock &b, FuncInfo &info) {
    z3::expr enter_cond = ctx.bool_val(false);
    for (std::pair<BasicBlock *, z3::expr> &p : cond_map[&b]) {
      enter_cond = enter_cond || p.second;
    }
    for (Instruction &inst : b) {
      if (GetElementPtrInst *gep = dyn_cast<GetElementPtrInst>(&inst)) {
        if (gep->isInBounds()) {
          if (ArrayType *arr_ty = dyn_cast<ArrayType>(gep->getSourceElementType())) {
            u32 len = arr_ty->getNumElements();
            Value *idx = gep->getOperand(2);
            u32 idx_size = idx->getType()->getIntegerBitWidth();
            solver.push();
            z3::expr idx1 = sext(mk_operand(idx, idx_size, info), 64 - idx_size);
            solver.add(exists(info.args, !(idx1 >= ctx.bv_val(0, 64) && idx1 < ctx.bv_val(len, 64))));
            solver.add(forall(info.args, enter_cond));
            checkAndReport(solver, *gep);
            solver.pop();
          }
        }
      }
    }
  }

public:
  static void work(Module &m) {
    Z3Walker().visitModule(m);
  }
};

i32 main(i32 argc, i8 **argv) {
  if (argc < 2) {
    errs() << "Usage: " << argv[0] << " <IR file>\n";
    return 1;
  }

  LLVMContext ctx;

  // Parse the input LLVM IR file into a module.
  SMDiagnostic err;
  if (std::unique_ptr<Module> module = parseIRFile(argv[1], err, ctx)) {
    Z3Walker::work(*module);
  } else {
    err.print(argv[0], errs());
    return 1;
  }

  return 0;
}