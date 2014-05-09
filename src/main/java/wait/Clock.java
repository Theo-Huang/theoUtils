package wait;

public interface Clock {

  long now();

  long laterBy(long durationInMillis);

  boolean isNowBefore(long endInMillis);

}
