package org.iam.common.basetypes;

import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarKey;
import org.iam.core.KvRelations;

import java.util.*;
import java.util.stream.Collectors;

public class Finding<T> implements GrammarlyAPI<T> {

    private Map<VarKey, String> finding = null;
    private T encodedExpr = null;

    public Finding(Policy<T> policy) {
        this.finding = new HashMap<>();
        for (VarKey key : policy.keySet()) {
            this.finding.putIfAbsent(key,
                    switch (key) {
                        case AWS_SOURCE_IP -> "0.0.0.0/0";
                        default -> "*";
                    });
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

    public T reduce(KvRelations relations, EncodedAPI<T> helper) {
        List<T> keyIdomExprs = this.finding.entrySet().stream().map(
                entry -> {
                    VarKey key = entry.getKey();
                    String value = entry.getValue();
                    Set<String> idomValues = relations.idom(key, value);
                    T idomExpr;
                    if (idomValues == null || idomValues.isEmpty()) {
                        idomExpr = null;
                    } else {
                        List<T> idomExprs = idomValues.stream().map(
                                idomValue -> {
                                    if (idomValue == null) {
                                        return helper.mkFalse();
                                    }
                                    return switch (key) {
                                        case AWS_SOURCE_IP ->
                                                helper.mkIpMatch(key.toString(), idomValue);
                                        default ->
                                                helper.mkReMatch(key.toString(), idomValue);
                                    };
                                }).toList();
                        idomExpr = helper.or(idomExprs);
                    }
                    T reducedExpr = switch (key) {
                        case AWS_SOURCE_IP ->
                                helper.mkIpMatch(key.toString(), value);
                        default ->
                                helper.mkReMatch(key.toString(), value);
                    };
                    if (idomExpr != null) {
                        reducedExpr = helper.and(Arrays.asList(reducedExpr, helper.not(idomExpr)));
                    }
                    return reducedExpr;
                }).toList();
        return helper.and(keyIdomExprs);
    }

    public Set<Finding<T>> refine(KvRelations relations) {
        Set<Finding<T>> refinements = new HashSet<>();
        for (VarKey key : this.finding.keySet()) {
            Set<String> values = relations.idom(key, this.finding.get(key));
            for (String idomValue : values) {
                Map<VarKey, String> refinedFinding = new HashMap<>(this.finding);
                refinedFinding.put(key, idomValue);
                Finding<T> newFinding = new Finding<>(refinedFinding);
                refinements.add(newFinding);
            }
        }
        return refinements;
    }

    @Override
    public T encode(EncodedAPI<T> helper) {
        if (this.finding == null || this.finding.isEmpty()) {
            return helper.mkFalse();
        }

        if (this.encodedExpr == null) {
            List<T> exprs = this.finding.entrySet().stream()
                    .map(e -> helper.mkReMatch(e.getKey().toString(), e.getValue()))
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
