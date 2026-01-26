package org.iam.common.apis;

import java.util.List;

/**
 * Define the basic operations of how to encode and check.
 * T: BoolExpr (Z3), Term (CVC5).
 */
public interface EncodedAPI<T> {

    T and(List<T> exprs);

    T and(T... exprs);

    T and(GrammarlyAPI<T>... exprs);

    T or(List<T> exprs);

    T or(T... exprs);

    T or(GrammarlyAPI<T>... exprs);

    T not(T expr);

    T not(GrammarlyAPI<T> expr);

    Boolean check(T expr);

    Boolean greaterThan(T lhs, T rhs);

    Boolean greaterEquals(T lhs, T rhs);

    // TODO: Maybe there are more methods whose parameters are String or GrammarlyAPI<T> waiting to be added.

}
