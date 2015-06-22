package tools.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileExistsException;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbSession;

public class SmbUtils {
  private static final String File_SEP = FileUtils.File_SEP;

  private static final List<copyFileHandler> depthCopyFromSmbFile(
      final SmbFile srcFile,
      final File dtFile,
      final NtlmPasswordAuthentication auth,
      final List<copyFileHandler> downloadList,
      boolean overWrite) throws IOException {
    String fileName;
    File toCreateFile;
    for (final SmbFile file : srcFile.listFiles()) {
      fileName = file.getName();
      toCreateFile = new File(dtFile.getAbsolutePath() + File_SEP + fileName);
      if (!overWrite && toCreateFile.exists()) {
        throw new FileExistsException("File exist. " + toCreateFile.getAbsolutePath());
      }
      if (file.isFile()) {
        if (!toCreateFile.exists()) {
          toCreateFile.createNewFile();
        }
        downloadList.add(
            new copyFileHandler(
                new SmbFileInputStream(file),
                new FileOutputStream(dtFile.getAbsolutePath() + File_SEP + fileName)));
      } else {
        if (!toCreateFile.exists()) {
          toCreateFile.mkdir();
        }
        depthCopyFromSmbFile(
            file, toCreateFile, auth, downloadList, overWrite);
      }
    }
    return downloadList;
  }

  private static final List<copyFileHandler> depthCopyToSmbFile(
      final File srcFile,
      final SmbFile dtFile,
      final NtlmPasswordAuthentication auth,
      final List<copyFileHandler> uploadList,
      boolean overWrite) throws IOException {
    String fileName;
    SmbFile toCreateFile;
    for (final File file : srcFile.listFiles()) {
      fileName = file.getName();
      toCreateFile = new SmbFile(dtFile.getCanonicalPath() + File_SEP + fileName, auth);
      if (file.isFile()) {
        if (!overWrite && toCreateFile.exists()) {
          throw new FileExistsException("File exist. " + toCreateFile.getCanonicalPath());
        } else if (!toCreateFile.exists()) {
          toCreateFile.createNewFile();
        }
        uploadList.add(
            new copyFileHandler(
                new FileInputStream(file),
                new SmbFileOutputStream(toCreateFile)));
      } else {
        if (!toCreateFile.exists()) {
          toCreateFile.mkdir();
        }
        depthCopyToSmbFile(
            file, toCreateFile, auth, uploadList, overWrite);
      }
    }
    return uploadList;
  }

  public static final boolean testAuthToPath(final String ipOrDomain, final String path, NtlmPasswordAuthentication auth)
      throws MalformedURLException, SmbException {
    try {
      String returnStr = resolvePath(ipOrDomain, path, true);
      SmbFile smbFile = new SmbFile(returnStr, auth);
      return smbFile.exists();
    } catch (SmbAuthException e) {
      return false;
    }
  }

  public static final SmbFile toValidSmbFile(final String ipOrDomain, final String path, NtlmPasswordAuthentication auth) throws IOException {
    // String returnStr = str;
    // returnStr = returnStr.replace("\\", "/");
    // returnStr = returnStr.replaceAll("^smb:", "");
    // returnStr = returnStr.replaceAll("^[/]*", "");
    String returnStr = resolvePath(ipOrDomain, path, true);
    SmbFile smbFile = new SmbFile(returnStr, auth);
    if (smbFile.exists()) {
      if (smbFile.isDirectory() && !returnStr.endsWith("/")) {
        smbFile = new SmbFile(returnStr + "/", auth);
      } else if (smbFile.isFile() && returnStr.endsWith("/")) {
        returnStr = returnStr.substring(0, returnStr.lastIndexOf("/"));
        smbFile = new SmbFile(returnStr, auth);
      }

    }
    return smbFile;
  }

  public static final boolean copyFileToNetDrive(
      final String ipOrDomain,
      final NtlmPasswordAuthentication auth,
      final String srFile,
      final String dtFile,
      final boolean overWrite) throws IOException, InterruptedException {
    final File srcFile = new File(srFile);
    final SmbFile destFile = toValidSmbFile(ipOrDomain, dtFile, auth);
    CopyAction action = new CopyAction() {

      @Override
      public boolean isDestExist() throws IOException {
        return destFile.exists();
      }

      @Override
      public boolean isSourceIsDir() throws IOException {
        return srcFile.isDirectory();
      }

      @Override
      public List<copyFileHandler> folderCopyThread() throws IOException {
        List<copyFileHandler> threadList = new ArrayList<copyFileHandler>();
        return depthCopyToSmbFile(
            srcFile, destFile, auth, threadList, overWrite);
      }

      @Override
      public copyFileHandler fileCopyThread() throws IOException {
        String srcFileName = srcFile.getName();
        SmbFile finalDestFile = destFile;
        if (!finalDestFile.getName().equals(srcFileName)) {
          finalDestFile = new SmbFile(finalDestFile.getCanonicalPath() + File_SEP + srcFileName, auth);
        }
        SmbFile smbParent = new SmbFile(finalDestFile.getParent(), auth);
        if (!smbParent.exists()) {
          smbParent.mkdirs();
        }
        return new copyFileHandler(new FileInputStream(srcFile), new SmbFileOutputStream(finalDestFile));
      }
    };
    return handleSmbFileCopy(auth, action, overWrite);
  }

  public static final boolean copyFileFromNetDrive(
      final String ipOrDomain,
      final NtlmPasswordAuthentication auth,
      final String srFile,
      final String dtFile,
      final boolean overWrite) throws IOException, InterruptedException {
    final SmbFile srcFile = toValidSmbFile(ipOrDomain, srFile, auth);
    final File destFile = new File(dtFile);
    CopyAction action = new CopyAction() {
      @Override
      public boolean isDestExist() {
        return destFile.exists();
      }

      @Override
      public boolean isSourceIsDir() throws SmbException {
        return srcFile.isDirectory();
      }

      @Override
      public List<copyFileHandler> folderCopyThread() throws IOException {
        List<copyFileHandler> threadList = new ArrayList<copyFileHandler>();
        return depthCopyFromSmbFile(
            srcFile, destFile, auth, threadList, overWrite);
      }

      @Override
      public copyFileHandler fileCopyThread() throws IOException {
        String srcFileName = srcFile.getName();
        File finalDestFile = destFile;
        if (!finalDestFile.getName().equals(srcFileName)) {
          finalDestFile = new File(finalDestFile.getCanonicalPath() + File_SEP + srcFileName);
        }

        if (!finalDestFile.exists()) {
          finalDestFile.getParentFile().mkdirs();
        }
        return new copyFileHandler(new SmbFileInputStream(srcFile), new FileOutputStream(finalDestFile));
      }

    };
    return handleSmbFileCopy(auth, action, overWrite);
  }

  private static final boolean handleSmbFileCopy(
      NtlmPasswordAuthentication auth,
      CopyAction action,
      boolean overWrite)
      throws IOException, InterruptedException {
    if (!overWrite && action.isDestExist()) {
      throw new FileExistsException("Dest File already exists.");
    }
    if (action.isSourceIsDir()) {
      List<copyFileHandler> threadList = action.folderCopyThread();
      boolean isSuccess = true;
      for (copyFileHandler thread : threadList) {
        thread.start();
      }
      for (copyFileHandler thread : threadList) {
        thread.join();
        if (!thread.isSuccess()) {
          isSuccess = false;
        }
      }
      return isSuccess;
    } else {
      copyFileHandler copyThread = action.fileCopyThread();
      copyThread.start();
      copyThread.join();
      return copyThread.isSuccess();
    }

  }

  public static final NtlmPasswordAuthentication smbLogin(String domainOrIpAddress, String username, String password)
      throws UnknownHostException, SmbException {
    NtlmPasswordAuthentication auth;
    if (Strings.isNullOrEmpty(username)) {
      auth = NtlmPasswordAuthentication.ANONYMOUS;
    } else {
      auth = new NtlmPasswordAuthentication(null, username, password);
    }
    UniAddress domain = UniAddress.getByName(domainOrIpAddress);
    SmbSession.logon(domain, auth);
    return auth;
  }

  public static final List<String> listSmbFileString(NtlmPasswordAuthentication auth, String domainOrIpAddress, String path)
      throws MalformedURLException, SmbException {
    List<String> fileList;
    // String folderPath = "smb://" + domainOrIpAddress + "/" + (Strings.isNullOrEmpty(path) ? "" : path + "/");
    String folderPath = resolvePath(domainOrIpAddress, path, true);
    SmbFile smbFile = auth == null ? new SmbFile(folderPath) : new SmbFile(folderPath, auth);
    fileList = Lists.newArrayList(smbFile.list());
    return fileList;
  }

  private static final String resolvePath(String domainOrIpAddress, String path, boolean withProtocol) {
    path = path.replace("\\", "/");
    path = path.replaceAll("(?i)^smb:", "");
    path = path.replaceAll("^[/]*", "");
    if (!Strings.isNullOrEmpty(domainOrIpAddress)) {
      path = path.replaceAll("\\Q" + domainOrIpAddress + "\\E/+", "");
    }
    if (withProtocol && !Strings.isNullOrEmpty(domainOrIpAddress)) {
      path = "smb://" + domainOrIpAddress + "/" + (Strings.isNullOrEmpty(path) ? "" : path + "/");
    }
    return path;
  }

  public static final List<SmbFile> listSmbFile(NtlmPasswordAuthentication auth, String domainOrIpAddress, String path) {
    List<SmbFile> fileList;
    try {
      // String folderPath = "smb://" + domainOrIpAddress + "/" + (Strings.isNullOrEmpty(path) ? "" : path + "/");
      String folderPath = resolvePath(domainOrIpAddress, path, true);
      SmbFile smbFile = auth == null ? new SmbFile(folderPath) : new SmbFile(folderPath, auth);
      fileList = Lists.newArrayList(smbFile.listFiles());
    } catch (Exception e) {
      e.printStackTrace();
      fileList = Lists.newArrayList();
    }
    return fileList;
  }

  public static SmbUrlInfo parseSmbUrl(String url) {
    return SmbUrlInfo.parse(url);
  }


  public static void main(String[] args) {
    try {
      NtlmPasswordAuthentication auth = smbLogin("10.201.236.31", "administrator", "thirdbrigade");
      for (String file : listSmbFileString(auth, "10.201.236.31", "trunk/build/DSM")) {
        System.out.println(file);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}


interface CopyAction {
  boolean isDestExist() throws IOException;

  boolean isSourceIsDir() throws IOException;

  List<copyFileHandler> folderCopyThread() throws IOException;

  copyFileHandler fileCopyThread() throws IOException;
}
