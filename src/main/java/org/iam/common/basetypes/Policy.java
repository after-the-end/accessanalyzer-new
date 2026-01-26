package org.iam.common.basetypes;

import com.fasterxml.jackson.annotation.*;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarEffect;
import org.iam.common.vars.VarKey;
import org.iam.utils.PolicyParser;

import java.util.*;

public class Policy<T> implements GrammarlyAPI<T> {

    @JsonProperty("Version")
    protected String version;

    @JsonProperty("Statement")
    protected Set<Statement<T>> statements;

    @JsonIgnore
    private Map<VarKey, Set<String>> kvMap = null;

    @JsonIgnore
    private T encodedExpr = null;

    public Policy(Policy<T> other) {
        this.version = other.version;
        this.statements = other.statements;
        this.kvMap = other.kvMap;
        this.encodedExpr = other.encodedExpr;
    }

    public Policy(String version, Set<Statement<T>> statement) {
        this.version = version;
        this.statements = statement;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<Statement<T>> getStatements() {
        return statements;
    }

    public void setStatements(Set<Statement<T>> statements) {
        this.statements = statements;
    }

    public Map<VarKey, Set<String>> getKvMap() {
        if (this.kvMap == null) {
            this.kvMap = buildKvMap();
        }
        return this.kvMap;
    }

    public Set<VarKey> keySet() {
        return this.getKvMap().keySet();
    }

    private Map<VarKey, Set<String>> buildKvMap() {
        Map<VarKey, Set<String>> newKvMap = new HashMap<>();
        for (Statement<T> statement : this.statements) {
            Map<VarKey, Set<String>> principals = statement.getPrincipal();
            for (VarKey key : principals.keySet()) {
                kvMap.computeIfAbsent(key, k -> new HashSet<>()).addAll(principals.get(key));
            }
            kvMap.computeIfAbsent(VarKey.ACTION, k -> new HashSet<>()).addAll(statement.getAction());
            kvMap.computeIfAbsent(VarKey.RESOURCE, k -> new HashSet<>()).addAll(statement.getResource());
            for (Condition<T> condition : statement.getCondition()) {
                kvMap.computeIfAbsent(condition.getKey(), k -> new HashSet<>()).addAll(condition.getValues());
            }
        }
        return kvMap;
    }

    @Override
    public T encode(EncodedAPI<T> helper) {
        if (this.statements == null || this.statements.isEmpty()) {
            throw new IllegalStateException("No valid statements in Policy, a parsing error may occurred.");
        }

        if (this.encodedExpr == null) {
            List<T> allowExprs = statements.stream()
                    .filter(s -> s.getEffect() == VarEffect.Allow)
                    .map(s -> s.encode(helper))
                    .toList();
            List<T> denyExprs = statements.stream()
                    .filter(s -> s.getEffect() == VarEffect.Deny)
                    .map(s -> s.encode(helper))
                    .toList();
            // allowed = allow1 OR allow2 OR ...
            // denied = deny1 OR deny2 OR ...
            // allowed AND NOT denied
            this.encodedExpr = helper.and(List.of(
                    helper.or(allowExprs),
                    helper.not(helper.or(denyExprs))
            ));
        }

        return this.encodedExpr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Policy<?> policy)) return false;
        return Objects.equals(version, policy.version) && Objects.equals(statements, policy.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, statements);
    }
}

