package org.iam.common;

import com.microsoft.z3.BoolExpr;
import io.github.cvc5.Kind;
import io.github.cvc5.Result;
import io.github.cvc5.Solver;
import io.github.cvc5.Term;
import io.github.cvc5.TermManager;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CVC5Encoder implements EncodedAPI<Term> {

    private final TermManager tm;
    private final Solver solver;
    private final Map<String, Term> variableCache;

    public CVC5Encoder() {
        this.tm = new TermManager();
        this.solver = new Solver(tm);
        this.variableCache = new HashMap<>();
    }

    @Override
    public Term mkTrue() {
        return tm.mkTrue();
    }

    @Override
    public Term mkFalse() {
        return tm.mkFalse();
    }

    @Override
    public Term mkReMatch(String key, String regex) {
        if (regex.equals("*")) {
            return mkTrue();
        }

        // CVC5 mkConst creates a fresh constant every time.
        // We use a cache to ensure the same key maps to the same Term object.
        Term keyConst = variableCache.computeIfAbsent(key, k -> tm.mkConst(tm.getStringSort(), k));

        if (regex.equals("?")) {
            Term emptyStr = tm.mkString("");
            return tm.mkTerm(Kind.NOT, tm.mkTerm(Kind.EQUAL, keyConst, emptyStr));
        }
        return tm.mkTerm(Kind.STRING_IN_REGEXP, keyConst, mkRegex(regex));
    }

    private Term mkRegex(String regex) {
        if (!regex.contains("?") && !regex.contains("*")) {
            return tm.mkTerm(Kind.STRING_TO_REGEXP, tm.mkString(regex));
        }

        int lastOpIndex = -1;
        List<Term> regexExprList = new ArrayList<>();

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '?' || c == '*') {
                if (i > lastOpIndex + 1) {
                    String prefix = regex.substring(lastOpIndex + 1, i);
                    regexExprList.add(tm.mkTerm(Kind.STRING_TO_REGEXP, tm.mkString(prefix)));
                }
                Term opRe;
                if (c == '?') {
                    opRe = tm.mkTerm(Kind.REGEXP_ALLCHAR);
                } else { // c == '*'
                    // .* equivalent
                    opRe = tm.mkTerm(Kind.REGEXP_STAR, tm.mkTerm(Kind.REGEXP_ALLCHAR));
                }
                regexExprList.add(opRe);
                lastOpIndex = i;
            }
        }
        if (lastOpIndex < regex.length() - 1) {
            String suffix = regex.substring(lastOpIndex + 1);
            regexExprList.add(tm.mkTerm(Kind.STRING_TO_REGEXP, tm.mkString(suffix)));
        }
        return tm.mkTerm(Kind.REGEXP_CONCAT, regexExprList.toArray(new Term[0]));
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
    public Boolean checkIntersection(List<Term> exprs) {
        Term intersectionExpr = tm.mkTerm(Kind.AND, exprs.toArray(new Term[0]));
        return check(intersectionExpr);
    }

    @Override
    public Boolean greaterThan(Term lhs, Term rhs) {
        Term containsExpr = tm.mkTerm(Kind.AND, lhs, tm.mkTerm(Kind.NOT, rhs));
        Term coversExpr = tm.mkTerm(Kind.AND, tm.mkTerm(Kind.NOT, lhs), rhs);
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

        Term lhsExpr = mkReMatch("tmp", lhs);
        Term rhsExpr = mkReMatch("tmp", rhs);
        return greaterThan(lhsExpr, rhsExpr);
    }

    @Override
    public Boolean greaterEquals(Term lhs, Term rhs) {
        Term coversExpr = tm.mkTerm(Kind.AND, tm.mkTerm(Kind.NOT, lhs), rhs);
        return !check(coversExpr);
    }

    @Override
    public Boolean greaterEquals(String lhs, String rhs) {
        if (lhs.equals("*")) {
            return true;
        }
        if (rhs.equals("*")) {
            return false;
        }
        if (lhs.equals(rhs)) {
            return true;
        }

        Term lhsExpr = mkReMatch("tmp", lhs);
        Term rhsExpr = mkReMatch("tmp", rhs);
        return greaterEquals(lhsExpr, rhsExpr);
    }
}
