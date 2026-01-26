package org.iam.common.apis;

import java.util.List;

/**
 * Grammar-Layer API, which represents an object that can be encoded logically.
 *
 * @param <T> The expression type of the underlying solver (Z3: BoolExpr, CVC5: Term)
 */
public interface GrammarlyAPI<T> {

    T encode(EncodedAPI<T> helper);

    default T and(GrammarlyAPI<T> rhs, EncodedAPI<T> helper) {
        return helper.and(List.of(this.encode(helper), rhs.encode(helper)));
    }

    default T or(GrammarlyAPI<T> rhs, EncodedAPI<T> helper) {
        return helper.or(List.of(this.encode(helper), rhs.encode(helper)));
    }

    default T not(EncodedAPI<T> helper) {
        return helper.not(this.encode(helper));
    }

    default Boolean greaterThan(GrammarlyAPI<T> rhs, EncodedAPI<T> helper) {
        return helper.greaterThan(this.encode(helper), rhs.encode(helper));
    }

    default Boolean greaterEquals(GrammarlyAPI<T> rhs, EncodedAPI<T> helper) {;
        return helper.greaterEquals(this.encode(helper), rhs.encode(helper));
    }
}
