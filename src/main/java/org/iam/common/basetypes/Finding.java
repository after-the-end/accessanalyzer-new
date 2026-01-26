package org.iam.common.basetypes;

import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarKey;

import java.util.*;
import java.util.stream.Collectors;

public class Finding<T> implements GrammarlyAPI<T> {

    private Map<VarKey, String> finding = null;
    private T encodedExpr = null;

    public Finding(Policy<T> policy) {
        this.finding = new HashMap<>();
        for (VarKey key : policy.keySet()) {
            // all the key of the root finding get a '.*' value
            this.finding.putIfAbsent(key, ".*");
        }
    }

    public Finding(Finding<T> other) {
        this.finding = other.finding;
        this.encodedExpr = other.encodedExpr;
    }

    public Finding(Map<VarKey, String> finding) {
        this.finding = finding;
        this.encodedExpr = null;
    }

    public Map<VarKey, String> getFinding() {
        return finding;
    }

    @Override
    public T encode(EncodedAPI<T> helper) {
        if (this.finding == null || this.finding.isEmpty()) {
            return helper.mkFalse();
        }

        if (this.encodedExpr == null) {
            List<T> exprs = this.finding.entrySet().stream()
                    .map(e -> helper.mkReMatch(helper.mkStringConst(e.getKey().toString()), e.getValue()))
                    .toList();
            this.encodedExpr = helper.and(exprs);
        }
        return this.encodedExpr;
    }

    @Override
    public String toString() {
        return "Finding{" +
                "finding=" + finding.entrySet().stream()
                .map(e -> e.getKey().toString() + "=" + e.getValue())
                .collect(Collectors.joining(", ")) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Finding<?> f)) return false;
        return Objects.equals(finding, f.finding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(finding);
    }
}
