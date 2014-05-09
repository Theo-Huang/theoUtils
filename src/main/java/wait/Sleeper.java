package wait;

import java.util.concurrent.TimeUnit;

public interface Sleeper {

  public static final Sleeper SYSTEM_SLEEPER = new Sleeper() {
    public void sleep(Duration duration) throws InterruptedException {
      Thread.sleep(duration.in(TimeUnit.MILLISECONDS));
    }
  };

  void sleep(Duration duration) throws InterruptedException;
}
