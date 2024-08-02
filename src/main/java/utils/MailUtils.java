package utils;

import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

public class MailUtils {

	public enum Sender {
		// NAOL("developer.naol@homefirstindia.com"),
		DNR("donotreply@homefirstindia.com");

		public final String value;

		Sender(String value) {
			this.value = value;
		}
	}

	private static MailUtils instance = null;
	// private static String naolCode = null;
	private static String dnrCode = null;

	public enum ContentType {
		TEXT_PLAIN("text/plain"), TEXT_HTML("text/html"), TEXT_CSV("text/csv"),
		TEXT_PDF("application/pdf");

		public final String value;

		ContentType(String value) {
			this.value = value;
		}
	}

	private MailUtils() {}

	public static MailUtils getInstance() throws Exception {
		if (null == instance) instance = new MailUtils();
		//if (null == naolCode) naolCode = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.NAOL_CODE);
		if (null == dnrCode) dnrCode = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.DNR_CODE);
		return instance;
	}

	/*
	 * public boolean sendDefaultMail(String subject, String emailMessage, String...
	 * users) {
	 * 
	 * final String user="developer.naol@homefirstindia.com";
	 * 
	 * //Get the session object Properties props = new Properties();
	 * props.setProperty("mail.transport.protocol", "smtp");
	 * props.setProperty("mail.host", "smtp.gmail.com"); props.put("mail.smtp.auth",
	 * "true"); props.put("mail.smtp.port", "465"); props.put("mail.debug", "true");
	 * props.put("mail.smtp.socketFactory.port", "465");
	 * props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
	 * props.put("mail.smtp.socketFactory.fallback", "false");
	 * 
	 * try {
	 * 
	 * //final String naolCode =
	 * ProptertyUtils.getValurForKey(ProptertyUtils.Keys.NAOL_CODE);
	 * 
	 * Session session = Session.getDefaultInstance(props, new
	 * javax.mail.Authenticator() { protected PasswordAuthentication
	 * getPasswordAuthentication() { return new PasswordAuthentication(user,
	 * naolCode); } } );
	 * 
	 * InternetAddress[] recipients = new InternetAddress[users.length]; for (int i
	 * = 0; i < users.length; i++) { recipients[i] = new InternetAddress(users[i]);
	 * }
	 * 
	 * MimeMessage message = new MimeMessage(session); message.setFrom(new
	 * InternetAddress(user)); message.addRecipients(Message.RecipientType.TO,
	 * recipients); message.setSubject(subject); message.setContent(emailMessage,
	 * "text/plain");
	 * 
	 * //send the message Transport.send(message);
	 * 
	 * LoggerUtils.log("==> Mail sent successfully...");
	 * 
	 * return true;
	 * 
	 * } catch (Exception e) { System.out.println("\n\nFailed to send message: " +
	 * e.toString()); e.printStackTrace(); }
	 * 
	 * return false; }
	 */

	public boolean sendDefaultMail( 
			ContentType contentType, 
			String subject, 
			String emailMessage,
			String... users
	) {

		// Get the session object
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", "smtp.gmail.com");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		props.put("mail.debug", "true");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");

		try {

			// final String passcode = (sender == Sender.NAOL) ? naolCode : dnrCode;

			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(Sender.DNR.value, dnrCode);
				}
			});

			InternetAddress[] recipients = new InternetAddress[users.length];
			for (int i = 0; i < users.length; i++) {
				recipients[i] = new InternetAddress(users[i]);
			}

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress((Sender.DNR.value),"HomeFirst"));
			message.addRecipients(Message.RecipientType.TO, recipients);
			message.setSubject(subject);
			message.setContent(emailMessage, contentType.value);

			// send the message
			Transport.send(message);

			LoggerUtils.log("==> Mail sent successfully...");

			return true;

		} catch (Exception e) {
			System.out.println("\n\nFailed to send message: " + e.toString());
			e.printStackTrace();
		}

		return false;
	}
	
	public boolean sendDefaultMailAttach(ContentType contentType, String subject, String fileName, String filePath,
			String text, String... users) {

		// Get the session object
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", "smtp.gmail.com");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		props.put("mail.debug", "true");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");

		try {

			// final String passcode = (sender == Sender.NAOL) ? naolCode : dnrCode;
			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(Sender.DNR.value, dnrCode);
				}
			});

			InternetAddress[] recipients = new InternetAddress[users.length];
			for (int i = 0; i < users.length; i++) {
				recipients[i] = new InternetAddress(users[i]);
			}

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress((Sender.DNR.value), "HomeFirst"));
			message.addRecipients(Message.RecipientType.TO, recipients);
			message.setSubject(subject);
			message.setContent(fileName, contentType.value);

			BodyPart messageBodyPart1 = new MimeBodyPart();
			messageBodyPart1.setText(text);

			MimeBodyPart messageBodyPart2 = new MimeBodyPart();

			DataSource source = new FileDataSource(filePath);
			messageBodyPart2.setDataHandler(new DataHandler(source));
			messageBodyPart2.setFileName(fileName);

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart1);
			multipart.addBodyPart(messageBodyPart2);

			message.setContent(multipart);

			// send the message
			Transport.send(message);

			LoggerUtils.log("==> Mail sent successfully...");

			return true;

		} catch (Exception e) {
			System.out.println("\n\nFailed to send message: " + e.toString());
			e.printStackTrace();
		}

		return false;
	}

}
