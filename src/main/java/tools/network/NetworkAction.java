package tools.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkAction {

  private static NetworkAction Instance;
  private static String interfaceName;
  private static final long commandInterval = 100;

  private NetworkAction() {
    interfaceName = getInterface().get(0);
    Instance = this;
  }

  private static final void sleep() {
    try {
      Thread.sleep(commandInterval);
    } catch (InterruptedException e) {}
  }

  public static final NetworkAction getInstance() {
    if (Instance == null) {
      new NetworkAction();
    }
    return Instance;
  }

  public static final List<String> getInterface() {
    try {
      List<String> interFaceList = new LinkedList<String>();
      Pattern pattern = Pattern.compile("(?<=(\")).*?(?=(\"))");
      Matcher matcher = pattern.matcher(executeNetshInterfaceCMD("ipv4 show config"));
      while (matcher.find()) {
        interFaceList.add(matcher.group());
      }
      return interFaceList;
    } catch (Exception e) {
      return null;
    }
  }

  private static final String executeNetshInterfaceCMD(String cmd) throws IOException, InterruptedException {
    sleep();
    return tools.system.SystemUtils.executeCMD("netsh interface " + cmd, false);
  }

  public final boolean enable() {
    try {
      return executeNetshInterfaceCMD("set interface \"" + interfaceName + "\" enable").trim().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  public final boolean disable() {
    try {
      return executeNetshInterfaceCMD("set interface \"" + interfaceName + "\" disable").trim().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  public final boolean setToStaticIP(String ip, String mask, String gateway, String DNS) {
    //    netsh interface ip set address   "區域連線" static 10.10.1.168 255.255.255.0 10.10.1.254 1
    //    netsh interface ip set dnsserver "區域連線" static 10.10.1.254 primary

    try {
      return executeNetshInterfaceCMD("ip set address \"" + interfaceName + "\" static " + ip + " " + mask + " " + gateway + " 1").trim().isEmpty()
          &&
          executeNetshInterfaceCMD("ip set dns \"" + interfaceName + "\" static " + DNS + " primary").trim().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  public final boolean setToDHCP() {
    try {
      return executeNetshInterfaceCMD("ip set dnsserver \"" + interfaceName + "\" source=dhcp").trim().isEmpty()
          &&
          executeNetshInterfaceCMD("ip set address \"" + interfaceName + "\" source=dhcp").trim().toLowerCase().startsWith("dhcp");
    } catch (Exception e) {
      return false;
    }
  }

  public final boolean disablePPPoE() {
    try {
      return !tools.system.SystemUtils.executeCMD("rasdial /DISCONNECT", false).contains("netcfg.chm");
    } catch (Exception e) {
      return false;
    }
  }

  public final boolean setToPPPoE(String profileName, String name, String pwd) {
    try {
      return !tools.system.SystemUtils.executeCMD("rasdial \"" + profileName + "\" " + name + " " + pwd, false).contains("netcfg.chm");
    } catch (Exception e) {
      return false;
    }
  }
}
