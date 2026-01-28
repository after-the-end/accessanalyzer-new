package org.iam.common;

import com.microsoft.z3.*;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Z3Encoder implements EncodedAPI<BoolExpr> {

    private final Context ctx;
    private final Solver solver;

    public Z3Encoder() {
        this.ctx = new Context();
        this.solver = ctx.mkSolver();
    }

    @Override
    public BoolExpr mkTrue() {
        return ctx.mkTrue();
    }

    @Override
    public BoolExpr mkFalse() {
        return ctx.mkFalse();
    }

    @Override
    public BoolExpr mkReMatch(String key, String regex) {
        if (regex.equals("*")) {
            return mkTrue();
        }
        if (regex.equals("?")) {
            return ctx.mkNot(ctx.mkEq(ctx.mkConst(key, ctx.getStringSort()), ctx.mkString("")));
        }
        return ctx.mkInRe(ctx.mkConst(key, ctx.getStringSort()), mkRegex(regex));
    }

    @SuppressWarnings("unchecked")
    private ReExpr<SeqSort<CharSort>> mkRegex(String regex) {
         if (!regex.contains("?") && !regex.contains("*")) {
             return ctx.mkToRe(ctx.mkString(regex));
         }

         int lastOpIndex = -1;
         List<ReExpr<SeqSort<CharSort>>> regexExprList = new ArrayList<>();

         for (int i = 0; i < regex.length(); i++) {
             char c = regex.charAt(i);
             if (c == '?' || c == '*') {
                 if (i > lastOpIndex + 1) {
                     String prefix = regex.substring(lastOpIndex + 1, i);
                     ReExpr<SeqSort<CharSort>> prefixRe = ctx.mkToRe(ctx.mkString(prefix));
                     regexExprList.add(prefixRe);
                 }
                 ReExpr<SeqSort<CharSort>> opRe;
                 if (c == '?') {
                     opRe = ctx.mkAllcharRe(ctx.mkReSort(ctx.getStringSort()));
                 } else { // c == '*'
                     opRe = ctx.mkFullRe(ctx.mkReSort(ctx.getStringSort()));
                 }
                 regexExprList.add(opRe);
                 lastOpIndex = i;
             }
         }
         if (lastOpIndex < regex.length() - 1) {
             String suffix = regex.substring(lastOpIndex + 1);
             ReExpr<SeqSort<CharSort>> suffixRe = ctx.mkToRe(ctx.mkString(suffix));
             regexExprList.add(suffixRe);
         }
         return ctx.mkConcat(
                 (ReExpr<SeqSort<CharSort>>[]) regexExprList.toArray(ReExpr[]::new)
         );
    }

    @Override
    public BoolExpr and(List<BoolExpr> epxrs) {
        return ctx.mkAnd(epxrs.toArray(new BoolExpr[0]));
    }

    @Override
    public BoolExpr and(BoolExpr... exprs) {
        return ctx.mkAnd(exprs);
    }

    @SafeVarargs
    @Override
    public final BoolExpr and(GrammarlyAPI<BoolExpr>... exprs) {
        BoolExpr[] encodedExprs = Arrays.stream(exprs)
                .map(e -> e.encode(this))
                .toArray(BoolExpr[]::new);
        return ctx.mkAnd(encodedExprs);
    }

    @Override
    public BoolExpr or(List<BoolExpr> exprs) {
        return ctx.mkOr(exprs.toArray(new BoolExpr[0]));
    }

    @Override
    public BoolExpr or(BoolExpr... exprs) {
        return ctx.mkOr(exprs);
    }

    @SafeVarargs
    @Override
    public final BoolExpr or(GrammarlyAPI<BoolExpr>... exprs) {
        BoolExpr[] encodedExprs = Arrays.stream(exprs)
                .map(e -> e.encode(this))
                .toArray(BoolExpr[]::new);
        return ctx.mkOr(encodedExprs);
    }

    @Override
    public BoolExpr not(BoolExpr expr) {
        return ctx.mkNot(expr);
    }

    @Override
    public BoolExpr not(GrammarlyAPI<BoolExpr> expr) {
        return ctx.mkNot(expr.encode(this));
    }

    @Override
    public Boolean check(BoolExpr expr) {
        solver.reset();
        solver.add(expr);
        Status status = solver.check();
        assert (status != Status.UNKNOWN) : "Unable to solve the problem";
        return status == Status.SATISFIABLE;
    }

    @Override
    public Boolean checkIntersection(List<BoolExpr> exprs) {
        solver.reset();
        solver.add(exprs.toArray(new BoolExpr[0]));
        Status status = solver.check();
        assert (status != Status.UNKNOWN) : "Unable to solve the problem";
        return status == Status.SATISFIABLE;
    }

    @Override
    public Boolean greaterThan(BoolExpr lhs, BoolExpr rhs) {
        BoolExpr containsExpr = ctx.mkAnd(lhs, ctx.mkNot(rhs));
        BoolExpr coversExpr = ctx.mkAnd(ctx.mkNot(lhs), rhs);
        return check(containsExpr) && !check(coversExpr);
    }

    @Override
    public Boolean greaterThan(String lhs, String rhs) {
        if (lhs.equals("*")) {
            return true;
        }
        if (rhs.equals("*")) {
            return false;
        }
        if (lhs.equals(rhs)) {
            return false;
        }

        BoolExpr lhsExpr = mkReMatch("tmp", lhs);
        BoolExpr rhsExpr = mkReMatch("tmp", rhs);
        return greaterThan(lhsExpr, rhsExpr);
    }

    @Override
    public Boolean greaterEquals(BoolExpr lhs, BoolExpr rhs) {
        BoolExpr coversExpr = ctx.mkAnd(ctx.mkNot(lhs), rhs);
        return !check(coversExpr);
    }

    @Override
    public Boolean greaterEquals(String lhs, String rhs) {
        if (lhs.equals(rhs)) {
            return true;
        }
        if (rhs.equals("*")) {
            return true;
        }
        if (lhs.equals("*")) {
            return false;
        }

        BoolExpr lhsExpr = mkReMatch("tmp", lhs);
        BoolExpr rhsExpr = mkReMatch("tmp", rhs);
        return greaterEquals(lhsExpr, rhsExpr);
    }
}
