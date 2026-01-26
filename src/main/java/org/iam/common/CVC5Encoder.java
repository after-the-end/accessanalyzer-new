package org.iam.common;

import io.github.cvc5.Kind;
import io.github.cvc5.Result;
import io.github.cvc5.Solver;
import io.github.cvc5.Term;
import io.github.cvc5.TermManager;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;

import java.util.Arrays;
import java.util.List;

public class CVC5Encoder implements EncodedAPI<Term> {

    private final TermManager tm;
    private final Solver solver;

    public CVC5Encoder() {
        this.tm = new TermManager();
        this.solver = new Solver(tm);
    }

    public Solver getSolver() {
        return solver;
    }

    public TermManager getTermManager() {
        return tm;
    }

    @Override
    public Term and(List<Term> exprs) {
        return tm.mkTerm(Kind.AND, exprs.toArray(new Term[0]));
    }

    @Override
    public Term and(Term... exprs) {
        return tm.mkTerm(Kind.AND, exprs);
    }

    @SafeVarargs
    @Override
    public final Term and(GrammarlyAPI<Term>... exprs) {
        Term[] encodedExprs = Arrays.stream(exprs)
                .map(e -> e.encode(this))
                .toArray(Term[]::new);
        return tm.mkTerm(Kind.AND, encodedExprs);
    }

    @Override
    public Term or(List<Term> exprs) {
        return tm.mkTerm(Kind.OR, exprs.toArray(new Term[0]));
    }

    @Override
    public Term or(Term... exprs) {
        return tm.mkTerm(Kind.OR, exprs);
    }

    @SafeVarargs
    @Override
    public final Term or(GrammarlyAPI<Term>... exprs) {
        Term[] encodedExprs = Arrays.stream(exprs)
                .map(e -> e.encode(this))
                .toArray(Term[]::new);
        return tm.mkTerm(Kind.OR, encodedExprs);
    }

    @Override
    public Term not(Term expr) {
        return tm.mkTerm(Kind.NOT, expr);
    }

    @Override
    public Term not(GrammarlyAPI<Term> expr) {
        return tm.mkTerm(Kind.NOT, expr.encode(this));
    }

    @Override
    public Boolean check(Term expr) {
        solver.resetAssertions();
        solver.assertFormula(expr);
        Result result = solver.checkSat();
        assert !result.isUnknown() : "Unable to solve the problem";
        return result.isSat();
    }

    @Override
    public Boolean greaterThan(Term lhs, Term rhs) {
        Term containsExpr = tm.mkTerm(Kind.AND, lhs, tm.mkTerm(Kind.NOT, rhs));
        Term coversExpr = tm.mkTerm(Kind.AND, tm.mkTerm(Kind.NOT, lhs), rhs);
        return check(containsExpr) && !check(coversExpr);
    }

    @Override
    public Boolean greaterEquals(Term lhs, Term rhs) {
        Term coversExpr = tm.mkTerm(Kind.AND, tm.mkTerm(Kind.NOT, lhs), rhs);
        return !check(coversExpr);
    }
}
