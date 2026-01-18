package org.iam.common.vars;

import org.iam.common.apis.StringComparableEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VarEffect implements StringComparableEnum {
    Allow("ALLOW"), Deny("DENY");

    private final String value;

    @JsonCreator
    VarEffect(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return StringComparableEnum.isValid(value, VarEffect.class);
    }

    public static VarEffect fromString(String value) {
        return StringComparableEnum.fromString(value, VarEffect.class);
    }
}
