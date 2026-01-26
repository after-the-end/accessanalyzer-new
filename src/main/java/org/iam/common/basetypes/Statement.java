package org.iam.common.basetypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.apis.GrammarlyAPI;
import org.iam.common.vars.VarEffect;
import org.iam.common.vars.VarKey;

import java.util.*;

public class Statement<T> implements GrammarlyAPI<T> {

    @JsonProperty("Effect")
    private VarEffect effect = null;

    @JsonProperty("Principal")
    @JsonDeserialize(using = PrincipalDeserializer.class)
    private Map<VarKey, Set<String>> principal = null;

    @JsonProperty("Action")
    @JsonDeserialize(using = StringDeserializer.class)
    private Set<String> action = null;

    @JsonProperty("Resource")
    @JsonDeserialize(using = StringDeserializer.class)
    private Set<String> resource = null;

    @JsonProperty("Condition")
    @JsonDeserialize(using = ConditionDeserializer.class)
    private Set<Condition<T>> condition = null;

    @JsonIgnore
    private T cachedExpr = null;

    public Statement(Statement<T> other) {
        this.effect = other.effect;
        this.principal = other.principal;
        this.action = other.action;
        this.resource = other.resource;
        this.condition = other.condition;
        this.cachedExpr = other.cachedExpr;
    }

    public Statement(VarEffect effect, Map<VarKey, Set<String>> principal, Set<String> action, Set<String> resource, Set<Condition<T>> condition) {
        this.effect = effect;
        this.principal = principal;
        this.action = action;
        this.resource = resource;
        this.condition = condition;
    }

    // This constructor is abandoned temporarily.
//    public Statement(Finding finding) {
//        this.effect = VarEffect.Allow;
//        this.principal = finding.getPrincipal();
//        this.action = finding.getAction();
//        this.resource = finding.getResource();
//        this.condition = finding.getCondition();
//    }

    public VarEffect getEffect() {
        return effect;
    }

    public void setEffect(VarEffect effect) {
        this.effect = effect;
    }

    public Set<String> getAction() {
        return action;
    }

    public void setAction(Set<String> action) {
        this.action = action;
    }

    public Set<String> getResource() {
        return resource;
    }

    public void setResource(Set<String> resource) {
        this.resource = resource;
    }

    public Set<Condition<T>> getCondition() {
        return condition;
    }

    public void setCondition(Set<Condition<T>> condition) {
        this.condition = condition;
    }

    public Map<VarKey, Set<String>> getPrincipal() {
        return principal;
    }

    public void setPrincipal(Map<VarKey, Set<String>> principal) {
        this.principal = principal;
    }

    @Override
    public T encode(EncodedAPI<T> helper) {
        if (this.cachedExpr == null) {
            List<T> exprs = new ArrayList<>();
            exprs.add(encodePrincipals(helper));
            exprs.add(encodeActions(helper));
            exprs.add(encodeResources(helper));
            if (this.condition != null && !this.condition.isEmpty()) {
                exprs.addAll(
                        condition.stream()
                                .map(c -> c.encode(helper))
                                .toList());
            }

            T allowExpr = helper.and(principalExpr, actionExpr, resourceExpr, conditionExpr);
            if (this.effect == VarEffect.Allow) {
                this.cachedExpr = allowExpr;
            } else {
                this.cachedExpr = helper.not(allowExpr);
            }
        }
        return this.cachedExpr;
    }

    private T encodePrincipals(EncodedAPI<T> helper) {
        if (this.principal == null || this.principal.isEmpty()) {
            return helper.mkFalse();
        }

        List<T> exprs = this.principal.entrySet().stream()
                .map(entry -> encodeOnePrincipal(entry.getKey(), entry.getValue(), helper))
                .toList();
        return helper.or(exprs);
    }

    private T encodeOnePrincipal(VarKey key, Set<String> values, EncodedAPI<T> helper) {
        if (values == null || values.isEmpty()) {
            return helper.mkFalse();
        }
        if (values.contains(".*")) {
            return helper.mkTrue();
        }

        T verifier = helper.mkStringConst(key.toString());

        List<T> matches = values.stream()
                .map(value -> helper.mkReMatch(verifier, value))
                .toList();
        return helper.or(matches);
    }

    private T encodeActions(EncodedAPI<T> helper) {
        if (this.action == null || this.action.isEmpty()) {
            return helper.mkFalse();
        }
        if (this.action.contains(".*")) {
            return helper.mkTrue();
        }

        List<T> matches = this.action.stream()
                .map(actionStr -> {
                    T actionConst = helper.mkStringConst(VarKey.ACTION.toString());
                    // Here are some problems in the original code.
                    return helper.mkReMatch(actionConst, actionStr);
                })
                .toList();
        return helper.or(matches);
    }

    private T encodeResources(EncodedAPI<T> helper) {
        if (this.resource == null || this.resource.isEmpty()) {
            return helper.mkFalse();
        }
        if (this.resource.contains(".*")) {
            return helper.mkTrue();
        }

        List<T> matches = this.resource.stream()
                .map(resourceStr -> {
                    T resourceConst = helper.mkStringConst(VarKey.RESOURCE.toString());
                    return helper.mkReMatch(resourceConst, resourceStr);
                })
                .toList();
        return helper.or(matches);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Statement<?> statement)) return false;
        return effect == statement.effect
                && Objects.equals(principal, statement.principal)
                && Objects.equals(action, statement.action)
                && Objects.equals(resource, statement.resource)
                && Objects.equals(condition, statement.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effect, principal, action, resource, condition);
    }
}