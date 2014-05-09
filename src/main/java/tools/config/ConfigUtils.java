package tools.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import exception.NotFoundException;
import exception.UtilityException;

@SuppressWarnings("serial")
public class ConfigUtils extends Properties {
  private final List<String> configContent;
  private final File configFile;

  private static final String configPattern = "\\s*=.*";

  public ConfigUtils(File configFile) {
    try {
      this.configFile = configFile;
      this.configContent = readConfigFile(this.configFile);
      loadToProperty();
    } catch (Exception e) {
      throw new UtilityException(e.getMessage() + e.getCause());
    }
  }

  public ConfigUtils(InputStream ins) {
    try {
      this.configFile = null;
      this.configContent = tools.file.FileUtils.fileRead(ins);
      loadToProperty();
    } catch (Exception e) {
      throw new UtilityException(e.getMessage() + e.getCause());
    }
  }

  private void loadToProperty() throws IOException {
    this.load(new StringReader(tools.office.StringUtils.stringListToString(getConfigContent())));
  }

  private List<String> readConfigFile(File config) {
    try {
      return tools.file.FileUtils.readFileData(config, true);
    } catch (IOException ie) {
      throw new NotFoundException("Can't read config file:" + config.getAbsolutePath());
    }
  }

  public final String parseConfig(String target) {
    for (String str : configContent) {
      if (!isComment(str) && str.matches(target.toString() + configPattern)) {
        return str.substring((str.indexOf('=') + 1));
      }
    }
    return null;
  }

  public final void setConfig(String target, String value) {
    String str;
    if (isComment(target)) {
      throw new exception.UtilityException("'//' or '#' prefix is comment.");
    }
    for (int i = 0, n = configContent.size(); i < n; i++) {
      str = configContent.get(i);
      if (str.matches(target.toString() + configPattern)) {
        str = target + "=" + value;
        configContent.remove(i);
        configContent.add(i, str);
        break;
      }
    }
  }

  public final void saveToFile() throws IOException {
    if (configFile != null) {
      tools.file.FileUtils.writeFileData(configFile, false, configContent, true);
    }
  }

  public final File getConfigFile() {
    return configFile;
  }

  public List<String> getConfigContent() {
    return configContent;
  }

  public Map<String, String> getConfigMap() {
    Map<String, String> map = new LinkedHashMap<String, String>();
    int index;
    for (String str : getConfigContent()) {
      index = str.indexOf("=");
      if (index > 0 && !isComment(str)) {
        map.put(str.substring(0, index), str.substring(index + 1));
      }

    }
    return map;
  }

  private boolean isComment(String str) {
    String s = str.trim();
    return s.startsWith("//") || s.startsWith("#");
  }
}
