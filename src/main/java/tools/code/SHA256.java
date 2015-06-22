/**
 * 
 */
package tools.code;


import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;


/**
 * @author Theo Huang
 * @mail theo_huang@trend.com.tw
 *
 */
public class SHA256 {

  public static final String getHex(File file) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file.getAbsolutePath());
      byte[] dataBytes = new byte[1024];

      int nread = 0;
      while ((nread = fis.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }
      byte[] hashedBytes = md.digest();

      return hash(hashedBytes);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  public static final String getHex(String str) throws Exception {

    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(str.getBytes());
    byte byteData[] = md.digest();

    return hash(byteData);
  }


  private static String hash(byte[] mdbytes) throws Exception {

    // convert the byte to hex format method 1
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < mdbytes.length; i++) {
      sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
    }
    //
    // // convert the byte to hex format method 2
    // StringBuffer hexString = new StringBuffer();
    // for (int i = 0; i < mdbytes.length; i++) {
    // hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
    // }

    return sb.toString();
  }


}
