package io.github.javaflow.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {{{
 * +-------------------+
 * | Resulting Flow    |
 * |                   |
 * | +------+          |
 * | |      |          |
 * | | this | ~~> In1  |
 * | |      |          |
 * | +------+          |
 * |          ~mix~    |~~> Out
 * | +------+          |
 * | |      |          |
 * | | that | ~~> In2  |
 * | |      |          |
 * | +------+          |
 * +-------------------+
 * }}}
 */
public interface Mixable<In1, In2, OUT> {

    Logger log = LoggerFactory.getLogger(Mixable.class);

    default Flow<Void, OUT> mix(final Flow<Void, In1> flow1, final Flow<Void, In2> flow2) {
        final AtomicReference<In1> in1AtomicReference = new AtomicReference<>();
        final AtomicReference<In2> in2AtomicReference = new AtomicReference<>();
        flow1.to(in1 -> {
            in1AtomicReference.set(in1);
            return null;
        });
        flow2.to(in2 -> {
            in2AtomicReference.set(in2);
            return null;
        });
        return unused -> {
            final var start = Instant.now();
            final var result = apply(in1AtomicReference.get(), in2AtomicReference.get());
            final var finish = Instant.now();
            log.info("{} time consumed {}ms", getClass().getSimpleName(), Duration.between(start, finish).toMillis());
            return result;
        };
    }

    OUT apply(final In1 in1, final In2 in2);

}
