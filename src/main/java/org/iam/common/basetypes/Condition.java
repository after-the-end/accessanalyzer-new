package org.iam.common.basetypes;

import org.iam.common.vars.VarOperator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Condition {
    protected VarOperator operator;

    protected Map<String, Set<String>> keyToValues;

    public Condition(Condition other) {
        this.operator = other.operator;
        this.keyToValues = other.keyToValues;
    }

    public Condition(VarOperator operator, Map<String, Set<String>> keyToValues) {
        this.operator = operator;
        this.keyToValues = keyToValues;
    }

    public VarOperator getOperator() {
        return operator;
    }

    public void setOperator(VarOperator operator) {
        this.operator = operator;
    }

    public Map<String, Set<String>> getKeyToValues() {
        return keyToValues;
    }

    public void setKeyToValues(Map<String, Set<String>> keyToValues) {
        this.keyToValues = keyToValues;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return operator == condition.operator && Objects.equals(keyToValues, condition.keyToValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, keyToValues);
    }

    @Override
    public String toString() {
        return "Condition{" +
                "operator=" + operator +
                ", keyToValues=" + keyToValues +
                '}';
    }
}
