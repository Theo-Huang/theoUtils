package tools.network.email;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

public class ReceivedMimeMessage {
  private MimeMessage mimeMessage = null;
  private String saveAttachPath = ""; // the path of attach file save
  private StringBuffer bodytext = new StringBuffer();// save mail content
  private StringBuffer bodyHtmltext = new StringBuffer();// save mail content
  private String dateformat = "yy-MM-dd HH:mm"; // default date format

  public ReceivedMimeMessage(MimeMessage mimeMessage) {
    this.mimeMessage = mimeMessage;
  }

  public void setMimeMessage(MimeMessage mimeMessage) {
    this.mimeMessage = mimeMessage;
  }

  /**
   * get from who & it't address
   * 
   * @throws Exception
   */
  public String getFrom() throws Exception {
    return getFromPersonal() + "," + getFromAddress();
  }

  public String getFromAddress() throws Exception {
    InternetAddress address[] = (InternetAddress[]) mimeMessage.getFrom();
    String from = address[0].getAddress();
    if (from == null)
      from = "";
    return from;
  }

  public String getFromPersonal() throws Exception {
    InternetAddress address[] = (InternetAddress[]) mimeMessage.getFrom();
    String personal = address[0].getPersonal();
    if (personal == null)
      personal = "";
    return personal;
  }

  /**
   * get receiver's info
   */
  public String getMailAddress(String type) throws Exception {
    String mailaddr = "";
    String addtype = type.toUpperCase();
    InternetAddress[] address = null;
    if (addtype.equals("TO") || addtype.equals("CC") || addtype.equals("BCC")) {
      if (addtype.equals("TO")) {
        address = (InternetAddress[]) mimeMessage.getRecipients(Message.RecipientType.TO);
      } else if (addtype.equals("CC")) {
        address = (InternetAddress[]) mimeMessage.getRecipients(Message.RecipientType.CC);
      } else {
        address = (InternetAddress[]) mimeMessage.getRecipients(Message.RecipientType.BCC);
      }
      if (address != null) {
        for (int i = 0; i < address.length; i++) {
          String email = address[i].getAddress();
          if (email == null)
            email = "";
          else {
            email = MimeUtility.decodeText(email);
          }
          String personal = address[i].getPersonal();
          if (personal == null)
            personal = "";
          else {
            personal = MimeUtility.decodeText(personal);
          }
          String compositeto = personal + "" + email + "";
          mailaddr += "," + compositeto;
        }
        mailaddr = mailaddr.substring(1);
      }
    } else {
      throw new Exception("Error emailaddr type!");
    }
    return mailaddr;
  }

  /**
   * get mail subject
   */
  public String getSubject() throws MessagingException {
    String subject = "";
    try {
      subject = MimeUtility.decodeText(mimeMessage.getSubject());
      if (subject == null)
        subject = "";
    } catch (Exception exce) {}
    return subject;
  }

  /**
   * get mail sent date
   */
  public String getSentDate() throws Exception {
    Date sentdate = mimeMessage.getSentDate();
    SimpleDateFormat format = new SimpleDateFormat(dateformat);
    return format.format(sentdate);
  }

  /**
   * get mail content
   */
  public List<String> getBodyText() {
    List<String> list = new ArrayList<String>();
    list.add(bodytext.toString());
    list.add(bodyHtmltext.toString());
    return list;
  }

  /**
   * analyze mail
   */
  public void getMailContent(Part part) throws Exception {
    String contenttype = part.getContentType();
    int nameindex = contenttype.indexOf("name");
    boolean conname = false;
    if (nameindex != -1)
      conname = true;
    if (part.isMimeType("text/plain") && !conname) {
      bodytext.append((String) part.getContent());
    } else if (part.isMimeType("text/html") && !conname) {
      bodyHtmltext.append((String) part.getContent());
    } else if (part.isMimeType("multipart/*")) {
      Multipart multipart = (Multipart) part.getContent();
      int counts = multipart.getCount();
      for (int i = 0; i < counts; i++) {
        getMailContent(multipart.getBodyPart(i));
      }
    } else if (part.isMimeType("message/rfc822")) {
      getMailContent((Part) part.getContent());
    } else {}
  }

  /**
   * analyze mail that need to replay or not ,if yes return true
   */
  public boolean getReplySign() throws MessagingException {
    boolean replysign = false;
    String needreply[] = mimeMessage.getHeader("Disposition-Notification-To");
    if (needreply != null) {
      replysign = true;
    }
    return replysign;
  }

  /**
   * get mail Message-ID
   */
  public String getMessageId() throws MessagingException {
    return mimeMessage.getMessageID();
  }

  /**
   * analyze mail is read if read return true
   */
  public boolean isNew() throws MessagingException {
    boolean isnew = false;
    Flags flags = ((Message) mimeMessage).getFlags();
    Flags.Flag[] flag = flags.getSystemFlags();
    for (int i = 0; i < flag.length; i++) {
      if (flag[i].equals(Flags.Flag.SEEN)) {
        isnew = true;
        break;
      }
    }
    return isnew;
  }

  //  /**
  //   * set mail read
  //   */
  //  public void isRead() throws MessagingException {
  //
  //  }

  /**
   * analyze mail has attachment
   */
  public boolean hasAttachment(Part part) throws Exception {
    boolean attachflag = false;
    if (part.isMimeType("multipart/*")) {
      Multipart mp = (Multipart) part.getContent();
      for (int i = 0; i < mp.getCount(); i++) {
        BodyPart mpart = mp.getBodyPart(i);
        String disposition = mpart.getDisposition();
        if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE))))
          attachflag = true;
        else if (mpart.isMimeType("multipart/*")) {
          attachflag = hasAttachment((Part) mpart);
        } else {
          String contype = mpart.getContentType();
          if (contype.toLowerCase().indexOf("application") != -1)
            attachflag = true;
          if (contype.toLowerCase().indexOf("name") != -1)
            attachflag = true;
        }
      }
    } else if (part.isMimeType("message/rfc822")) {
      attachflag = hasAttachment((Part) part.getContent());
    }
    return attachflag;
  }

  /**
   * save attachment
   */
  public void saveAttachMent(Part part) throws Exception {
    if (part.isMimeType("multipart/*")) {
      Multipart mp = (Multipart) part.getContent();
      for (int i = 0; i < mp.getCount(); i++) {
        BodyPart mpart = mp.getBodyPart(i);
        String disposition = mpart.getDisposition();
        if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) {
          saveFile(
              mpart,
              saveAttachPath,
              mpart.getInputStream());
        } else if (mpart.isMimeType("multipart/*")) {
          saveAttachMent(mpart);
        } else {
          saveFile(
              mpart,
              saveAttachPath,
              mpart.getInputStream());
        }
      }
    } else if (part.isMimeType("message/rfc822")) {
      saveAttachMent((Part) part.getContent());
    }
  }

  /**
   * set attachment save path
   */

  public void setAttachPath(String attachpath) {
    this.saveAttachPath = attachpath;
  }

  /**
   * set mail date show format
   */
  public void setDateFormat(String format) throws Exception {
    this.dateformat = format;
  }

  /**
   * get attachment save path
   */
  public String getAttachPath() {
    return saveAttachPath;
  }

  /**
   * real save file to input path
   * 
   * @throws IOException
   * @throws MessagingException
   */
  private void saveFile(BodyPart bodyPart, String savefilePathName, InputStream in) throws IOException, MessagingException {
    String fileName = bodyPart.getFileName();
    if ((fileName != null)) {
      fileName = MimeUtility.decodeText(fileName);
      BufferedOutputStream bos = null;
      bos = new BufferedOutputStream(
          new FileOutputStream(savefilePathName + tools.file.FileUtils.File_SEP + fileName));
      tools.file.FileUtils.copyFileStream(in, bos);
    }

  }
}
