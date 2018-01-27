import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vavr.control.Validation;

import static org.hamcrest.CoreMatchers.*;

@RunWith(Parameterized.class)
public class ValidationToTryTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        final Validation<String,String> invalid =
                Validation.invalid("a problem");

        final Validation<? extends Throwable,String> invalidViaMapError =
                invalid.mapError(IllegalArgumentException::new);

        final Either<String,String> leftEither =
                invalid.toEither();

        return Arrays.asList(new Object[][] {

                { "via mapError() then toTry()",
                  Try.of(()-> invalidViaMapError.toTry().get()) },

                { "via getOrElseThrow()",
                  Try.of(()-> invalid.getOrElseThrow(IllegalArgumentException::new)) },

                { "via mapError() then user-defined toTry()",
                  Try.of(()-> toTry(invalidViaMapError).get()) },

                { "via toEither() then toTry()",
                  /*
                   If we try to use a method reference here, the compiler helpfully informs us that the
                   reference is ambiguous. You see, IllegalArgumentException has a default constructor
                   and one that takes a String. getOrElseThrow() is defined up in Value and down in
                   Either. The one up in Value takes a Supplier whereas the one down in Either takes
                   a Function. The latter is passed the left value if the Either isLeft()
                   */
                  Try.of(()-> leftEither.getOrElseThrow(error->new IllegalArgumentException(error)))}

        });
    }

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public Try<String> tryGet;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void test() {
        collector.checkThat(description, tryGet.isFailure(), is(true)); // ok
        collector.checkThat(description, tryGet.getCause(),is(instanceOf(IllegalArgumentException.class))); // ok
        collector.checkThat(description, tryGet.getCause().getMessage(),is("a problem")); // ok
    }

    private static <T> Try<T> toTry(final Validation<? extends Throwable,T> v) {
        // avoid calling v.toTry() if v.isInvalid()
        return v.isValid() ? v.toTry() : Try.failure(v.getError());
    }

}
