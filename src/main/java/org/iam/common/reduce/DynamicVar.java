package org.iam.common.reduce;

import org.iam.common.apis.EncodedAPI;
import java.util.List;
import java.util.Objects;

public class DynamicVar<T> {
    private final EncodedAPI<T> encoder;
    private final T value;

    public DynamicVar(EncodedAPI<T> encoder, T value) {
        this.encoder = encoder;
        this.value = value;
    }

    public DynamicVar<T> union(DynamicVar<T> other) {
        T unionTerm = encoder.or(List.of(this.value, other.value));
        return new DynamicVar<>(encoder, unionTerm);
    }

    public DynamicVar<T> inter(DynamicVar<T> other) {
        T interTerm = encoder.and(this.value, other.value);
        return new DynamicVar<>(encoder, interTerm);
    }

    public DynamicVar<T> minus(DynamicVar<T> other) {
        T minusTerm = encoder.and(this.value, encoder.not(other.value));
        return new DynamicVar<>(encoder, minusTerm);
    }

    public T getValue() {
        return value;
    }

    public boolean isEmpty() {
        return !encoder.check(value);
    }

    @Override
    public String toString() {
        return "DynamicVar{" + "value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicVar<?> that = (DynamicVar<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
