package tools.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exception.UtilityException;

public class SmbUrlInfo {
  private String protocol;
  private String account;
  private String password;
  private String url;
  private String host;
  private String path;

  private SmbUrlInfo() {}

  public String getProtocol() {
    return protocol;
  }

  public String getAccount() {
    return account;
  }


  public String getPassword() {
    return password;
  }

  public String getUrl() {
    return url;
  }

  public String getHost() {
    return host;
  }


  public String getPath() {
    return path;
  }

  static final SmbUrlInfo parse(String url) {
    UtilityException ue = new UtilityException("Fail to resovle smb url:" + url);
    String protocol = "smb";
    url = url.replace("\\", "/");
    if (!url.toLowerCase().startsWith(protocol + "://")) {
      url = url.replaceAll("(?i)^" + protocol + ":", "");
      url = url.replaceAll("^[/]*", "");
      url = protocol + "://" + url;
    }
    SmbUrlInfo si = new SmbUrlInfo();
    si.url = url;
    si.protocol = protocol;
    Pattern accountPasswordPattern = Pattern.compile("(?<=([:][/]{2})).*?(?=(@+))");
    Matcher accountPasswordMathcer = accountPasswordPattern.matcher(url);
    if (accountPasswordMathcer.find()) {
      String[] result = accountPasswordMathcer.group().split("[:]");
      si.account = result[0];
      if (result.length > 1) {
        si.password = result[1];
      }
    }

    Pattern hostPattern = Pattern.compile("(?<=(^.+[@]|[:][/]{2})).*?(?=([/]))");
    Matcher hostMathcer = hostPattern.matcher(url);
    if (hostMathcer.find()) {
      si.host = hostMathcer.group();
      if (si.account != null) {
        si.host = si.host.replace(si.account, "");
      }
      if (si.password != null) {
        si.host = si.host.replace(si.password, "");
      }
      si.host = si.host.replaceAll("[:@]", "");
    } else {
      throw ue;
    }
    Pattern pathPattern = Pattern.compile("(?<=(\\Q" + si.host + "\\E)).*(?=($))");
    Matcher pathMathcer = pathPattern.matcher(url);
    if (pathMathcer.find()) {
      si.path = pathMathcer.group();
    } else {
      si.path = "/";
    }
    if (si.path.endsWith("//")) {
      si.path = si.path.substring(0, si.path.lastIndexOf("/"));
    }
    return si;
  }
}
