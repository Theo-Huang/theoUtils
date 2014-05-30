package tools.code;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public class MD5 {

  public static final MessageDigest md = getMessageDigest();
  private static int byteArraySize = 4096;

  private static final MessageDigest getMessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      //      e.printStackTrace();
      //  should not happen
    }
    return null;
  }

  public static final void setByteArraySize(int byteArraySize) {
    MD5.byteArraySize = byteArraySize;
  }

  public static final String getMD5(File file) throws Exception {
    String digest = getDigest(new FileInputStream(file));
    return digest;
  }

  public synchronized static final String getDigest(InputStream is) throws Exception {
    md.reset();
    byte[] bytes = new byte[byteArraySize];
    int numBytes;
    while ((numBytes = is.read(bytes)) != -1) {
      md.update(bytes, 0, numBytes);
    }
    byte[] digest = md.digest();
    String result = new String(Hex.encodeHex(digest));
    return result;
  }

  public synchronized static final String getMD5(String str) {
    md.reset();
    StringBuffer hexString = new StringBuffer();
    byte[] hash = md.digest(str.getBytes());
    for (int i = 0; i < hash.length; i++) {
      if ((0xff & hash[i]) < 0x10) {
        hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
      } else {
        hexString.append(Integer.toHexString(0xFF & hash[i]));
      }
    }
    return hexString.toString();
  }

}
