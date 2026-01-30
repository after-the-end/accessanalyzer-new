package org.iam.common.vars;

import org.iam.common.apis.StringComparableEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * All the Keys used for comparison
 */
public enum VarKey implements StringComparableEnum {
    /**
     * From Principal
     */
    AWS("AWS"), IAM("IAM"), SERVICE("Service"), FEDERATED("Federated"),
    /**
     * From Action
     */
    ACTION("Action"),
    /**
     * From Resource
     */
    RESOURCE("Resource"),
    /**
     * From Operator
     */
    AWS_PRINCIPAL_ARN("aws:PrincipalArn"),
    AWS_SOURCE_ARN("aws:SourceArn"),
    AWS_SOURCE_IP("aws:SourceIp");

    private final String value;

    @JsonCreator
    VarKey(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return StringComparableEnum.isValid(value, VarKey.class);
    }

    public static VarKey fromString(String value) {
        return StringComparableEnum.fromString(value, VarKey.class);
    }
}
