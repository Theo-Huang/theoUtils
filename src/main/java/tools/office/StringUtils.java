package tools.office;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
  private static final String UTF8_STR = Charset.forName("UTF-8").toString();
  private static final String BIG5_STR = Charset.forName("BIG5").toString();
  private static final String UTF8_BOM = "\uFEFF";

  public final boolean isStringListContain(List<String> list, final String containPattern) {
    final Pattern pattern = Pattern.compile(containPattern);
    for (String str : list) {
      if (pattern.matcher(str).find()) {
        return true;
      }
    }
    return false;
  }

  public static final String stringListToString(List<String> list) {
    StringBuilder sb = new StringBuilder();
    for (String str : list) {
      sb.append(str + System.lineSeparator());
    }
    return sb.toString();
  }

  public static final List<String> stringToStringList(String string) {
    LinkedList<String> strList = new LinkedList<String>();
    for (String s : string.split("[\r\n\n" + tools.file.FileUtils.Line_SEP + "]")) {
      strList.add(s);
    }
    return strList;
  }

  public static final List<String> patternMatcher(String str, String patternStr) {
    ArrayList<String> returnList = new ArrayList<String>();
    Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(str);
    while (matcher.find()) {
      returnList.add(matcher.group());
    }
    return returnList;
  }

  public static String toValidFileName(String fileName) {
    return fileName.replaceAll("[\\p{Punct}&&[^_-]]|\\s+", "_");
  }

  public static String getUTF8String() {
    return UTF8_STR;
  }

  public static String getBIG5String() {
    return BIG5_STR;
  }

  public static PrintStream getPrinter(OutputStream out, String charsetEncoding) throws UnsupportedEncodingException {
    return new PrintStream(out, true, Charset.forName(charsetEncoding).toString());
  }

  public static PrintStream getPrinter() {
    try {
      return getPrinter(System.out, getUTF8String());
    } catch (UnsupportedEncodingException e) {
      //should not happen
      return null;
    }
  }

  public static void removeUTF8BOM(List<String> stringList) {
    stringList.set(0, removeUTF8BOM(stringList.get(0)));
  }

  public static String removeUTF8BOM(String firstStr) {
    if (firstStr.startsWith(UTF8_BOM)) {
      return firstStr.replaceFirst(UTF8_BOM, "");
    }
    return firstStr;
  }

  public static InputStream stringToInputStream(String string) {
    try {
      return stringToInputStream(string, tools.office.StringUtils.getUTF8String());
    } catch (UnsupportedEncodingException e) {
      //should not happen
    }
    return null;
  }

  public static InputStream stringToInputStream(String string, String charSet) throws UnsupportedEncodingException {
    return new ByteArrayInputStream(string.getBytes(charSet));
  }
}
