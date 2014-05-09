package tools.network.wireless;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import tools.system.SystemUtils;
import wait.Duration;
import wait.Sleeper;

public abstract class WirelessAction implements WirelessInterface {

  Sleeper sleeper = Sleeper.SYSTEM_SLEEPER;
  private static WirelessAction Instance;
  public static Duration sleeptime = new Duration(1500, MILLISECONDS);

  protected WirelessAction() {

  }

  public static final WirelessAction getInstance() {
    if (Instance == null) {
      if (SystemUtils.isMac()) {
        Instance = new WirelessConnectMac();
      } else if (SystemUtils.isWindows()) {
        Instance = new WirelessConnectWin();
      }
    }
    return Instance;
  }

  protected final static String getStringToHex(String strValue) {
    byte byteData[] = null;
    int intHex = 0;
    String strHex = "";
    String strReturn = "";
    try {
      byteData = strValue.getBytes("ISO8859-1");
      for (int intI = 0; intI < byteData.length; intI++) {
        intHex = (int) byteData[intI];
        if (intHex < 0)
          intHex += 256;
        if (intHex < 16)
          strHex += "0" + Integer.toHexString(intHex).toUpperCase();
        else
          strHex += Integer.toHexString(intHex).toUpperCase();
      }
      strReturn = strHex;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return strReturn;
  }

  protected final static CommandLine getConfigParser(String[] args) {
    try {
      Options options = new Options();
      options.addOption(wirelessOption.ssid.toString(), true, "SSID");
      options.addOption(wirelessOption.auth.toString(), true, "[open|shared|WPA|WPA2|WPAPSK|WPA2PSK]");
      options.addOption(wirelessOption.encrypt.toString(), true, "[none|WEP|TKIP|AES]");
      options.addOption(wirelessOption.key.toString(), true, "wireless key");
      CommandLineParser cli = new BasicParser();
      return cli.parse(options, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  protected final static String executeCMD(String cmd) {
    try {
      return SystemUtils.executeCMD(cmd);
    } catch (Exception e) {
      System.err.println(cmd);
      e.printStackTrace();
    }
    return null;
  }

  public boolean checkConnection() {
    try {
      return checkConnection("www.google.com");
    } catch (Exception e) {
      return false;
    }
  }

  public void sleep() {
    try {
      sleeper.sleep(sleeptime);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
