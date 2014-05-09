package exception;

public class TimeoutException extends UtilityException {

	private static final long serialVersionUID = 12L;

	public TimeoutException() {
	  }

	  public TimeoutException(String message) {
	    super(message);
	  }

	  public TimeoutException(Throwable cause) {
	    super(cause);
	  }

	  public TimeoutException(String message, Throwable cause) {
	    super(message, cause);
	  }
	}