package tools.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import tools.system.SystemUtils;
import exception.NotFoundException;
import exception.UtilityException;

public class HttpUtils {

  public static final boolean setTrust() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
          }
      };
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      return true;
    } catch (KeyManagementException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static final boolean ping(String host) {
    try {
      String cmd = "ping -" + (SystemUtils.isWindows() ? "n" : "c") + " 1 " + host;
      Process process = SystemUtils.getRuntime().exec(cmd);
      process.waitFor();
      return process.exitValue() == 0;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static final String urlDecode(String url) throws UnsupportedEncodingException {
    return java.net.URLDecoder.decode(url, tools.office.StringUtils.getUTF8String());
  }

  public static final void getMethod(HttpRequestCapabilities httpRequestCapabilities) throws IOException {
    URL url = null;
    HttpURLConnection conn = null;
    try {
      url = new URL(urlDecode(httpRequestCapabilities.getUrl()));
      conn = (HttpURLConnection) url.openConnection();
      if (!httpRequestCapabilities.isKeepAlive()) {
        conn.setRequestProperty("connection", "close");
        conn.setRequestProperty("Connection", "close");
      }
      conn.setInstanceFollowRedirects(httpRequestCapabilities.getFollowRedirects());
      conn.setRequestMethod("GET");
      if (httpRequestCapabilities.getRequestProperties() == null ||
          httpRequestCapabilities.getRequestProperties().isEmpty()) {
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      } else {
        for (Entry<String, String> entry : httpRequestCapabilities.getRequestProperties().entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }
    } catch (IOException e) {
      throw e;
    } finally {
      handleRequestResult(conn, httpRequestCapabilities);
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public static final String getMethod(String targetURL) throws IOException {
    HttpRequestCapabilities httpRequestCapabilities = new HttpRequestCapabilities(targetURL);
    getMethod(httpRequestCapabilities);
    return handleRequestResultInString(httpRequestCapabilities);
  }

  public static final Map<String, String> queryStringToMap(String query) {
    Map<String, String> queryPairs = new LinkedHashMap<String, String>();
    String[] pairs = query.split("&");
    try {
      for (String pair : pairs) {
        int equalIndex = pair.indexOf("=");
        if (pair.contains("%") && !pair.contains(" ") && pair.matches(".*[%][0-9]+.*")) {
          queryPairs.put(
              URLDecoder.decode(pair.substring(0, equalIndex),
                  tools.office.StringUtils.getUTF8String()),
              URLDecoder.decode(pair.substring(equalIndex + 1),
                  tools.office.StringUtils.getUTF8String()));
        } else {
          queryPairs.put(pair.substring(0, equalIndex), pair.substring(equalIndex + 1));
        }

      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Parsing error:" + e.getMessage());
    }
    return queryPairs;
  }

  public static final String queryMapToString(Map<String, String> postDataMap) throws IOException {
    ArrayList<String> postData = new ArrayList<String>();
    Iterator<Entry<String, String>> iterator = postDataMap.entrySet().iterator();
    Entry<String, String> parameterEntry;
    String LsA;
    while (iterator.hasNext()) {
      parameterEntry = iterator.next();
      LsA = parameterEntry.getKey() + "=" + URLEncoder.encode(parameterEntry.getValue(), tools.office.StringUtils.getUTF8String());
      postData.add(LsA);
    }
    String urlParameters = "";
    if (postData.size() == 0) {
      throw new NotFoundException("postData size = 0");
    }
    for (int i = 0; i < postData.size(); i++) {
      if (i == 0) {
        urlParameters += postData.get(i);
      } else {
        urlParameters += "&" + postData.get(i);
      }
    }
    return urlParameters;
  }

  public static final void postMethod(HttpRequestCapabilities httpRequestCapabilities) throws IOException {
    String postContent;
    if (httpRequestCapabilities.getRequestPostParameters() != null &&
        !httpRequestCapabilities.getRequestPostParameters().isEmpty()) {
      postContent = queryMapToString(httpRequestCapabilities.getRequestPostParameters());
    } else if (!Strings.isNullOrEmpty(httpRequestCapabilities.getRequestPostStringContent())) {
      postContent = httpRequestCapabilities.getRequestPostStringContent();
    } else {
      throw new IllegalArgumentException("Empty post content.");
    }
    DataOutputStream wr = null;
    HttpURLConnection conn = null;
    URL url;
    try {
      // Create connection
      url = new URL(urlDecode(httpRequestCapabilities.getUrl()));
      conn = (HttpURLConnection) url.openConnection();
      if (!httpRequestCapabilities.isKeepAlive()) {
        conn.setRequestProperty("connection", "close");
        conn.setRequestProperty("Connection", "close");
      }
      conn.setInstanceFollowRedirects(httpRequestCapabilities.getFollowRedirects());
      conn.setRequestMethod("POST");
      if (httpRequestCapabilities.getRequestProperties() == null ||
          httpRequestCapabilities.getRequestProperties().isEmpty()) {
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", "" +
            Integer.toString(postContent.getBytes().length));
        conn.setRequestProperty("Content-Language", "en-US");
      } else {
        conn.setRequestProperty("Content-Length", Integer.toString(postContent.getBytes().length));
        for (Entry<String, String> entry : httpRequestCapabilities.getRequestProperties().entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      // Send request
      wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(postContent);
      wr.flush();
    } catch (IOException e) {
      throw e;
    } finally {
      handleRequestResult(conn, httpRequestCapabilities);
      if (wr != null) {
        try {
          wr.close();
        } catch (IOException ioe) {
        }
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public static final String postMethod(String targetURL, Map<String, String> parameters) throws IOException {
    HttpRequestCapabilities httpRequestCapabilities = new HttpRequestCapabilities(targetURL, parameters);
    postMethod(httpRequestCapabilities);
    return handleRequestResultInString(httpRequestCapabilities);
  }

  private static final void handleRequestResult(HttpURLConnection conn, HttpRequestCapabilities httpRequestCapabilities) throws IOException {
    InputStream is;
    if (conn.getResponseCode() >= 400) {
      is = conn.getErrorStream();
    } else {
      is = conn.getInputStream();
    }
    try {
      String returnStr = readInputStream(is);
      httpRequestCapabilities.setResponseContent(returnStr);
      httpRequestCapabilities.setResponseCode(conn.getResponseCode());
      httpRequestCapabilities.setResponseHeaderMap(conn.getHeaderFields());
      httpRequestCapabilities.setResponseMessage(conn.getResponseMessage());
      try {
        httpRequestCapabilities.setContentObject(conn.getContent());
      } catch (Exception e) {
        // this may null
      }
    } finally {
      if (is != null) {
        is.close();
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static final String handleRequestResultInString(HttpRequestCapabilities httpRequestCapabilities) throws IOException {
    if (!httpRequestCapabilities.getResponseContent().isEmpty()) {
      return httpRequestCapabilities.getResponseContent();
    } else if (httpRequestCapabilities.getResponseCode() == 301 || httpRequestCapabilities.getResponseCode() == 302) {
      return httpRequestCapabilities.getResponseHeaderMap().toString() + httpRequestCapabilities.getResponseMessage();
    } else {
      return String.valueOf(httpRequestCapabilities.getResponseCode());
    }
  }

  public static final boolean isExistURL(String targetURL) {
    HttpURLConnection conn = null;
    try {
      URL url;
      url = new URL(urlDecode(targetURL));
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestProperty("connection", "close");
      conn.setRequestProperty("Connection", "close");
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  public static final String postStringTo(String URL, String content) throws ClientProtocolException, IOException {
    HttpRequestCapabilities httpRequestCapabilities = new HttpRequestCapabilities(URL, content);
    httpRequestCapabilities.setRequestProperties(ImmutableMap.of("Content-Type", "application/x-www-form-urlencoded"));
    postMethod(httpRequestCapabilities);
    return handleRequestResultInString(httpRequestCapabilities);
  }

  //
  // public static final String postStringTo(String URL, String content) throws ClientProtocolException, IOException {
  // CloseableHttpClient httpClient = HttpClients.createDefault();
  // HttpPost request = new HttpPost(URL);
  // StringEntity params = new StringEntity(content);
  // request.addHeader();
  // request.setEntity(params);
  // return handlePostResult(httpClient.execute(request));
  // }

  public static final String postUploadFile(String url, File file, String userName, String password) throws IOException, AuthenticationException {
    CloseableHttpClient httpClient = HttpClients.createDefault();

    Credentials credentials =
        new UsernamePasswordCredentials(userName, password);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, credentials);
    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credsProvider);

    HttpPost post = new HttpPost(url);
    post.addHeader(new BasicScheme().authenticate(credentials, post, context));
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    builder.addPart("file", new FileBody(file));
    post.setEntity(builder.build());
    return handlePostResult(httpClient.execute(post, context));
  }

  public static final String postUploadFile(String url, File file) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost post = new HttpPost(url);
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    builder.addPart("file", new FileBody(file));
    post.setEntity(builder.build());
    return handlePostResult(httpClient.execute(post));
  }

  public static final String postUploadFile(URL url, File file) throws IOException {
    return postUploadFile(url.toExternalForm(), file);
  }

  private static final String handlePostResult(HttpResponse response) throws IllegalStateException, IOException {
    String result = readInputStream(response.getEntity().getContent());
    if (!result.isEmpty()) {
      return result;
    } else if (response.getStatusLine().getStatusCode() == 301 || response.getStatusLine().getStatusCode() == 302) {
      return response.toString();
    } else {
      return String.valueOf(response.getStatusLine().getStatusCode());
    }
  }

  private static final String readInputStream(InputStream ins) throws IOException {
    BufferedReader rd = null;
    try {
      rd = new BufferedReader(new InputStreamReader(ins));
      String line;
      StringBuffer response = new StringBuffer();
      while ((line = rd.readLine()) != null) {
        response.append(line);
        response.append(tools.file.FileUtils.Line_SEP);
      }
      if (response.length() != 0) {
        return response.substring(0, response.lastIndexOf(tools.file.FileUtils.Line_SEP));
      } else {
        return "";
      }
    } finally {
      if (rd != null) {
        try {
          rd.close();
        } catch (IOException ioe) {

        }
      }
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException ioe) {
        }
      }
    }
  }

  public static final List<String> nslookup(String host) {
    try {
      String result = SystemUtils.executeCMD("nslookup " + host);
      result = result.substring(result.indexOf(host) + host.length());
      List<String> returnList = new ArrayList<String>();
      Pattern ipPattern = Pattern.compile("(?<=[\\s\t\r\n])(\\d{1,3}[.]){3}\\d{1,3}(?=[\\s\t\r\n]||$)");
      Matcher ipMatcher = ipPattern.matcher(result);
      while (ipMatcher.find()) {
        returnList.add(ipMatcher.group());
      }
      return returnList;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static boolean isUrlReachable(URL url) {
    Socket socket = null;
    boolean reachable = false;
    try {
      socket = new Socket(url.getHost(), url.getPort());
      reachable = true;
    } catch (Exception e) {
    } finally {
      if (socket != null)
        try {
          socket.close();
        } catch (IOException e) {
        }
    }
    return reachable;
  }

  public static boolean isPortAvailable(int port) {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(port);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(port);
      ds.setReuseAddress(true);
      return true;
    } catch (IOException e) {
    } finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
        }
      }
    }
    return false;
  }

  public static int getUsablePort() {
    for (int i = 65534; i > 0; i--) {
      if (isPortAvailable(i)) {
        return i;
      }
    }
    throw new UtilityException("No usable port .");
  }
}
