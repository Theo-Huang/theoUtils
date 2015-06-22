package tools.network;

import java.io.Serializable;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class HttpRequestCapabilities implements Serializable {

  private static final long serialVersionUID = 1L;

  private String url;
  private String responseContent;
  private String responseMessage;
  private Map<String, List<String>> responseHeaderMap;
  private Map<String, String> requestPostParameters;
  private Map<String, String> requestProperties;
  private String requestPostStringContent;
  private int responseCode = -1;
  private Object contentObject;
  private boolean followRedirects = false;
  private boolean keepAlive = true;


  public HttpRequestCapabilities() {}

  public HttpRequestCapabilities(String url) {
    this.url = url;
  }

  public HttpRequestCapabilities(String url, String postStringContent) {
    this.url = url;
    this.setRequestPostStringContent(postStringContent);
  }

  public HttpRequestCapabilities(String url, Map<String, String> postParameters) {
    this.url = url;
    this.setRequestPostParameters(postParameters);
  }

  public String getUrl() {
    return url;
  }

  public String getResponseContent() {
    return responseContent;
  }

  public void setResponseContent(String responseContent) {
    this.responseContent = responseContent;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  public Map<String, List<String>> getResponseHeaderMap() {
    return responseHeaderMap;
  }

  public void setResponseHeaderMap(Map<String, List<String>> responseHeaderMap) {
    this.responseHeaderMap = responseHeaderMap;
  }

  public Map<String, String> getRequestPostParameters() {
    return requestPostParameters;
  }

  public String getRequestPostStringContent() {
    return requestPostStringContent;
  }

  public void setRequestPostStringContent(String content) {
    this.requestPostStringContent = content;
  }

  public void setRequestPostParameters(Map<String, String> postParameters) {
    this.requestPostParameters = postParameters;
  }

  public void setRequestPostParameters(String query) {
    Map<String, String> queryPairs = new LinkedHashMap<String, String>();
    String[] pairs = query.split("&");
    try {
      for (String pair : pairs) {
        int equalIndex = pair.indexOf("=");
        queryPairs.put(
            URLDecoder.decode(pair.substring(0, equalIndex),
                tools.office.StringUtils.getUTF8String()),
            URLDecoder.decode(pair.substring(equalIndex + 1),
                tools.office.StringUtils.getUTF8String()));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Parsing error:" + e.getMessage());
    }
    this.requestPostParameters = queryPairs;
  }

  public Map<String, String> getRequestProperties() {
    return requestProperties;
  }

  public void setRequestProperties(Map<String, String> requestProperties) {
    this.requestProperties = requestProperties;
  }

  public void addRequestProperties(String key, String value) {
    if (getRequestProperties() == null) {
      setRequestProperties(Maps.<String, String>newLinkedHashMap());
    }
    getRequestProperties().put(key, value);
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public void setContentObject(Object o) {
    this.contentObject = o;
  }

  public Object getContentObject() {
    return this.contentObject;
  }

  public void setFollowRedirects(boolean follow) {
    this.followRedirects = follow;
  }

  public boolean getFollowRedirects() {
    return this.followRedirects;
  }

  public boolean isKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }
}
