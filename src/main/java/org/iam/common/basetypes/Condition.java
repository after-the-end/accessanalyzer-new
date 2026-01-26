package org.iam.common.basetypes;

import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarKey;
import org.iam.common.vars.VarOperator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Condition<T> implements GrammarlyAPI<T> {
    private VarOperator operator;
    private VarKey key;
    private Set<String> values;
    private T cachedExpr = null;

    public Condition() {}

    public Condition(String operator, String key, Set<String> values) {
        try{
            this.operator = VarOperator.fromString(operator);
            this.key = VarKey.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operator or key in Condition: " + e.getMessage());
        }
        this.values = values;

        if (!isValidCondition()) {
            throw new IllegalArgumentException("Incompatible operator and key in Condition: " + this.toString());
        }
    }

    public Condition(VarOperator operator, Map<String, Set<String>> kvMap) {
        String keyStr = kvMap.keySet().iterator().next();
        try {
            this.operator = operator;
            this.key = VarKey.fromString(keyStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid key in Condition: " + e.getMessage());
        }
        this.values = kvMap.get(keyStr);

        if (!isValidCondition()) {
            throw new IllegalArgumentException("Incompatible operator and key in Condition: " + this.toString());
        }
    }

    @Override
    public final T encode(EncodedAPI<T> helper) {
        if (this.cachedExpr == null) {
            switch (this.operator) {
                // TODO: Implement the encoding logic for each operator.
                case STRING_EQUALS, STRING_EQUALS_IF_EXISTS, FOR_ALL_VALUES_STRING_EQUALS, FOR_ANY_VALUE_STRING_EQUALS -> {}
                case STRING_NOT_EQUALS, STRING_NOT_EQUALS_IF_EXISTS, FOR_ALL_VALUES_STRING_NOT_EQUALS, FOR_ANY_VALUE_STRING_NOT_EQUALS -> {}
                case STRING_EQUALS_IGNORE_CASE, STRING_EQUALS_IGNORE_CASE_IF_EXISTS, FOR_ALL_VALUES_STRING_EQUALS_IGNORE_CASE, FOR_ANY_VALUE_STRING_EQUALS_IGNORE_CASE -> {}
                case STRING_NOT_EQUALS_IGNORE_CASE, STRING_NOT_EQUALS_IGNORE_CASE_IF_EXISTS, FOR_ALL_VALUES_STRING_NOT_EQUALS_IGNORE_CASE, FOR_ANY_VALUE_STRING_NOT_EQUALS_IGNORE_CASE -> {}
                case STRING_LIKE, STRING_MATCH, STRING_MATCH_IF_EXISTS, FOR_ALL_VALUES_STRING_MATCH, FOR_ANY_VALUE_STRING_MATCH, ARN_LIKE -> {}
                case STRING_NOT_LIKE, STRING_NOT_MATCH, STRING_NOT_MATCH_IF_EXISTS, FOR_ALL_VALUES_STRING_NOT_MATCH, FOR_ANY_VALUE_STRING_NOT_MATCH, ARN_NOT_LIKE -> {}
                case IP_ADDRESS, IP_ADDRESS_IF_EXISTS -> {}
                case NOT_IP_ADDRESS, NOT_IP_ADDRESS_IF_EXISTS -> {}
            }
        }
        return this.cachedExpr;
    }

    private Boolean isValidCondition() {
        // TODO: Check whether Operator matches VarKey.
        return true;
    }

    public VarOperator getOperator() {
        return operator;
    }

    public void setOperator(VarOperator operator) {
        this.operator = operator;
    }

    public VarKey getKey() {
        return key;
    }

    public void setKey(VarKey key) {
        this.key = key;
    }

    public Set<String> getValues() {
        return values;
    }

    public void setValues(Set<String> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition<?> condition = (Condition<?>) o;
        return operator == condition.operator && Objects.equals(key, condition.key) && Objects.equals(values, condition.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, key, values);
    }

    @Override
    public String toString() {
        return "Condition{" +
                "operator=" + operator +
                ", key=" + key +
                ", values=" + values +
                '}';
    }
}
