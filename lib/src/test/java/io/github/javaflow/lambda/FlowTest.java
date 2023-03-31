package io.github.javaflow.lambda;

import io.github.javaflow.lambda.resilience.RecoverStrategy;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class FlowTest {

    Flow<String, Double> doubleParsingFlow = Double::parseDouble;

    @Test
    void via_ShouldBeProcessed_WhenViaFlow() {
        final Source<String> numberStringSource = t -> "123456.654321";
        numberStringSource.via(doubleParsingFlow).to(t -> {
            assertEquals(123456.654321, t);
            return null;
        });
    }

    @Test
    void withRetry_ShouldRetry_WhenRuntimeExceptionOccurring() {

        final AtomicInteger ai = new AtomicInteger(3);
        final Source<Integer> exceptionSource = t -> {
            final var i = ai.decrementAndGet();
            if (i > 1) throw new RuntimeException();
            else return i;
        };
        exceptionSource.withRetry(Retry.ofDefaults("retry 3 times")).to(t -> {
            assertEquals(1, t);
            return null;
        });
    }

    @Test
    void withRecover_ShouldRecover_WhenMatchingException() {
        final Source<String> exceptionSource = t -> {
            throw new RuntimeException();
        };
        final var expected = "fallback string";
        exceptionSource
                .withRecover(ex -> new RecoverStrategy<Throwable, Flow<Void, String>>()
                        .match(RuntimeException.class, Flow.source(expected)).apply(ex))
                .to(actual -> {
                    assertEquals(expected, actual);
                    return null;
                });
    }

    @Test
    void tryRecover_ShouldHandleSuccess_WhenNoException() {
        final var expected = "abcd";
        Flow.source(expected)
                .tryRecover(
                        o -> assertEquals(expected, o),
                        ex -> fail(),
                        ex -> new RecoverStrategy<Throwable, Flow<Void, String>>()
                                .match(RuntimeException.class, Flow.source("fallback string")).apply(ex))
                .to(actual -> {
                    assertEquals(expected, actual);
                    return null;
                });
    }

    @Test
    void tryRecover_ShouldBeAbleToHandleFailureAndRecover_WhenExceptionOccur() {
        final Source<String> exceptionSource = t -> {
            throw new RuntimeException();
        };
        final var expected = "fallback string";
        exceptionSource
                .tryRecover(
                        o -> fail(),
                        ex -> assertInstanceOf(RuntimeException.class, ex),
                        ex -> new RecoverStrategy<Throwable, Flow<Void, String>>()
                                .match(RuntimeException.class, Flow.source(expected)).apply(ex))
                .to(actual -> {
                    assertEquals(expected, actual);
                    return null;
                });
    }

    @Test
    void identity_ShouldGetWhat_WhenGivingWhat() {
        final var expected = "123456.654321";
        Flow.source(expected).via(Flow.identity()).to(t -> {
            assertEquals(expected, t);
            return null;
        });
    }
}
