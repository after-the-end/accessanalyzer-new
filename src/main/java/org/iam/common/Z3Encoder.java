package org.iam.common;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;

import java.util.Arrays;
import java.util.List;

public class Z3Encoder implements EncodedAPI<BoolExpr> {

    private final Context ctx;
    private final Solver solver;

    public Z3Encoder() {
        this.ctx = new Context();
        this.solver = ctx.mkSolver();
    }

    public Context getContext() {
        return ctx;
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
    public Boolean greaterThan(BoolExpr lhs, BoolExpr rhs) {
        BoolExpr containsExpr = ctx.mkAnd(lhs, ctx.mkNot(rhs));
        BoolExpr coversExpr = ctx.mkAnd(ctx.mkNot(lhs), rhs);
        return check(containsExpr) && !check(coversExpr);
    }

    @Override
    public Boolean greaterEquals(BoolExpr lhs, BoolExpr rhs) {
        BoolExpr coversExpr = ctx.mkAnd(ctx.mkNot(lhs), rhs);
        return !check(coversExpr);
    }
}
