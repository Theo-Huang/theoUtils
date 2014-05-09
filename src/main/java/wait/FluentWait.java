package wait;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import exception.UtilityException;
import exception.TimeoutException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class FluentWait<T> implements Wait<T>{

	  public static Duration FIVE_HUNDRED_MILLIS = new Duration(500, MILLISECONDS);
	  private final T input;
	  private final Clock clock;
	  private final Sleeper sleeper;
 
	  private Duration timeout = FIVE_HUNDRED_MILLIS;
	  private Duration interval = FIVE_HUNDRED_MILLIS;
	  private String message = null;

	  private List<Class<? extends Throwable>> ignoredExceptions = Lists.newLinkedList();

	  public FluentWait(T input) {
	    this(input, new SystemClock(), Sleeper.SYSTEM_SLEEPER);
	  }
	  public FluentWait(T input, Clock clock, Sleeper sleeper) {
	    this.input = checkNotNull(input);
	    this.clock = checkNotNull(clock);
	    this.sleeper = checkNotNull(sleeper);
	  }
	  public FluentWait<T> withTimeout(long duration, TimeUnit unit) {
	    this.timeout = new Duration(duration, unit);
	    return this;
	  }
	  public FluentWait<T> withMessage(String message) {
	    this.message = message;
	    return this;
	  }

	  public FluentWait<T> pollingEvery(long duration, TimeUnit unit) {
	    this.interval = new Duration(duration, unit);
	    return this;
	  }
	  public <K extends Throwable> FluentWait<T> ignoreAll(Collection<Class<? extends K>> types) {
	    ignoredExceptions.addAll(types);
	    return this;
	  }

	  public FluentWait<T> ignoring(Class<? extends Throwable> exceptionType) {
	    return this.ignoreAll(ImmutableList.<Class<? extends Throwable>>of(exceptionType));
	  }

	  public FluentWait<T> ignoring(Class<? extends Throwable> firstType,
	                                Class<? extends Throwable> secondType) {

	    return this.ignoreAll(ImmutableList.<Class<? extends Throwable>>of(firstType, secondType));
	  }
	  public void until(final Predicate<T> isTrue) {
	    until(new Function<T, Boolean>() {
	      public Boolean apply(T input) {
	        return isTrue.apply(input);
	      }

	      public String toString() {
	        return isTrue.toString();
	      }
	    });
	  }
	  public <V> V until(Function<? super T, V> isTrue) {
	    long end = clock.laterBy(timeout.in(MILLISECONDS));
	    Throwable lastException = null;
	    while (true) {
	      try {
	        V value = isTrue.apply(input);
	        if (value != null && Boolean.class.equals(value.getClass())) {
	          if (Boolean.TRUE.equals(value)) {
	            return value;
	          }
	        } else if (value != null) {
	          return value;
	        }
	      } catch (Throwable e) {
	        lastException = propagateIfNotIngored(e);
	      }

	      // Check the timeout after evaluating the function to ensure conditions
	      // with a zero timeout can succeed.
	      if (!clock.isNowBefore(end)) {
	        String toAppend = message == null ?
	            " waiting for " + isTrue.toString() : ": " + message;

	        String timeoutMessage = String.format("Timed out after %d seconds%s",
	            timeout.in(SECONDS), toAppend);
	        throw timeoutException(timeoutMessage, lastException);
	      }

	      try {
	        sleeper.sleep(interval);
	      } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	        throw new UtilityException(e);
	      }
	    }
	  }

	  private Throwable propagateIfNotIngored(Throwable e) {
	    for (Class<? extends Throwable> ignoredException : ignoredExceptions) {
	      if (ignoredException.isInstance(e)) {
	        return e;
	      }
	    }
	    throw Throwables.propagate(e);
	  }
	  	protected RuntimeException timeoutException(String message, Throwable lastException) {
	    throw new TimeoutException(message, lastException);
	  }

}
