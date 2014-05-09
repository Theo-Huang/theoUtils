package exception;

public class UtilityException extends RuntimeException{
	private static final long serialVersionUID = 10L;

	public UtilityException() {
	    super();
	  }

	  public UtilityException(String message) {
	    super(message);
	  }

	  public UtilityException(Throwable cause) {
	    super(cause);
	  }

	  public UtilityException(String message, Throwable cause) {
	    super(message, cause);
	  }

	  @Override
	  public String getMessage() {
	    return createMessage(super.getMessage());
	  }

	  private String createMessage(String originalMessageString) {
	    return String.format("%sSystem info: %s\nDriver info: %s",
	                         originalMessageString == null ? "" : originalMessageString + "\n",
	                         getSystemInformation(),
	                         getDriverInformation());
	  }

	  public String getSystemInformation() {
	    return String.format("os.name: '%s', os.arch: '%s', os.version: '%s', java.version: '%s'",
	                         System.getProperty("os.name"),
	                         System.getProperty("os.arch"),
	                         System.getProperty("os.version"),
	                         System.getProperty("java.version"));
	  }

	  public String getDriverInformation() {
	    return "driver.version: " + getDriverName(getStackTrace());
	  }

	  public static String getDriverName(StackTraceElement[] stackTraceElements) {
	    String driverName = "unknown";
	    for (StackTraceElement e : stackTraceElements) {
	      if (e.getClassName().endsWith("Driver")) {
	        String[] bits = e.getClassName().split("\\.");
	        driverName = bits[bits.length - 1];
	      }
	    }

	    return driverName;
	  }
}
