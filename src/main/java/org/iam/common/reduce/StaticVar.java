package org.iam.common.reduce;

import org.iam.common.apis.EncodedAPI;
import org.iam.common.basetypes.Finding;
import org.iam.common.basetypes.Policy;

public class StaticVar<T> {
    private final Finding<T> finding;
    private final Policy<T> policy;
    private final T forcedExpr; // Used for Logic True
    private final VarType varType;
    private DynamicVar<T> cachedDynamicVar;

    private StaticVar(Builder<T> builder) {
        this.finding = builder.finding;
        this.policy = builder.policy;
        this.forcedExpr = builder.forcedExpr;
        this.varType = builder.varType;
    }

    public static class Builder<T> {
        private Finding<T> finding;
        private Policy<T> policy;
        private T forcedExpr;
        private VarType varType;

        public Builder<T> setPolicy(Policy<T> policy) {
            this.policy = policy;
            this.varType = VarType.POLICY;
            return this;
        }

        public Builder<T> setFinding(Finding<T> finding) {
            this.finding = finding;
            this.varType = VarType.FINDING;
            return this;
        }

        public Builder<T> setExpr(T expr) {
            this.forcedExpr = expr;
            this.varType = VarType.EXPR;
            return this;
        }

        public StaticVar<T> build() {
            return new StaticVar<>(this);
        }
    }

    public enum VarType {
        FINDING,
        POLICY,
        EXPR
    }

    @Override
    public int hashCode() {
        if (varType == VarType.FINDING) return finding.hashCode();
        if (varType == VarType.POLICY) return policy.hashCode();
        if (varType == VarType.EXPR) return forcedExpr.hashCode();
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticVar<?> that = (StaticVar<?>) o;
        if (varType != that.varType) return false;
        if (varType == VarType.FINDING) return finding.equals(that.finding);
        if (varType == VarType.POLICY) return policy.equals(that.policy);
        if (varType == VarType.EXPR) return forcedExpr.equals(that.forcedExpr);
        return false;
    }

    public DynamicVar<T> convert(EncodedAPI<T> encoder) {
        if (cachedDynamicVar != null) {
            return cachedDynamicVar;
        }

        T value = switch (varType) {
            case FINDING -> finding.encode(encoder);
            case POLICY -> policy.encode(encoder);
            case EXPR -> forcedExpr;
        };

        cachedDynamicVar = new DynamicVar<>(encoder, value);
        return cachedDynamicVar;
    }

    public Object getValue() {
        return switch (varType) {
            case FINDING -> finding;
            case POLICY -> policy;
            case EXPR -> forcedExpr;
        };
    }

    public VarType getVarType() {
        return varType;
    }
}
