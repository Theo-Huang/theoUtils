package tools.network.email;

import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import com.google.common.collect.Lists;

public class EmailReceiver {

  private final static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
  private String email;
  private String pwd;
  private SupportedProtocal protocal;
  private boolean readNewOnly;
  private File attachmentSaveFolder;
  private boolean receiveAttachment = false;

  public enum SupportedReceiveServer {
    gmail,
  }

  public enum SupportedProtocal {
    pop3, imap
  }

  public EmailReceiver(
      String email,
      String pwd,
      SupportedProtocal protocal,
      boolean readNewOnly) {
    this.email = email;
    this.pwd = pwd;
    this.protocal = protocal;
    this.readNewOnly = readNewOnly;
  }

  private Store getMailFieldAndConnect() throws NoSuchProviderException, MessagingException, Exception {
    if (email.toLowerCase().contains("@gmail.")) {
      Properties props = System.getProperties();
      Session session;
      URLName urln;
      Store store;
      MimeMessage msg;
      switch (protocal) {
        case imap:
          String imapHost = "imap.gmail.com";
          props.setProperty("mail.imap.host", imapHost);
          props.setProperty("mail.imap.port", "993");
          props.setProperty("mail.smtp.auth", "true");
          props.setProperty("mail.store.protocol", "imaps");
          session = Session.getDefaultInstance(props, null);
          urln = new URLName(protocal.name(), imapHost, 995, null, email.substring(0, email.indexOf("@")), pwd);
          store = session.getStore("imaps");
          store.connect(imapHost, email.substring(0, email.indexOf("@")), pwd);
          msg = new MimeMessage(session);
          msg.setHeader("Content-Transfer-Encoding", tools.office.StringUtils.getUTF8String());
          return store;
        case pop3:
          String pop3Host = "imap.gmail.com";
          props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
          props.setProperty("mail.pop3.socketFactory.fallback", "false");
          props.setProperty("mail.pop3.port", "995");
          props.setProperty("mail.pop3.socketFactory.port", "995");
          props.setProperty("mail.smtp.host", pop3Host);
          props.setProperty("mail.smtp.auth", "true");
          session = Session.getDefaultInstance(props, null);
          urln = new URLName(protocal.name(), pop3Host, 995, null, email.substring(0, email.indexOf("@")), pwd);
          store = session.getStore(urln.getProtocol());
          store.connect(pop3Host, email.substring(0, email.indexOf("@")), pwd);
          msg = new MimeMessage(session);
          msg.setHeader("Content-Transfer-Encoding", tools.office.StringUtils.getUTF8String());
          return store;
      }
    }
    throw new IllegalArgumentException("Unsupport");
  }

  public void setAttachmentSaveFolder(File folder) {
    attachmentSaveFolder = folder;
  }

  public void setReceiveAttachment(boolean receiveAttachment) {
    this.receiveAttachment = receiveAttachment;
  }

  public List<MailEntity> reciveMail(int maxReceiveNum) throws NoSuchProviderException, MessagingException, Exception {
    ReceivedMimeMessage pmm = null;
    List<MailEntity> mailEntityList = Lists.newLinkedList();
    MailEntity mailEntity;
    Store store = getMailFieldAndConnect();
    Folder folder = store.getFolder("INBOX");
    folder.open(Folder.READ_ONLY);
    Message message[] = folder.getMessages();
    try {
      if (message.length > 0) {
        // get all mail  
        for (int i = message.length - 1; i > message.length - maxReceiveNum - 1 && i >= 0; i--) {
          pmm = new ReceivedMimeMessage((MimeMessage) message[i]);
          if (readNewOnly ? !pmm.isNew() : true) {
            mailEntity = new MailEntity();
            mailEntity.setSubject(pmm.getSubject());
            mailEntity.setSentDate(pmm.getSentDate());
            mailEntity.setHasRead(pmm.isNew());
            mailEntity.setFrom(pmm.getFrom());
            mailEntity.setFromPersonal(pmm.getFromPersonal());
            mailEntity.setFromAddress(pmm.getFromAddress());
            mailEntity.setMailTo(pmm.getMailAddress("to"));
            // get mail content
            pmm.getMailContent((Part) message[i]);
            mailEntity.setContentText(pmm.getBodyText().get(0));
            mailEntity.setContent(pmm.getBodyText().get(1));
            // save attachment
            if (receiveAttachment && attachmentSaveFolder != null && pmm.hasAttachment((Part) message[i])) {
              if (!attachmentSaveFolder.exists()) {
                attachmentSaveFolder.mkdirs();
              }
              pmm.setAttachPath(attachmentSaveFolder.getAbsolutePath());
              pmm.saveAttachMent((Part) message[i]);
              mailEntity.setAttachmentSaveFolder(attachmentSaveFolder);
            }
            mailEntityList.add(mailEntity);
          } else {
            continue;
          }
        }
      }
      return mailEntityList;
    } finally {
      folder.close(false);
      store.close();
    }
  }
}
