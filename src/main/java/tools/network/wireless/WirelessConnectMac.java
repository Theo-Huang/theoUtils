package tools.network.wireless;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;

import tools.network.HttpUtils;
import wait.ExpectedCondition;
import wait.Wait;
import wait.WaitUntil;

class WirelessConnectMac extends WirelessAction {
  // mac
  private final static String MAC_WIRELESS_ON_CMD = "sudo networksetup -setairportpower en1 on";
  private final static String MAC_DISCONNECT_CMD = "sudo networksetup -setairportpower en1 off";
  private final static String MAC_REMOVE_PREFER_CMD = "sudo networksetup -removeallpreferredwirelessnetworks en1";
  private final static String MAC_GET_INTERFACE_CMD = "sudo networksetup -getairportnetwork en1";

  @Override
  public boolean checkConnection(String checkTarget) {
    boolean state = false;
    String result = executeCMD("ifconfig en1");
    Pattern ipPattern = Pattern.compile("(?<=[\\s\t\r\n])(\\d{1,3}[.]){3}\\d{1,3}(?=[\\s\t\r\n]||$)");
    Matcher ipMatcher = ipPattern.matcher(result);
    if (ipMatcher.find()) {
      state = ipMatcher.group().matches("(\\d{1,3}[.]){3}\\d{1,3}");
    }
    return HttpUtils.ping(checkTarget) && state;
  }

  @Override
  public String getWirelessMAC() {
    executeCMD(MAC_WIRELESS_ON_CMD);
    String result = executeCMD("ifconfig en1");
    Pattern ipPattern =
        Pattern.compile("(?<=[\\s\t\r\n])(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))(?=[\\s\t\r\n]||$)");
    Matcher ipMatcher = ipPattern.matcher(result);
    String mac = "";
    while (ipMatcher.find()) {
      mac = ipMatcher.group();
    }
    return mac;
  }

  @Override
  public boolean connect(String ssid, String... option) {
    /**
     * 
     * sudo networksetup -setairportnetwork $INTERFACE $SSID $PASSWORD -getairportnetwork
     * hardwareport -removepreferredwirelessnetwork hardwareport network -setairportpower
     * hardwareport on
     * 
     */
    executeCMD(MAC_WIRELESS_ON_CMD);
    CommandLine cmd = getConfigParser(option);
    String key = "";
    if (cmd.hasOption(wirelessOption.key.toString())) {
      key = cmd.getOptionValue(wirelessOption.key.toString());
    } else if (option.length == 1) {
      key = option[0];
    }
    executeCMD(macConnectCmdBuilder(ssid, key));
    ExpectedCondition<Boolean> expect =
        new ExpectedCondition<Boolean>() {
          String response;

          @Override
          public Boolean apply(Object ssid) {
            response = executeCMD(MAC_GET_INTERFACE_CMD);
            if (response == null) {
              return false;
            }
            return response.contains((String) ssid) && checkConnection();
          }
        };
    Wait<Object> wait = new WaitUntil(ssid, 60);
    return wait.until(expect);
  }

  @Override
  public void disconnect(boolean... deleteTemp) {
    executeCMD(MAC_REMOVE_PREFER_CMD);
    sleep();
    executeCMD(MAC_DISCONNECT_CMD);
    sleep();
  }

  private String macConnectCmdBuilder(String ssid, String wirelessKey) {
    return "sudo networksetup -setairportnetwork en1 " + ssid + " " + wirelessKey;
  }

  @Override
  public void cleanTempFile() {}

}
