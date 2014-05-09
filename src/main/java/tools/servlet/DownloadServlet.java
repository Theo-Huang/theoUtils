package tools.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;

@SuppressWarnings("serial")
public class DownloadServlet extends javax.servlet.http.HttpServlet {
  private static boolean CheckFlag = true;
  private static final int downloadExpireTime = 60 * 1000 * 10;// 10minutes
  private static final Object syncObj = new Object();
  private static final Map<String, Entry<String, Long>> sessionFileMap =
      new ConcurrentHashMap<String, Entry<String, Long>>();
  public static String URLPattern;
  public static final String DownloadSessionStr = "DownloadSession";
  private static String DOWNLOAD_PATTERN = "/Download";

  public DownloadServlet() {}

  public DownloadServlet(String downloadServletPattern) {
    DOWNLOAD_PATTERN = downloadServletPattern;
  }

  public static final String getServletPathPattern() {
    return DOWNLOAD_PATTERN;
  }

  public static synchronized void setMap(String sessionIDandFileNameHash, String filePath) {
    synchronized (syncObj) {
      sessionFileMap.put(
          sessionIDandFileNameHash,
          Maps.immutableEntry(filePath, System.currentTimeMillis()));
    }
  }

  @Override
  public void init() {
    try {
      super.init();
    } catch (javax.servlet.ServletException e) {}
    Thread checkThread = new Thread() {
      @Override
      public void run() {
        Entry<String, Entry<String, Long>> entry;
        Iterator<Entry<String, Entry<String, Long>>> it;
        long nowTime;
        while (CheckFlag) {
          nowTime = System.currentTimeMillis();
          it = sessionFileMap.entrySet().iterator();
          while (it.hasNext()) {
            entry = it.next();
            if (nowTime - entry.getValue().getValue() >= downloadExpireTime) {
              synchronized (syncObj) {
                sessionFileMap.remove(entry.getKey());
              }
            }
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // skip
          }
        }
      }

    };
    checkThread.start();
  }

  @Override
  public void destroy() {
    CheckFlag = false;
  }

  protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse response)
      throws javax.servlet.ServletException, IOException {
    req.setCharacterEncoding(tools.office.StringUtils.getUTF8String());
    response.setCharacterEncoding(tools.office.StringUtils.getUTF8String());
    String session = req.getParameter(DownloadSessionStr);
    Entry<String, Long> entry = sessionFileMap.get(session);
    if (entry == null) {
      response.sendError(400, "Invalid session.");
      return;
    }
    File file = new File(entry.getKey());
    try {
      ServletUtils.buildSendFileResponse(response, file);
    } catch (Exception e) {
      response.sendError(500, e.getMessage());
    }

  }

  public static final String buildDownloadPath(javax.servlet.http.HttpServletRequest req, File file) {
    String hash = req.getSession().getId() + file.getName().hashCode();
    setMap(
        hash,
        file.getAbsolutePath());
    return req.getContextPath()
        + DOWNLOAD_PATTERN
        + "?" + DownloadSessionStr + "=" + hash;
  }

}
