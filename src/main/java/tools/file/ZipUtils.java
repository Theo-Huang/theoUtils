package tools.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.zip.ZipOutputStream;

public class ZipUtils {

  static void copy(InputStream in, OutputStream out) throws IOException {
    copy(in, out, true, true);
  }

  static void copy(InputStream in, OutputStream out, boolean closeInput, boolean closeOutput) throws IOException {
    try {
      byte[] buffer = new byte[1024];
      while (true) {
        int readCount = in.read(buffer);
        if (readCount < 0) {
          break;
        }
        out.write(buffer, 0, readCount);
      }
    } finally {
      if (in != null && closeInput) {
        in.close();
      }
      if (out != null && closeOutput) {
        out.close();
      }
    }
  }



  static void copy(TFile file, ZipOutputStream out) throws IOException {
    copy(file, out, true, true);
  }


  static void copy(TFile file, ZipOutputStream out, boolean closeInput, boolean closeOutput) throws IOException {
    TFileInputStream in = new TFileInputStream(file);
    copy(in, out, closeInput, closeOutput);
  }


  static void copy(InputStream in, TFile file) throws IOException {
    copy(in, file, true, true);
  }


  static void copy(InputStream in, TFile file, boolean closeInput, boolean closeOutput) throws IOException {
    OutputStream out = new TFileOutputStream(file);
    copy(in, out, closeInput, closeOutput);
  }


}
