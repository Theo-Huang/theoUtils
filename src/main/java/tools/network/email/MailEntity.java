package tools.network.email;

import java.io.File;

public class MailEntity {
  private String from;
  private String fromPersonal;
  private String fromAddress;
  private String subject;
  private String content;
  private String contentText;
  private String SentDate;
  private String mailTo;
  private File attachmentFolder;
  private boolean hasRead;

  public String getMailTo() {
    return mailTo;
  }

  public void setMailTo(String mailTo) {
    this.mailTo = mailTo;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public boolean isHasRead() {
    return hasRead;
  }

  public void setHasRead(boolean hasRead) {
    this.hasRead = hasRead;
  }

  public String getSentDate() {
    return SentDate;
  }

  public void setSentDate(String sentDate) {
    SentDate = sentDate;
  }

  public String getFromPersonal() {
    return fromPersonal;
  }

  public void setFromPersonal(String fromPersonal) {
    this.fromPersonal = fromPersonal;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setContentText(String content) {
    this.contentText = content;
  }

  public String getContentText() {
    return contentText;
  }

  public File getAttachmentSaveFolder() {
    return attachmentFolder;
  }

  public void setAttachmentSaveFolder(File folder) {
    attachmentFolder = folder;
  }

}
