package tools.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileExistsException;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
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

  public static final SmbFile toValidSmbFile(final String str, NtlmPasswordAuthentication auth) throws IOException {
    String returnStr = str;
    returnStr = returnStr.replace("\\", "/");
    returnStr = returnStr.replaceAll("^smb:", "");
    returnStr = returnStr.replaceAll("^[/]*", "");
    returnStr = "smb://" + returnStr;
    SmbFile smbFile = new SmbFile(returnStr, auth);
    if (smbFile.exists()) {
      if (smbFile.isDirectory() && !returnStr.endsWith("/")) {
        smbFile = new SmbFile(returnStr + "/", auth);
      }
    }
    return smbFile;
  }

  public static final boolean copyFileToNetDrive(
      final String account,
      final String pwd,
      final String srFile,
      final String dtFile,
      final boolean overWrite) throws IOException, InterruptedException {
    final NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, account, pwd);
    final File srcFile = new File(srFile);
    final SmbFile destFile = toValidSmbFile(dtFile, auth);
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
      final String account,
      final String pwd,
      final String srFile,
      final String dtFile,
      final boolean overWrite) throws IOException, InterruptedException {
    final NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, account, pwd);
    final SmbFile srcFile = toValidSmbFile(srFile, auth);
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
      NtlmPasswordAuthentication
      auth,
      CopyAction
      action,
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

  public static final void smbLogin(String ipAddress, String username, String password) {
    try {
      UniAddress domain = UniAddress.getByName(ipAddress);
      NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(ipAddress, username, password);
      SmbSession.logon(domain, auth);
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
