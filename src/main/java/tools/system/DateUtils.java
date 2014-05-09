package tools.system;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

  public static final String getDateString() {
    return getDateString("MM-dd-HH_mm_ss");
  }

  public static final String getDateString(String format) {
    DateFormat dateFormat = new SimpleDateFormat(format);
    Date date = new Date();
    return dateFormat.format(date);
  }
}
