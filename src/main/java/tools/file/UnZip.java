package tools.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipFile;

public class UnZip {

  // private static final int BUFFER = 2048;
  private TFile source;
  private TFile targetFolder;
  boolean isSourceZip;

  public UnZip(java.io.File sourceToUnZip, java.io.File target) throws IOException {
    if (sourceToUnZip.getName().trim().toLowerCase().endsWith(".zip")) {
      isSourceZip = true;
    } else {
      isSourceZip = false;
    }

    source = new TFile(sourceToUnZip);
    targetFolder = new TFile(target);
  }

  public void unzip() throws IOException {
    try {
      TVFS.umount(source);
      TVFS.umount();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // source.cp_rp(targetFolder);
    // TFile.cp_rp(source, targetFolder, TArchiveDetector.NULL, TArchiveDetector.NULL);
    toUnzip();
    TVFS.umount();

  }

  private void toUnzip() throws IOException {
    if (isSourceZip) {
      ZipFile zfile = new ZipFile(source, Zip.getCharset());
      Enumeration<? extends ZipEntry> entries = zfile.entries();
      List<InputStream> colseI = new ArrayList<InputStream>();
      try {
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          TFile file = new TFile(targetFolder, entry.getName());
          if (entry.isDirectory()) {
            file.mkdirs();
          } else {
            file.getParentFile().mkdirs();
            InputStream in = zfile.getInputStream(entry);
            colseI.add(in);
            ZipUtils.copy(in, file, false, true);
          }
        }
      } finally {
        for (InputStream is : colseI) {
          try {
            is.close();
          } catch (Exception e) {
            // ignore
          }
        }
        zfile.close();
      }
    } else {
      String folderName = source.getName();
      folderName = folderName.replace(".tar", "");
      folderName = folderName.replace(".gz", "");
      folderName = folderName.replace(".bz2", "");
      folderName = folderName.replace(".rar", "");
      folderName = folderName.replace(".7zip", "");
      folderName = folderName.replace(".zip", "");

      new TFile(source).cp_rp(new File(targetFolder.getAbsolutePath() + FileUtils.File_SEP + folderName));
    }
  }

  public static void main(String[] args) {
    try {
      tools.file.FileUtils.unzipFile(new File("/Users/theohuang/Desktop/12345.tar.gz"),
          new File("/Users/theohuang/Desktop/" + System.currentTimeMillis()));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
  // public void unzip() throws IOException {
  // try {
  // String fileName = source.getAbsolutePath();
  // if (!fileName.endsWith("zip")) {
  // return;
  // }
  // ZipFile zipFile = new ZipFile(fileName, charset);
  // String targetFolderPath = targetFolder.getAbsolutePath() + tools.FileUtils.File_SEP;
  // @SuppressWarnings("rawtypes")
  // Enumeration emu = zipFile.entries();
  // BufferedInputStream bis;
  // File file;
  // File parent;
  // FileOutputStream fos;
  // BufferedOutputStream bos;
  // while (emu.hasMoreElements()) {
  // ZipEntry entry = (ZipEntry) emu.nextElement();
  // if (entry.isDirectory())
  // {
  // new File(targetFolderPath + entry.getName()).mkdirs();
  // continue;
  // }
  // bis = new BufferedInputStream(zipFile.getInputStream(entry));
  // file = new File(targetFolderPath + entry.getName());
  // parent = file.getParentFile();
  // if (parent != null && (!parent.exists())) {
  // parent.mkdirs();
  // }
  // fos = new FileOutputStream(file);
  // bos = new BufferedOutputStream(fos, BUFFER);
  //
  // int count;
  // byte data[] = new byte[BUFFER];
  // while ((count = bis.read(data, 0, BUFFER)) != -1)
  // {
  // bos.write(data, 0, count);
  // }
  // bos.flush();
  // bos.close();
  // bis.close();
  // }
  // zipFile.close();
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // }
}
