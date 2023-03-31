package io.github.javaflow.lambda.resilience;

import io.vavr.API.Match.Case;
import io.vavr.PartialFunction;

import java.util.LinkedList;
import java.util.List;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

/**
 * Recover strategies declaring builder.
 *
 * @param <T> The super exception type that can be caught, like <code>Throwable</code>,<code>RuntimeException</code>
 * @param <F> Fallback type for recover, e.g. <code>Flow&lt;Void, String&gt;</code>
 */
public class RecoverStrategy<T extends Throwable, F> implements PartialFunction<T, F> {

    public static <T extends Throwable, F> RecoverStrategy<T, F> of() {
        return new RecoverStrategy<>();
    }

    public static <F> RecoverStrategy<Throwable, F> ofThrowable(final F f) {
        return new RecoverStrategy<Throwable, F>().match(Throwable.class, f);
    }

    protected List<Case<T, F>> statements = new LinkedList<>();

    /**
     * Match one of recover strategies: exception -> fallback.
     *
     * @param classOfEx some type of exception
     * @param fallback  fallback/backup
     * @param <E>       some kind of exception
     * @return builder itself
     */
    public <E> RecoverStrategy<T, F> match(final Class<E> classOfEx, final F fallback) {
        addStatement(Case($(classOfEx::isInstance), ex -> fallback));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public F apply(final T e) {
        if (!isDefinedAt(e))
            throw new NoSuchMatchException("Cannot find any matched exception types for " + e, e);
        return (F) Match(e).of(build());
    }

    @Override
    public boolean isDefinedAt(final T value) {
        return statements.stream().anyMatch(it -> it.isDefinedAt(value));
    }

    protected void addStatement(final Case<T, F> statement) {
        statements.add(statement);
    }

    @SuppressWarnings("rawtypes")
    private Case[] build() {
        final Case[] cases = new Case[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            cases[i] = statements.get(i);
        }
        return cases;
    }

    static class NoSuchMatchException extends RuntimeException {
        public NoSuchMatchException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
