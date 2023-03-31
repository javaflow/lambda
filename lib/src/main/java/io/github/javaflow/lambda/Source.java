package io.github.javaflow.lambda;

public interface Source<Out> extends Flow<Void, Out> {
    static <T> Source<T> of(T out) {
        return (Source<T>) Flow.source(out);
    }
}
