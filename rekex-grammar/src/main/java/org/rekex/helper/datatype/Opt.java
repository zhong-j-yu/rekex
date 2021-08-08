package org.rekex.helper.datatype;

// optional.
//
// why the short name: Opt<Foo> is probably used a lot in a grammar.
//   we also don't want to clash with existing Option/Optional names.
// If user prefers some other Optional types, they can easily
//   do so with ctors in a rule catalog.

import org.rekex.spec.Ctor;
import org.rekex.spec.Permits;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An optional value of `T`.
 */
@Permits({Opt.Some.class, Opt.None.class})
public sealed interface Opt<T>
{
    /**
     * Invoke the action if `this` is {@link Some}.
     */
    void ifPresent(Consumer<T> action);

    /**
     * Map the value, depending on the type of `this`.
     */
    <R> R caseOf(Function<T,R> mapSome, Supplier<R> mapNone);

    record Some<T>
        (T value)
        implements Opt<T>
    {
        @Override
        public void ifPresent(Consumer<T> action)
        {
            action.accept(value);
        }

        @Override
        public <R> R caseOf(Function<T, R> mapSome, Supplier<R> mapNone)
        {
            return mapSome.apply(value);
        }
    }

    record None<T>() implements Opt<T>
    {
        @Ctor public static <T> None<T> ctor(/* empty */)
        {
            return new None<>();
        }

        @Override
        public void ifPresent(Consumer<T> action)
        {
            // nothing
        }

        @Override
        public <R> R caseOf(Function<T, R> mapSome, Supplier<R> mapNone)
        {
            return mapNone.get();
        }
    }
}
