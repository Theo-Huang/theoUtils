package tools.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import exception.UtilityException;

public class SystemUtils {
  private static final String OS = System.getProperty("os.name").toLowerCase();
  private static final Runtime rt = Runtime.getRuntime();

  public static final void setSystemEncodingToUTF8()
      throws SecurityException,
      NoSuchFieldException,
      IllegalArgumentException,
      IllegalAccessException {
    setSystemEncodingToCharSet(tools.office.StringUtils.getUTF8String());
  }

  public static final void setSystemEncodingToCharSet(String CharSet)
      throws SecurityException,
      NoSuchFieldException,
      IllegalArgumentException,
      IllegalAccessException {
    System.setProperty("file.encoding", Charset.forName(CharSet).toString());
    Field charset = Charset.class.getDeclaredField("defaultCharset");
    charset.setAccessible(true);
    charset.set(null, null);
  }

  public static final boolean hasProcess(final String processName) throws IOException, InterruptedException {
    List<String> returnStrings;
    if (isWindows()) {
      returnStrings = Arrays.asList(executeCMD("tasklist", false).split("[\n\r\n]"));
      for (String str : returnStrings) {
        if (str.split("\\s+")[0].trim().equals(processName)) {
          return true;
        }
      }
    } else if (isMac() || isUnix()) {
      returnStrings = Arrays.asList(executeCMD("sudo ps aux").split("[\n\r\n]"));
      int startIndex = 10;
      String[] strArrays;
      for (String str : returnStrings) {
        strArrays = str.split("\\s+");
        str = str.substring(str.indexOf(strArrays[startIndex]), str.length());
        if (str.contains(processName)) {
          return true;
        }
      }
    }
    return false;
  }

  public static final void processKiller(final String processName) throws IOException, InterruptedException {
    String pidPattern = "\\d+";
    if (isWindows()) {
      if (processName.matches(pidPattern)) {
        rt.exec("taskkill /F /PID " + processName);
      } else {
        rt.exec("taskkill /F /IM " + processName);
      }
    } else if (isMac() || isUnix()) {
      if (processName.matches(pidPattern)) {
        rt.exec("sudo kill " + processName);
      } else {
        String pid;
        String[] strArrays;
        int startIndex = 10;
        for (String str : executeCMD("sudo ps aux").split("[\n\r\n]")) {
          strArrays = str.split("\\s+");
          str = str.substring(str.indexOf(strArrays[startIndex]), str.length());
          if (str.contains(processName)) {
            pid = strArrays[1].trim();
            if (pid.matches(pidPattern)) {
              processKiller(pid);
            }
          }
        }
      }
    }
  }

  @Deprecated
  public static final boolean isSupportedPlatform() {
    return isWindows() || isMac();
  }

  public static final String getPlatform() {
    return OS;
  }

  public static boolean isWindows() {
    return (OS.toLowerCase().contains("win"));
  }

  public static boolean isUnix() {
    return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
  }

  public static boolean isMac() {
    return (OS.toLowerCase().contains("mac"));
  }

  public static final String executeCMD(String cmd, Boolean... print) throws IOException, InterruptedException {
    return readProcessInputStream(rt.exec(cmd), print);
  }

  public static final String executeCMD(String[] cmds, Boolean... print) throws IOException, InterruptedException {
    return readProcessInputStream(new ProcessBuilder(cmds).start(), print);
  }

  public static final void executeCMDwithoutResult(String cmd) throws IOException {
    rt.exec(cmd);
  }

  public static final void executeCMDwithoutResult(String[] cmds) throws IOException {
    new ProcessBuilder(cmds).start();
  }

  public static final void executeCMDInPathWithoutResult(String cmd, File path) throws IOException {
    rt.exec(cmd, null, path);
  }

  public static final void executeCMDInPathWithoutResult(String[] cmds, File path) throws IOException {
    new ProcessBuilder(cmds).directory(path).start();
  }

  public static final String executeCMDinFilePath(String cmd, File path, Boolean... print) throws IOException, InterruptedException {
    return readProcessInputStream(getRuntime().exec(cmd, null, path), print);
  }

  public static final String executeCMDinFilePath(String[] cmds, File path, Boolean... print) throws IOException, InterruptedException {
    return readProcessInputStream(new ProcessBuilder(cmds).directory(path).start(), print);
  }

  public static final String readProcessInputStream(Process ps, Boolean... print) throws IOException, InterruptedException {
    InputStream is = ps.getInputStream();
    InputStream isErr = ps.getErrorStream();
    boolean isPrint = tools.config.OptionUtils.getBooleanArrayOption(print, false);
    String returnStr = "";
    String decode = isWindows() ? "ms950" : "CP857";
    InputStreamReader isr = new InputStreamReader(is, decode);
    InputStreamReader isrErr = new InputStreamReader(isErr, decode);
    BufferedReader br = new BufferedReader(isr);
    BufferedReader brErr = new BufferedReader(isrErr);
    String line;
    String lineErr;
    while ((line = br.readLine()) != null) {
      returnStr += line + tools.file.FileUtils.Line_SEP;
      if (isPrint) {
        System.out.println(line);
      }
    }

    while ((lineErr = brErr.readLine()) != null) {
      returnStr += lineErr + tools.file.FileUtils.Line_SEP;
      if (isPrint) {
        System.err.println(lineErr);
      }
    }
    ps.waitFor();
    return returnStr;
  }

  public static final Runtime getRuntime() {
    return rt;
  }

  public static final void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new UtilityException(e.getMessage());
    }
  }
}
