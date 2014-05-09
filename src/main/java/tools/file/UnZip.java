package tools.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipFile;

public class UnZip {

  //  private static final int BUFFER = 2048;
  private TFile source;
  private TFile targetFolder;

  public UnZip(java.io.File sourceToUnZip, java.io.File target) {
    source = new TFile(sourceToUnZip);
    targetFolder = new TFile(target);
  }

  public void unzip() throws IOException {
    TVFS.umount(source);
    TVFS.umount();
    //    source.cp_rp(targetFolder);
    //    TFile.cp_rp(source, targetFolder, TArchiveDetector.NULL, TArchiveDetector.NULL);
    toUnzip();
    TVFS.umount();
  }

  private void toUnzip() throws IOException {
    ZipFile zfile = new ZipFile(source, Zip.getCharset());
    Enumeration<? extends ZipEntry> entries = zfile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      TFile file = new TFile(targetFolder, entry.getName());
      if (entry.isDirectory()) {
        file.mkdirs();
      } else {
        file.getParentFile().mkdirs();
        InputStream in = zfile.getInputStream(entry);
        try {
          ZipUtils.copy(in, file);
        } finally {
          in.close();
        }
      }
    }
  }

  //  public void unzip() throws IOException {
  //    try {
  //      String fileName = source.getAbsolutePath();
  //      if (!fileName.endsWith("zip")) {
  //        return;
  //      }
  //      ZipFile zipFile = new ZipFile(fileName, charset);
  //      String targetFolderPath = targetFolder.getAbsolutePath() + tools.FileUtils.File_SEP;
  //      @SuppressWarnings("rawtypes")
  //      Enumeration emu = zipFile.entries();
  //      BufferedInputStream bis;
  //      File file;
  //      File parent;
  //      FileOutputStream fos;
  //      BufferedOutputStream bos;
  //      while (emu.hasMoreElements()) {
  //        ZipEntry entry = (ZipEntry) emu.nextElement();
  //        if (entry.isDirectory())
  //        {
  //          new File(targetFolderPath + entry.getName()).mkdirs();
  //          continue;
  //        }
  //        bis = new BufferedInputStream(zipFile.getInputStream(entry));
  //        file = new File(targetFolderPath + entry.getName());
  //        parent = file.getParentFile();
  //        if (parent != null && (!parent.exists())) {
  //          parent.mkdirs();
  //        }
  //        fos = new FileOutputStream(file);
  //        bos = new BufferedOutputStream(fos, BUFFER);
  //
  //        int count;
  //        byte data[] = new byte[BUFFER];
  //        while ((count = bis.read(data, 0, BUFFER)) != -1)
  //        {
  //          bos.write(data, 0, count);
  //        }
  //        bos.flush();
  //        bos.close();
  //        bis.close();
  //      }
  //      zipFile.close();
  //    } catch (Exception e) {
  //      e.printStackTrace();
  //    }
  //  }
}
