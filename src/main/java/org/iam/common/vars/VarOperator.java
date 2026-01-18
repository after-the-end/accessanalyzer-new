package org.iam.common.vars;

import org.iam.common.apis.StringComparableEnum;

public enum VarOperator implements StringComparableEnum {
    STRING_MATCH("StringMatch"),
    STRING_NOT_MATCH("StringNotMatch"),
    STRING_LIKE("StringLike"),
    STRING_NOT_LIKE("StringNotLike"),
    STRING_EQUALS("StringEquals"),
    STRING_NOT_EQUALS("StringNotEquals"),
    STRING_EQUALS_IGNORE_CASE("StringEqualsIgnoreCase"),
    STRING_NOT_EQUALS_IGNORE_CASE("StringNotEqualsIgnoreCase"),

    IP_ADDRESS("IpAddress"),
    NOT_IP_ADDRESS("NotIpAddress"),

    STRING_EQUALS_IF_EXISTS("StringEqualsIfExists"),
    STRING_NOT_EQUALS_IF_EXISTS("StringNotEqualsIfExists"),
    STRING_MATCH_IF_EXISTS("StringMatchIfExists"),
    STRING_NOT_MATCH_IF_EXISTS("StringNotMatchIfExists"),
    STRING_EQUALS_IGNORE_CASE_IF_EXISTS("StringEqualsIgnoreCaseIfExists"),
    STRING_NOT_EQUALS_IGNORE_CASE_IF_EXISTS("StringNotEqualsIgnoreCaseIfExists"),

    IP_ADDRESS_IF_EXISTS("IpAddressIfExists"),
    NOT_IP_ADDRESS_IF_EXISTS("NotIpAddressIfExists"),

    FOR_ALL_VALUES_STRING_EQUALS("ForAllValues:StringEquals"),
    FOR_ALL_VALUES_STRING_NOT_EQUALS("ForAllValues:StringNotEquals"),
    FOR_ALL_VALUES_STRING_MATCH("ForAllValues:StringMatch"),
    FOR_ALL_VALUES_STRING_NOT_MATCH("ForAllValues:StringNotMatch"),
    FOR_ALL_VALUES_STRING_EQUALS_IGNORE_CASE("ForAllValues:StringEqualsIgnoreCase"),
    FOR_ALL_VALUES_STRING_NOT_EQUALS_IGNORE_CASE("ForAllValues:StringNotEqualsIgnoreCase"),

    FOR_ANY_VALUE_STRING_EQUALS("ForAnyValue:StringEquals"),
    FOR_ANY_VALUE_STRING_NOT_EQUALS("ForAnyValue:StringNotEquals"),
    FOR_ANY_VALUE_STRING_MATCH("ForAnyValue:StringMatch"),
    FOR_ANY_VALUE_STRING_NOT_MATCH("ForAnyValue:StringNotMatch"),
    FOR_ANY_VALUE_STRING_EQUALS_IGNORE_CASE("ForAnyValue:StringEqualsIgnoreCase"),
    FOR_ANY_VALUE_STRING_NOT_EQUALS_IGNORE_CASE("ForAnyValue:StringNotEqualsIgnoreCase"),

    ARN_LIKE("ArnLike"),
    ARN_NOT_LIKE("ArnNotLike");

    private final String value;

    VarOperator(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return StringComparableEnum.isValid(value, VarOperator.class);
    }

    public static VarOperator fromString(String value) {
        return StringComparableEnum.fromString(value, VarOperator.class);
    }

    @Override
    public String toString() {
        return "VarOperator{" +
                "value='" + value + '\'' +
                '}';
    }
}