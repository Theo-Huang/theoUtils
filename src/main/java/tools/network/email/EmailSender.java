package tools.network.email;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailSender {
  private Properties props;
  private Authenticator authenticator;
  private String account;

  public EmailSender(
      final String account,
      final String pwd,
      String host,
      String port,
      boolean hasAuth,
      boolean ttlsEnable) {
    this.account = account;
    props = System.getProperties();
    props.setProperty("mail.transport.protocol", "smtp");
    props.setProperty("mail.mime.encodefilename", "true");
    props.setProperty("mail.smtp.auth", String.valueOf(hasAuth));
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.socketFactory.port", port);
    props.put("mail.smtp.connectiontimeout", 5000);
    props.put("mail.smtp.timeout", 5000);
    props.put("mail.smtp.starttls.enable", String.valueOf(ttlsEnable));
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    if (hasAuth) {
      authenticator = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(account, pwd);
        }
      };
    }
  }

  public void send(String subject, String content, String senderName, String sendToAddress)
      throws MessagingException, UnsupportedEncodingException {

    String encodingCharset = tools.office.StringUtils.getUTF8String();
    Session mailSession;
    if (authenticator == null) {
      mailSession = Session.getInstance(props);
    } else {
      mailSession = Session.getInstance(props, authenticator);
    }
    Transport transport = mailSession.getTransport();
    try {
      MimeMessage message = new MimeMessage(mailSession);
      Multipart multipart = new MimeMultipart("alternative");

      BodyPart bp = new MimeBodyPart();
      bp.setHeader("Content-Type", "text/html;charset=" + encodingCharset);
      bp.setHeader("Content-Transfer-Encoding", "quoted-printable");
      bp.setContent(content, "text/html;charset=" + encodingCharset);

      multipart.addBodyPart(bp);
      message.setContent(multipart);

      message.setFrom(new InternetAddress(account, senderName, encodingCharset));
      message.setSubject(subject, encodingCharset);
      message.addRecipient(javax.mail.Message.RecipientType.TO,
          new InternetAddress(sendToAddress));
      transport.connect();
      transport.sendMessage(message,
          message.getRecipients(javax.mail.Message.RecipientType.TO));
    } finally {
      transport.close();
    }
  }
}
