package tools.code;

import java.util.HashMap;

public class Base64 {

  private static final String default_code = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  private static final char default_pad = '$';

  private String encode = null;
  private char pad;
  private HashMap<Character, Integer> decode = null;

  public Base64() {
    this(null, default_pad);
  }

  public Base64(String code, char pad) {
    if (code != null && code.length() == 64) {
      this.encode = code;
      this.pad = pad;
    } else {
      this.encode = default_code;
      this.pad = default_pad;
    }

    decode = new HashMap<Character, Integer>();
    for (int i = 0; i < 64; i++) {
      decode.put(encode.charAt(i), i);
    }
    decode.put(this.pad, 0);
  }

  public String encode(String string) {
    String result = "";
    int padnum = 3 - (string.length() % 3);
    if (padnum == 3) {
      padnum = 0;
    }
    byte[] org = new byte[3];
    char[] res = new char[4];

    for (int i = 0; i < padnum; i++) {
      string += '\0';
    }

    for (int i = 0; i < string.length(); i += 3) {
      org[0] = (byte) string.charAt(i);
      org[1] = (byte) string.charAt(i + 1);
      org[2] = (byte) string.charAt(i + 2);

      res[0] = encode.charAt((org[0] >> 2) & 0x3F);
      res[1] = encode.charAt(((org[0] & 0x3) << 4)
          | ((org[1] >> 4) & 0xf));
      res[2] = encode.charAt(((org[1] & 0xf) << 2)
          | ((org[2] >> 6) & 0x3));
      res[3] = encode.charAt(org[2] & 0x3F);
      result = result + res[0] + res[1] + res[2] + res[3];
    }

    result = result.substring(0, result.length() - padnum);

    for (int i = 0; i < padnum; i++) {
      result += this.pad;
    }
    return result;
  }

  public String decode(String string) {
    String result = null;
    int padnum = 0;
    if (string.length() == 0) {
      return "";
    }
    if (string.length() % 4 != 0) {
      return null;
    }
    if (string.length() < 2) {
      return null;
    }
    if (string.charAt(string.length() - 2) == this.pad) {
      padnum = 2;
    } else if (string.charAt(string.length() - 1) == this.pad) {
      padnum = 1;
    }

    byte[] buf = new byte[string.length() / 4 * 3];
    byte[] org = new byte[4];
    byte[] res = new byte[3];
    for (int i = 0; i < string.length(); i += 4) {
      org[0] = ((Integer) decode.get(string.charAt(i))).byteValue();
      org[1] = ((Integer) decode.get(string.charAt(i + 1))).byteValue();
      org[2] = ((Integer) decode.get(string.charAt(i + 2))).byteValue();
      org[3] = ((Integer) decode.get(string.charAt(i + 3))).byteValue();

      res[0] = (byte) ((org[0] << 2) | (org[1] >> 4));
      res[1] = (byte) (((org[1] & 0xf) << 4) | (org[2] >> 2));
      res[2] = (byte) (((org[2] & 0x3) << 6) | org[3]);
      buf[i / 4 * 3] = res[0];
      buf[i / 4 * 3 + 1] = res[1];
      buf[i / 4 * 3 + 2] = res[2];
    }
    result = new String(buf);
    result = result.substring(0, buf.length - padnum);
    return result;
  }
}
