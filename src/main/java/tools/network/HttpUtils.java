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
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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

  public static final String getMethod(String targetURL) throws IOException {
    URL url = null;
    HttpURLConnection conn = null;
    try {
      url = new URL(urlDecode(targetURL));
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      return handleRequestResult(conn);
    } catch (IOException e) {
      throw e;
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  public static final String postMethod(String targetURL, Map<String, String> parameters) throws IOException {
    ArrayList<String> postData = new ArrayList<String>();
    Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();
    Entry<String, String> entry;
    String LsA;
    while (iterator.hasNext()) {
      entry = iterator.next();
      LsA = entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), tools.office.StringUtils.getUTF8String());
      postData.add(LsA);
    }
    URL url;
    String urlParameters = "";
    if (postData.size() == 0) {
      throw new NotFoundException("postData size = 0");
    }
    for (int i = 0; i < postData.size(); i++) {
      if (i == 0)
        urlParameters += postData.get(i);
      else
        urlParameters += "&" + postData.get(i);
    }
    DataOutputStream wr = null;
    HttpURLConnection conn = null;
    try {
      // Create connection
      url = new URL(urlDecode(targetURL));
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("Content-Length", "" +
          Integer.toString(urlParameters.getBytes().length));
      conn.setRequestProperty("Content-Language", "en-US");
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      // Send request
      wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.flush();
      wr.close();
      return handleRequestResult(conn);
    } catch (IOException e) {
      throw e;
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  private static final String handleRequestResult(HttpURLConnection conn) throws IOException {
    String returnStr = readInputStream(conn.getInputStream());
    if (!returnStr.isEmpty()) {
      return returnStr;
    } else if (conn.getResponseCode() == 301 || conn.getResponseCode() == 302) {
      return conn.getHeaderFields().toString() + conn.getResponseMessage();
    } else {
      return String.valueOf(conn.getResponseCode());
    }
  }

  public static final boolean isExistURL(String targetURL) {
    HttpURLConnection conn = null;
    try {
      URL url;
      url = new URL(urlDecode(targetURL));
      conn = (HttpURLConnection) url.openConnection();
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
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(URL);
    StringEntity params = new StringEntity(content);
    request.addHeader("content-type", "application/x-www-form-urlencoded");
    request.setEntity(params);
    return handlePostResult(httpClient.execute(request));
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
        rd.close();
      }
      if (ins != null) {
        ins.close();
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
    } catch (Exception e) {} finally {
      if (socket != null)
        try {
          socket.close();
        } catch (IOException e) {}
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
    } catch (IOException e) {} finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {}
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
