package org.iam.common;

import com.microsoft.z3.*;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    @Override
    public BoolExpr mkStringEq(String key, String value) {
        // Strict string equality
        return ctx.mkEq(ctx.mkConst(key, ctx.getStringSort()), ctx.mkString(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public BoolExpr mkStringEqIgnoreCase(String key, String value) {
        if (value.isEmpty()) {
            return mkStringEq(key, value);
        }

        List<ReExpr<SeqSort<CharSort>>> charRes = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String lower = String.valueOf(Character.toLowerCase(c));
            String upper = String.valueOf(Character.toUpperCase(c));

            ReExpr<SeqSort<CharSort>> r1 = ctx.mkToRe(ctx.mkString(lower));

            if (!lower.equals(upper)) {
                ReExpr<SeqSort<CharSort>> r2 = ctx.mkToRe(ctx.mkString(upper));
                charRes.add(ctx.mkUnion(r1, r2));
            } else {
                charRes.add(r1);
            }
        }

        ReExpr<SeqSort<CharSort>> fullRe;
        if (charRes.size() == 1) {
            fullRe = charRes.get(0);
        } else {
            fullRe = ctx.mkConcat(
                    (ReExpr<SeqSort<CharSort>>[]) charRes.toArray(new ReExpr[0])
            );
        }

        return ctx.mkInRe(ctx.mkConst(key, ctx.getStringSort()), fullRe);
    }

    // ... [mkIpMatch, ipToLong, mkRegex, and, or, not, check methods kept as is] ...
    @Override
    public BoolExpr mkIpMatch(String key, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String ipPart = parts[0];
            int prefixLength = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;

            long ipLong = ipToLong(ipPart);
            long maskLong = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            long networkLong = ipLong & maskLong;

            BitVecExpr ipVar = ctx.mkBVConst(key, 32);
            BitVecNum mask = ctx.mkBV(maskLong, 32);
            BitVecNum network = ctx.mkBV(networkLong, 32);

            return ctx.mkEq(ctx.mkBVAND(ipVar, mask), network);
        } catch (Exception e) {
            return mkFalse();
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
             throw new IllegalArgumentException("Invalid IP address: " + ip);
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(octets[i]);
        }
        return result;
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
    public BoolExpr and(List<BoolExpr> exprs) {
        List<BoolExpr> newExprs = exprs.stream()
                .filter(Objects::nonNull)
                .toList();
        return ctx.mkAnd(newExprs.toArray(new BoolExpr[0]));
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
    public Boolean greaterThan(VarKey key, String lhs, String rhs) {
        if (rhs.equals("*")) {
            return false;
        }
        if (lhs.equals("*")) {
            return true;
        }
        if (lhs.equals(rhs)) {
            return false;
        }

        switch(key) {
            case AWS_SOURCE_IP -> {
                return greaterThan(mkIpMatch("tmp", lhs), mkIpMatch("tmp", rhs));
            }
            default -> {
                return greaterThan(mkReMatch("tmp", lhs), mkReMatch("tmp", rhs));
            }
        }
    }

    @Override
    public Boolean greaterEquals(BoolExpr lhs, BoolExpr rhs) {
        BoolExpr coversExpr = ctx.mkAnd(ctx.mkNot(lhs), rhs);
        return !check(coversExpr);
    }

    @Override
    public Boolean greaterEquals(VarKey key, String lhs, String rhs) {
        if (lhs.equals(rhs)) {
            return true;
        }
        if (rhs.equals("*")) {
            return false;
        }
        if (lhs.equals("*")) {
            return true;
        }

        switch (key) {
            case AWS_SOURCE_IP -> {
                BoolExpr lhsExpr = mkIpMatch("tmp", lhs);
                BoolExpr rhsExpr = mkIpMatch("tmp", rhs);
                return greaterEquals(lhsExpr, rhsExpr);
            }
            default -> {
                BoolExpr lhsExpr = mkReMatch("tmp", lhs);
                BoolExpr rhsExpr = mkReMatch("tmp", rhs);
                return greaterEquals(lhsExpr, rhsExpr);
            }
        }
    }
}
