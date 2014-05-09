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
    byte[] buffer = new byte[1024];
    while (true) {
      int readCount = in.read(buffer);
      if (readCount < 0) {
        break;
      }
      out.write(buffer, 0, readCount);
    }
  }

  static void copy(TFile file, ZipOutputStream out) throws IOException {
    TFileInputStream in = new TFileInputStream(file);
    try {
      copy(in, out);
    } finally {
      in.close();
    }
  }

  static void copy(InputStream in, TFile file) throws IOException {
    OutputStream out = new TFileOutputStream(file);
    try {
      copy(in, out);
    } finally {
      out.close();
    }
  }

}
