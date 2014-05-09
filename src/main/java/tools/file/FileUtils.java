package tools.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.IOUtils;

import tools.config.OptionUtils;
import exception.NotFoundException;

public class FileUtils {
  public static final String File_SEP = File.separator;
  public static final String Line_SEP = System.getProperty("line.separator");
  private static final Object syncObj = new Object();
  private static String fileCharset = Charset.forName("UTF-8").toString();

  public static final void fileDelete(File file, String containToDelete) {
    FileDeleter fd = new FileDeleter(file, containToDelete);
    fd.delete();
  }

  public static final boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  public static final void setFilePermission(File file, int permission) throws IOException {
    final int owner = 3;
    final int type = 3;
    int tempOwner = owner;
    int tempType = type;
    Boolean permissions[][] = new Boolean[owner][type];
    if (permission > 777) {
      throw new IOException("Invalid permission.");
    }
    for (int i = 100; i > 0; i /= 10) {
      tempOwner--;
      for (int j = 4, k = permission / i; j > 0; j = j >>> 1) {
        tempType--;
        if (k > 7) {
          throw new IOException("Invalid permission of:" + k);
        }
        if (k / j >= 1) {
          permissions[tempOwner][tempType] = true;
          k = k - j;
        } else {
          permissions[tempOwner][tempType] = false;
        }
      }
      permission = permission % i;
      tempType = type;
    }
    tempOwner = owner;
    Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
    //add owner permissions
    if (permissions[2][2])
      perms.add(PosixFilePermission.OWNER_READ);
    if (permissions[2][1])
      perms.add(PosixFilePermission.OWNER_WRITE);
    if (permissions[2][0])
      perms.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    if (permissions[1][2])
      perms.add(PosixFilePermission.GROUP_READ);
    if (permissions[1][1])
      perms.add(PosixFilePermission.GROUP_WRITE);
    if (permissions[1][0])
      perms.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    if (permissions[0][2])
      perms.add(PosixFilePermission.OTHERS_READ);
    if (permissions[0][1])
      perms.add(PosixFilePermission.OTHERS_WRITE);
    if (permissions[0][0])
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
    setFilePermission(file, perms);
  }

  public static final void setFilePermission(File file, Set<PosixFilePermission> perms) throws IOException {
    Files.setPosixFilePermissions(Paths.get(file.getAbsolutePath()), perms);
    if (file.isDirectory() && file.canRead()) {
      for (File f : file.listFiles()) {
        setFilePermission(f, perms);
      }
    }
  }

  public static final File getTempFolder() {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  public static final List<String> fileRead(InputStream in) throws IOException {
    List<String> returnStrList = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new InputStreamReader(in, fileCharset));
    try {
      String line;
      while ((line = br.readLine()) != null) {
        if (line != null) {
          returnStrList.add(line);
        }
      }
    } finally {
      in.close();
    }
    return returnStrList;
  }

  public static final File getSystemTempFile() {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  public static final void setFileCharset(String charset) {
    fileCharset = Charset.forName(charset).toString();
  }

  public static final List<String> readFileData(File file, Boolean... sync) throws IOException {
    return readFileData(new DataInputStream(new FileInputStream(file)), sync);
  }

  private static final List<String> fileRead(InputStream in, boolean sync) throws IOException {
    if (sync) {
      synchronized (syncObj) {
        return fileRead(in);
      }
    } else {
      return fileRead(in);
    }
  }

  public static final List<String> readFileData(InputStream in, Boolean... sync) throws IOException {
    return fileRead(in, OptionUtils.getBooleanArrayOption(sync, false));
  }

  private static final void syncWriteFile(File file, boolean append, List<String> content) throws IOException {
    synchronized (syncObj) {
      writeFile(file, append, content);
    }
  }

  private static final void writeFile(File file, boolean append, List<String> content) throws IOException {
    Writer out = null;
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
        file.createNewFile();
      }
      out = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(file), fileCharset));
      for (String str : content) {
        out.append(str + Line_SEP);
      }
    } finally {
      if (out != null) {
        out.flush();
        out.close();
      }
    }
  }

  public static final void writeFileData(File file, boolean append, List<String> content, Boolean... sync) throws IOException {
    if (OptionUtils.getBooleanArrayOption(sync, false)) {
      syncWriteFile(file, append, content);
    } else
      writeFile(file, append, content);
  }

  public static final File depthFindFile(File defaultFile, final String filePattern) {
    File returnFile = null;
    String[] files = defaultFile.list();
    if (files == null || files.length == 0)
      return null;
    File findFile = getFileInFolder(defaultFile, filePattern);
    if (findFile != null) {
      return findFile;
    }
    for (File file : defaultFile.listFiles()) {
      returnFile = depthFindFile(file, filePattern);
      if (returnFile != null) {
        return returnFile;
      }
    }
    return null;
  }

  public static final File getFileInFolder(File defaultFolder, String pattern) {
    File[] files = defaultFolder.isDirectory() ? defaultFolder.listFiles() : defaultFolder.getParentFile().listFiles();
    for (File f : files) {
      if (f.getName().matches(pattern)) {
        return f;
      }
    }
    return null;
  }

  public static final boolean copyFile(String srcFilepath, String dstFilepath, boolean overWrite) {
    return copyFile(new File(srcFilepath), new File(dstFilepath), overWrite);
  }

  public static final boolean copyFile(File srcFile, File dstFile, boolean overWrite) {
    try {
      copyFileHandler copyThread =
          new copyFileHandler(srcFile, dstFile, overWrite);
      copyThread.start();
      copyThread.join();
      return copyThread.isSuccess();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static final boolean copyFileStream(final InputStream in, final OutputStream out) throws IOException {
    try {
      IOUtils.copy(in, out);
      return true;
    } catch (IOException e) {
      throw e;
    } finally {
      in.close();
      out.flush();
      out.close();
    }
  }

  public static final boolean unzipFile(File source) throws IOException {
    return unzipFile(source, source.getParentFile());
  }

  public static final boolean unzipFile(File source, File destFolder) throws IOException {
    if (!source.exists()) {
      throw new NotFoundException("Can't find source zip:" + source.getAbsolutePath());
    }
    if (!destFolder.exists()) {
      destFolder.mkdirs();
    }
    UnZip unzip = new UnZip(source, destFolder);
    unzip.unzip();
    return true;
  }

  public static final boolean zipFile(File source) throws IOException {
    return zipFile(source, new File(source.getAbsolutePath() + ".zip"), false);
  }

  public static final boolean zipFile(File source, File DestZip) throws IOException {
    return zipFile(source, DestZip, false);
  }

  public static final boolean zipFile(File source, File DestZip, boolean isLenient) throws IOException {
    tools.file.Zip zip = new tools.file.Zip(source, DestZip, isLenient);
    if (!source.exists()) {
      throw new NotFoundException("Can't find source file:" + source.getAbsolutePath());
    }
    if (!DestZip.getParentFile().exists()) {
      DestZip.getParentFile().mkdirs();
    }
    zip.zip();
    if (DestZip.exists()) {
      return true;
    }
    return false;
  }

  public static final void copyResourceFromJar(
      @SuppressWarnings("rawtypes") Class executingClass,
      String jarRelativePath,
      File outputFile) throws IOException, InterruptedException {
    InputStream stream = executingClass.getClass().getResourceAsStream(jarRelativePath);
    if (stream == null) {
      throw new FileNotFoundException("No such file in jar");
    }
    if (!outputFile.exists()) {
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    }
    OutputStream resStreamOut = new FileOutputStream(outputFile);
    copyFileHandler cfh = new copyFileHandler(stream, resStreamOut);
    cfh.start();
    cfh.join();
  }

}

class copyFileHandler extends Thread {
  private InputStream in;
  private OutputStream out;
  private File srcFile;
  private File dstFile;
  private boolean isSuccess = false;
  private boolean overWrite = false;

  public copyFileHandler(final InputStream In, final OutputStream Out) {
    in = In;
    out = Out;
    srcFile = null;
    dstFile = null;
  }

  public copyFileHandler(final File SrcFile, final File DstFile, boolean OverWrite) {
    srcFile = SrcFile;
    dstFile = DstFile;
    in = null;
    out = null;
    overWrite = OverWrite;
  }

  @Override
  public void run() {
    try {
      isSuccess = in == null ? copyFile(srcFile, dstFile) : FileUtils.copyFileStream(in, out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isSuccess() {
    return isSuccess;
  }

  private boolean copyFile(final File SrcFile, File DstFile) throws IOException {
    if (DstFile.exists() && !overWrite) {
      throw new FileExistsException(DstFile.getAbsolutePath() + " already exists...");
    }
    if (SrcFile.isDirectory()) {
      org.apache.commons.io.FileUtils.copyDirectory(SrcFile, DstFile);
    } else {
      String SrcFileName = SrcFile.getName();
      if (!SrcFileName.equals(DstFile.getName())) {
        DstFile = new File(DstFile.getAbsoluteFile() + FileUtils.File_SEP + SrcFileName);
      }
      DstFile.getParentFile().mkdirs();
      org.apache.commons.io.FileUtils.copyFile(SrcFile, DstFile);
    }
    return true;
  }

}

class FileDeleter {
  /**
   * before edit this class please make sure your brain is clear....
   * 
   * especially notice that filter's action in this class
   * 
   * if miss something, you will get a folder with nothing....
   * 
   * */
  final boolean isContainToDeleteEmpty;
  final String containToDelete;
  final File targetDeletefile;

  FileDeleter(File file, String containToDelete) {
    isContainToDeleteEmpty = containToDelete == null || containToDelete.isEmpty();
    this.containToDelete = containToDelete;
    this.targetDeletefile = file;
  }

  void delete() {
    if (!targetDeletefile.exists()) {
      return;
    }
    fileDelete(targetDeletefile, containToDelete);
  }

  private void forceDelete(File deleteFile) {
    try {
      FileDeleteStrategy.FORCE.delete(deleteFile);
    } catch (Exception e) {
      try {
        if (tools.system.SystemUtils.isMac() || tools.system.SystemUtils.isUnix()) {
          tools.system.SystemUtils.executeCMD("sudo rm -rf " + deleteFile.getAbsoluteFile(), false);
        } else if (tools.system.SystemUtils.isWindows()) {
          tools.system.SystemUtils.executeCMD("del /f " + deleteFile.getAbsoluteFile(), false);
        }
      } catch (Exception e1) {
        //          e1.printStackTrace();
        //          ignore
      }
      //e.printStackTrace();
      //ignore
    }
  }

  private void fileDelete(File folder, String containToDelete) {
    try {
      for (final File fileEntry : folder.listFiles()) {
        if (fileEntry.isDirectory()) {
          fileDelete(fileEntry, containToDelete);
        } else if (isContainToDeleteEmpty || fileEntry.getName().contains(containToDelete)) {
          forceDelete(fileEntry);
        }
      }
      if (folder.listFiles().length == 0 && isContainToDeleteEmpty) {
        forceDelete(folder);
      }
    } catch (Exception e) {
      if (containToDelete == null || containToDelete.isEmpty()) {
        forceDelete(folder);
      }
    }
  }
  /******* Warning *******/

}
