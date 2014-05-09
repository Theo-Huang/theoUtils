package tools.file;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipOutputStream;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;

/**
 * https://truezip.java.net/faq.html
 */
public class Zip {
  private static TConfig config;
  private TFile target;
  private TFile source;
  private boolean isLenient;

  static {
    HashMap<FsScheme, FsDriver> map = new HashMap<FsScheme, FsDriver>();
    map.put(FsScheme.create("zip"), new newZipDriver());
    config = TConfig.push();
    config.setArchiveDetector(new TArchiveDetector(TConfig.get().getArchiveDetector(), map));
    config.setOutputPreferences(config.getOutputPreferences().set(FsOutputOption.GROW));
  }

  public Zip(java.io.File source, java.io.File target) {
    this(source, target, false);
  }

  public Zip(java.io.File source, java.io.File target, boolean isLenient) {
    this.source = new TFile(source);
    this.target = new TFile(target);
    this.isLenient = isLenient;
    config.setLenient(isLenient);
    config = TConfig.push();
  }

  public void zip() throws IOException {
    try {
      URI base = source.toURI();
      Deque<TFile> queue = new LinkedList<TFile>();
      queue.push(source);
      TFileOutputStream out = new TFileOutputStream(target);
      Closeable res = out;
      try {
        ZipOutputStream zout =
            new ZipOutputStream(
                out,
                getCharset());
        res = zout;
        while (!queue.isEmpty()) {
          source = queue.pop();
          for (TFile kid : source.listFiles()) {
            String name = base.relativize(new java.io.File(kid.getAbsolutePath()).toURI()).getPath();
            if (kid.isDirectory()) {
              queue.push(kid);
              name = name.endsWith("/") ? name : name + "/";
              try {
                zout.putNextEntry(new ZipEntry(name));
              } catch (IOException e) {
                if (!isLenient) {
                  throw e;
                }
              }
            } else {
              try {
                zout.putNextEntry(new ZipEntry(name));
                ZipUtils.copy(kid, zout);
              } catch (IOException e) {
                if (!isLenient) {
                  throw e;
                }
              } finally {
                zout.closeEntry();
              }
            }
          }
        }
      } finally {
        res.close();
        TVFS.umount(target);
      }
    } catch (Exception ee) {
      throw new IOException(ee);
    }
  }

  public static final Charset getCharset() {
    return tools.system.SystemUtils.isWindows() ?
        Charset.forName(tools.office.StringUtils.getBIG5String()) :
        Charset.forName("CP437");
  }
  //  public void zip() {
  //    generateFileList(this.source);
  //    if (!target.getName().toLowerCase().endsWith(".zip")) {
  //      throw new exception.UtilityException("Final zip file name path must end with '.zip'");
  //    }
  //    if (!target.getParentFile().exists()) {
  //      target.getParentFile().mkdirs();
  //    }
  //    zipIt(this.target);
  //  }
  //
  //  private void zipIt(File zipFile) {
  //    byte[] buffer = new byte[1024];
  //    try {
  //      FileOutputStream fos = new FileOutputStream(zipFile);
  //      ZipOutputStream zos = new ZipOutputStream(fos);
  //      for (String file : this.fileList) {
  //        ZipEntry ze = new ZipEntry(file);
  //        zos.putNextEntry(ze);
  //        FileInputStream in = new FileInputStream(source + tools.FileUtils.File_SEP + file);
  //        int len;
  //        while ((len = in.read(buffer)) > 0) {
  //          zos.write(buffer, 0, len);
  //        }
  //        in.close();
  //      }
  //      zos.closeEntry();
  //      zos.close();
  //    } catch (IOException ex) {
  //      ex.printStackTrace();
  //    }
  //  }
  //
  //  private void generateFileList(File node) {
  //    if (node.isFile()) {
  //      fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
  //    }
  //    if (node.isDirectory()) {
  //      String[] subNote = node.list();
  //      for (String filename : subNote) {
  //        generateFileList(new File(node, filename));
  //      }
  //    }
  //  }
  //
  //  private String generateZipEntry(String file) {
  //    return file.substring(this.source.getAbsolutePath().length() + 1, file.length());
  //  }
}

class newZipDriver extends ZipDriver {
  protected newZipDriver() {
    super(IOPoolLocator.SINGLETON, Zip.getCharset());
  }
}
