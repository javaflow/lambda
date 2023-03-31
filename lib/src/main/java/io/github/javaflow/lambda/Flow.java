package io.github.javaflow.lambda;

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Flow<In, Out> extends Function<In, Out> {

    Logger log = LoggerFactory.getLogger(Flow.class);

    Out apply(In t);

    default <V> Flow<In, V> via(Flow<? super Out, ? extends V> after) {
        Objects.requireNonNull(after);
        return in -> {
            final var start = Instant.now();
            final V v = after.apply(apply(in));
            final var finish = Instant.now();
            log.info("{} time consumed {}ms", getClass().getSimpleName(), Duration.between(start, finish).toMillis());
            return v;
        };
    }


    /**
     * Decorate as retryable flow for <code>RuntimeException</code>
     *
     * @param retry config
     * @return a retryable flow
     */
    default Flow<In, Out> withRetry(Retry retry) {
        return in -> {
            final Retry.Context<Out> context = retry.context();
            do {
                try {
                    final Out result = apply(in);
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (RuntimeException runtimeException) {
                    context.onRuntimeError(runtimeException);
                }
            } while (true);
        };
    }

    /**
     * Returns this flow if success, otherwise tries to recover the exception with recoverFn.
     *
     * @param recoverFn recover strategies
     * @return original flow if no exception, fallback flow otherwise and log warn the failure
     */
    default Flow<In, Out> withRecover(Function<Throwable, Flow<In, Out>> recoverFn) {
        return tryRecover(out -> { }, ex -> log.warn("Recover since exception occur in flow: ", ex), recoverFn);
    }

    /**
     * Returns this flow if success, otherwise tries to recover the exception with recoverFn.
     * Add ability to do side effect handle when this flow succeeds/fails
     *
     * @param handleSuccess success callback
     * @param handleFailure failure callback
     * @param recoverFn recover strategies if fail
     * @return original flow if no exception, fallback flow otherwise
     */
    default Flow<In, Out> tryRecover(Consumer<Out> handleSuccess,
                                     Consumer<Throwable> handleFailure,
                                     Function<Throwable, Flow<In, Out>> recoverFn) {
        return in -> Try.ofSupplier(() -> apply(in))
                .onSuccess(handleSuccess)
                .onFailure(handleFailure)
                .recover(Throwable.class, t -> recoverFn.apply(t).apply(in)).get();
    }

    default Flow<In, Out> tryRecover(BiConsumer<In, Out> handleSuccess,
                                     Consumer<Throwable> handleFailure,
                                     Function<Throwable, Flow<In, Out>> recoverFn) {
        return in -> Try.ofSupplier(() -> apply(in))
                .onSuccess(t -> handleSuccess.accept(in, t))
                .onFailure(handleFailure)
                .recover(Throwable.class, t -> recoverFn.apply(t).apply(in)).get();
    }

    default Void to(Sink<Out> sink) {
        return via(sink).apply(null);
    }

    static <Out> Flow<Void, Out> source(Out source) {
        return any -> source;
    }

    static <In> Flow<In, In> identity() {
        return t -> t;
    }
}
