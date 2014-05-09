package wait;

import java.util.concurrent.TimeUnit;

import exception.NotFoundException;

public class WaitUntil extends FluentWait<Object> {

  public final static long DEFAULT_SLEEP_TIMEOUT = 500;

  public WaitUntil(Object input, long timeOutInSeconds) {
    this(input, new SystemClock(), Sleeper.SYSTEM_SLEEPER, timeOutInSeconds, DEFAULT_SLEEP_TIMEOUT);
  }

  public WaitUntil(Object input, long timeOutInSeconds, long sleepInMillis) {
    this(input, new SystemClock(), Sleeper.SYSTEM_SLEEPER, timeOutInSeconds, sleepInMillis);
  }

  protected WaitUntil(Object intput, Clock clock, Sleeper sleeper, long timeOutInSeconds, long sleepTimeOut) {
    super(intput, clock, sleeper);
    withTimeout(timeOutInSeconds, TimeUnit.SECONDS);
    pollingEvery(sleepTimeOut, TimeUnit.MILLISECONDS);
    ignoring(NotFoundException.class);
  }

}
