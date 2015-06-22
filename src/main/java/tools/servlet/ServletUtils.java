package tools.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class ServletUtils {

  private static final int maxFileSize = 500 * 1024 * 1024;// 500mb
  private static final int maxMemSize = 1024 * 1024;

  public static final File receiveFile(HttpServletRequest req, File tempRepo) throws Exception {
    DiskFileItemFactory factory = new DiskFileItemFactory();
    // maximum size that will be stored in memory
    factory.setSizeThreshold(maxMemSize);
    // Location to save data that is larger than maxMemSize.
    factory.setRepository(tempRepo);
    // Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload(factory);
    // maximum file size to be uploaded.
    upload.setSizeMax(maxFileSize);
    File file = null;
    // Parse the request to get file items.
    List<FileItem> fileItems = upload.parseRequest(req);
    // Process the uploaded file items
    Iterator<FileItem> i = fileItems.iterator();
    while (i.hasNext()) {
      FileItem fi = (FileItem) i.next();
      if (!fi.isFormField()) {
        String fileName = fi.getName();
        // Write the file
        if (fileName.lastIndexOf(tools.file.FileUtils.File_SEP) >= 0) {
          file = new File(tempRepo.getCanonicalPath() +
              fileName.substring(fileName.lastIndexOf(tools.file.FileUtils.File_SEP)));
        } else {
          file = new File(tempRepo.getCanonicalPath() + tools.file.FileUtils.File_SEP +
              fileName.substring(fileName.lastIndexOf(tools.file.FileUtils.File_SEP) + 1));
        }
        if (file.isDirectory()) {
          throw new IOException("Invalid file path. Is directory");
        }
        fi.write(file);
      }
    }
    return file;
  }

  public static final void buildSendFileResponse(HttpServletResponse response, InputStream inputStream, String fileName) throws Exception {
    ServletOutputStream stream = null;
    BufferedInputStream buf = null;
    try {
      stream = response.getOutputStream();
      // set response headers
      response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

      buf = new BufferedInputStream(inputStream);
      int readBytes = 0;
      ArrayList<Integer> readBytesArray = new ArrayList<Integer>();
      // count byte;
      int index = 0;
      while ((readBytes = buf.read()) != -1) {
        readBytesArray.add(index++, readBytes);
      }
      response.setContentLength(index);
      for (int i : readBytesArray) {
        stream.write(i);
      }

    } catch (IOException ioe) {
      response.sendError(500, ioe.getMessage());
      return;
    } finally {
      if (stream != null) {
        stream.close();
      }
      if (buf != null) {
        buf.close();
      }
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  public static final void buildSendFileResponse(HttpServletResponse response, File sendFile) throws Exception {
    ServletOutputStream stream = null;
    BufferedInputStream buf = null;
    try {
      stream = response.getOutputStream();
      // set response headers
      response.addHeader("Content-Disposition", "attachment; filename=" + sendFile.getName());
      response.setContentLength((int) sendFile.length());
      FileInputStream input = new FileInputStream(sendFile);
      buf = new BufferedInputStream(input);
      int readBytes = 0;
      // read from the file; write to the ServletOutputStream
      while ((readBytes = buf.read()) != -1)
        stream.write(readBytes);
    } catch (IOException ioe) {
      response.sendError(500, ioe.getMessage());
      return;
    } finally {
      if (stream != null)
        stream.close();
      if (buf != null)
        buf.close();
    }
  }

  public static final String getRequestIp(HttpServletRequest request) {
    String ipAddress = request.getHeader("Remote_Addr");
    if (ipAddress == null || ipAddress.trim().isEmpty()) {
      ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
      if (ipAddress == null || ipAddress.trim().isEmpty()) {
        ipAddress = request.getRemoteAddr();
      }
    }
    return ipAddress;
  }
}
