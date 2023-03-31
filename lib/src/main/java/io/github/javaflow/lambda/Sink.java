package io.github.javaflow.lambda;

public interface Sink<In> extends Flow<In, Void> {
    default Void to(Sink<Void> sink) {
        throw new UnsupportedOperationException("Can not sink to another sink");
    }
}
