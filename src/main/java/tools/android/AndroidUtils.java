package tools.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wait.Duration;
import wait.Sleeper;
import exception.NotFoundException;

public class AndroidUtils {

  private final String adbPath;
  private String deviceID;

  public AndroidUtils(String androidAdbPath, String deviceId) {
    adbPath = androidAdbPath;
    deviceID = deviceId;
  }

  public AndroidUtils(String androidAdbPath) {
    this(androidAdbPath, "");
  }

  public final void setDeviceID(String id) {
    deviceID = id;
  }

  public final String getDeviceModel(String deviceID) {
    try {
      setDeviceID(deviceID);
      String result = executeShellCMD("cat /system/build.prop", true, true).toString();
      Pattern pattern = Pattern.compile("(?<=(product.model=)).*");
      Matcher matcher = pattern.matcher(result);
      String returnStr = "";
      while (matcher.find()) {
        returnStr += matcher.group() + " ";
      }
      return returnStr.trim();
    } finally {
      setDeviceID(null);
    }
  }

  public final String getDeviceModel() {
    if (deviceID == null || deviceID.isEmpty()) {
      throw new NotFoundException("Device ID not set.");
    }
    return getDeviceModel(deviceID);
  }

  public final List<String> getUsableDevice() {
    List<String> deviceList = new ArrayList<String>();
    try {
      for (String device : toStringLineList(executeCMD(" devices", false, true).toString())) {
        if (!device.toLowerCase().contains("attached") && device.toLowerCase().trim().endsWith("device")) {
          deviceList.add(device.split("\\s.+")[0].trim());
        }
      }
      if (deviceList.size() == 0) {
        throw new Exception();
      }
      return deviceList;
    } catch (Exception e) {
      throw new NotFoundException("Unable to find device.");
    }

  }

  public final boolean installApkToDevice(String apkPath) throws IOException, InterruptedException {
    List<String> result = toStringLineList(executeCMD("install -r \"" + apkPath + "\"", true, true).toString());
    for (int i = result.size() - 1; i >= 0; i--) {
      if (result.get(i).toLowerCase().contains("fail")) {
        throw new IOException("Install fail.");
      }
    }
    return true;
  }

  public final void uninstallPackage(String packagePatternFilter) throws IOException, InterruptedException {
    List<String> result =
        toStringLineList(executeShellCMD("pm list packages", true, true).toString());
    String packageName;
    for (String str : result) {
      if (str.isEmpty()) {
        continue;
      }
      packageName = str.substring(str.indexOf(":") + 1);
      if (packageName.matches(packagePatternFilter)) {
        try {
          executeShellCMD("pm clear " + packageName, true, false);
          sleep(3000);
        } catch (Exception e) {
          e.printStackTrace();
        }
        executeShellCMD("pm uninstall " + packageName, true, true);
        sleep(3000);
      }
    }
  }

  public final Object executeShellCMD(String cmd, Boolean withDevice, Boolean... waitResponse) {
    return executeCMD("shell " + cmd, withDevice, waitResponse);
  }

  public final Object executeCMD(String cmd, Boolean withDevice, Boolean... waitResponse) {
    try {
      return tools.config.OptionUtils.getBooleanArrayOption(waitResponse, true) ?
          tools.system.SystemUtils.executeCMD(cmdBuilder(cmd, withDevice), false)
          :
          tools.system.SystemUtils.getRuntime().exec(cmdBuilder(cmd, withDevice));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private final String cmdBuilder(String cmd, boolean withDevice) {
    return adbPath + " " +
        ((withDevice && !deviceID.isEmpty()) ? ("-s " + deviceID) : "")
        + " " + cmd;
  }

  public static void sleep(long interval) {
    Duration duration = new Duration(interval, TimeUnit.MILLISECONDS);
    try {
      Sleeper.SYSTEM_SLEEPER.sleep(duration);
    } catch (InterruptedException ignored) {}
  }

  private static List<String> toStringLineList(String str) {
    return Arrays.asList(str.split("[\n" + tools.file.FileUtils.Line_SEP + "\r\n]"));
  }

  public static enum InstallTargetDevice {
    all, single
  }

}
