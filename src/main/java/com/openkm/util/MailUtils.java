/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
 * Copyright (c) 2006-2013 Paco Avila & Josep Llort
 * 
 * No bytes were intentionally harmed during the development of this application.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FlagTerm;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auxilii.msgparser.RecipientEntry;
import com.auxilii.msgparser.rtf.SimpleRTF2HTMLConverter;
import com.openkm.api.OKMDocument;
import com.openkm.api.OKMFolder;
import com.openkm.api.OKMMail;
import com.openkm.api.OKMRepository;
import com.openkm.automation.AutomationException;
import com.openkm.bean.Document;
import com.openkm.bean.Mail;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.ConversionException;
import com.openkm.core.DatabaseException;
import com.openkm.core.FileSizeExceededException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.MimeTypeConfig;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UnsupportedMimeTypeException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.VirusDetectedException;
import com.openkm.dao.MailAccountDAO;
import com.openkm.dao.bean.MailAccount;
import com.openkm.dao.bean.MailFilter;
import com.openkm.dao.bean.MailFilterRule;
import com.openkm.extension.core.ExtensionException;
import com.openkm.module.db.DbDocumentModule;
import com.openkm.module.db.DbMailModule;
import com.openkm.module.jcr.JcrDocumentModule;
import com.openkm.module.jcr.JcrMailModule;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MailUtils {
	private static Logger log = LoggerFactory.getLogger(MailUtils.class);
	public static final String NO_SUBJECT = "(Message without subject)";
	public static final String NO_BODY = "(Message without body)";
	public static final String MAIL_REGEX = "([_A-Za-z0-9-]+)(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})";
	
	/**
	 * Common properties for all mail sessions.
	 */
	public static Properties getProperties() {
		Properties props = System.getProperties();
		props.put("mail.imaps.ssl.trust", "*");
		return props;
	}
	
	/**
	 * Send mail without FROM addresses.
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param content The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendMessage(Collection<String> toAddress, String subject, String content)
			throws MessagingException {
		try {
			send(null, toAddress, subject, content, new ArrayList<String>());
		} catch (PathNotFoundException e) {
			log.warn(e.getMessage(), e);
		} catch (AccessDeniedException e) {
			log.warn(e.getMessage(), e);
		} catch (RepositoryException e) {
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Send mail without FROM addresses.
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param content The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendMessage(String toAddress, String subject, String content) throws MessagingException {
		try {
			ArrayList<String> toList = new ArrayList<String>();
			toList.add(toAddress);
			send(null, toList, subject, content, new ArrayList<String>());
		} catch (PathNotFoundException e) {
			log.warn(e.getMessage(), e);
		} catch (AccessDeniedException e) {
			log.warn(e.getMessage(), e);
		} catch (RepositoryException e) {
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Send mail without FROM addresses.
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param content The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendMessage(String fromAddress, List<String> toAddress, String subject, String content)
			throws MessagingException {
		try {
			send(fromAddress, toAddress, subject, content, new ArrayList<String>());
		} catch (PathNotFoundException e) {
			log.warn(e.getMessage(), e);
		} catch (AccessDeniedException e) {
			log.warn(e.getMessage(), e);
		} catch (RepositoryException e) {
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Send mail without FROM addresses.
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param content The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendMessage(String fromAddress, String toAddress, String subject, String content)
			throws MessagingException {
		try {
			ArrayList<String> toList = new ArrayList<String>();
			toList.add(toAddress);
			send(fromAddress, toList, subject, content, new ArrayList<String>());
		} catch (PathNotFoundException e) {
			log.warn(e.getMessage(), e);
		} catch (AccessDeniedException e) {
			log.warn(e.getMessage(), e);
		} catch (RepositoryException e) {
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Send document to non-registered OpenKM users
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param text The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendDocument(String fromAddress, List<String> toAddress, String subject, String text,
			String docPath) throws MessagingException, PathNotFoundException, AccessDeniedException, 
			RepositoryException, IOException, DatabaseException {
		send(fromAddress, toAddress, subject, text, Arrays.asList(new String[] { docPath }));
	}
	
	/**
	 * Send document to non-registered OpenKM users
	 * 
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param text The mail body.
	 * @throws MessagingException If there is any error.
	 */
	public static void sendDocuments(String fromAddress, List<String> toAddress, String subject, String text,
			List<String> docsPath) throws MessagingException, PathNotFoundException, AccessDeniedException, 
			RepositoryException, IOException, DatabaseException {
		send(fromAddress, toAddress, subject, text, docsPath);
	}
	
	/**
	 * Send mail with FROM addresses.
	 * 
	 * @param fromAddress Origin address.
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param text The mail body.
	 * @throws MessagingException If there is any error.
	 */
	private static void send(String fromAddress, Collection<String> toAddress, String subject, String text,
			Collection<String> docsPath) throws MessagingException, PathNotFoundException, AccessDeniedException, 
			RepositoryException, IOException, DatabaseException {
		log.debug("send({}, {}, {}, {}, {})", new Object[] { fromAddress, toAddress, subject, text, docsPath });
		List<File> tmpAttachments = new ArrayList<File>();
		
		try {
			// Need a temporal file for every attachment.
			for (int i=0; i<docsPath.size(); i++) {
				tmpAttachments.add(FileUtils.createTempFile());
			}
			
			MimeMessage m = create(fromAddress, toAddress, subject, text, docsPath, tmpAttachments);
			Transport.send(m);
		} finally {
			for (File tmpAttach : tmpAttachments) {
				FileUtils.deleteQuietly(tmpAttach);
			}
		}
		
		log.debug("send: void");
	}
	
	/**
	 * Forward a mail in the repository.
	 * 
	 * @param token Authentication token.
	 * @param fromAddress Origin address.
	 * @param toAddress Destination addresses.
	 * @param mailPath Path of the mail to be forwarded.
	 * @throws MessagingException If there is any error.
	 */
	public static void forwardMail(String token, String fromAddress, String toAddress, String message, String mailPath)
			throws MessagingException, PathNotFoundException, AccessDeniedException, RepositoryException,
			IOException, DatabaseException {
		ArrayList<String> toList = new ArrayList<String>();
		toList.add(toAddress);
		forwardMail(token, fromAddress, toList, message, mailPath);
	}
	
	/**
	 * Forward a mail in the repository.
	 * 
	 * @param token Authentication token.
	 * @param fromAddress Origin address.
	 * @param toAddress Destination addresses.
	 * @param mailId Path of the mail to be forwarded or its UUID.
	 * @throws MessagingException If there is any error.
	 */
	public static void forwardMail(String token, String fromAddress, Collection<String> toAddress, String message,
			String mailId) throws MessagingException, PathNotFoundException, AccessDeniedException,
			RepositoryException, IOException, DatabaseException {
		log.debug("forwardMail({}, {}, {}, {}, {})", new Object[] { token, fromAddress, toAddress, mailId });
		Mail mail = OKMMail.getInstance().getProperties(token, mailId);
		mail.setSubject("Fwd: " + mail.getSubject());
		
		if (Mail.MIME_TEXT.equals(mail.getMimeType())) {
			mail.setContent(message + "\n\n---------- Forwarded message ----------\n\n" + mail.getContent());
		} else if (Mail.MIME_HTML.equals(mail.getMimeType())) {
			mail.setContent(message + "<br/><br/>---------- Forwarded message ----------<br/><br/>" + mail.getContent());
		} else {
			log.warn("Email does not specify content MIME type");
		}
		
		if (fromAddress != null) {
			mail.setFrom(fromAddress);
		}
		
		if (toAddress != null && !toAddress.isEmpty()) {
			String[] to = (String[]) toAddress.toArray(new String[toAddress.size()]);
			mail.setTo(to);
		}
		
		MimeMessage m = create(token, mail);
		Transport.send(m);
		log.debug("forwardMail: void");
	}
	
	/**
	 * Create a mail.
	 * 
	 * @param fromAddress Origin address.
	 * @param toAddress Destination addresses.
	 * @param subject The mail subject.
	 * @param text The mail body.
	 * @throws MessagingException If there is any error.
	 */
	private static MimeMessage create(String fromAddress, Collection<String> toAddress, String subject, String text,
			Collection<String> docsPath, List<File> tmpAttachments) throws MessagingException, PathNotFoundException, 
			AccessDeniedException, RepositoryException, IOException, DatabaseException {
		log.debug("create({}, {}, {}, {}, {})", new Object[] { fromAddress, toAddress, subject, text, docsPath });
		Session mailSession = getMailSession();
		MimeMessage msg = new MimeMessage(mailSession);
		
		if (fromAddress != null) {
			InternetAddress from = new InternetAddress(fromAddress);
			msg.setFrom(from);
		} else {
			msg.setFrom();
		}
		
		InternetAddress[] to = new InternetAddress[toAddress.size()];
		int idx = 0;
		
		for (Iterator<String> it = toAddress.iterator(); it.hasNext();) {
			to[idx++] = new InternetAddress(it.next());
		}
		
		// Build a multiparted mail with HTML and text content for better SPAM behaviour
		Multipart content = new MimeMultipart();
		
		// HTML Part
		MimeBodyPart htmlPart = new MimeBodyPart();
		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
		htmlContent.append("<html>\n<head>\n");
		htmlContent.append("<meta content=\"text/html;charset=UTF-8\" http-equiv=\"Content-Type\"/>\n");
		htmlContent.append("</head>\n<body>\n");
		htmlContent.append(text);
		htmlContent.append("\n</body>\n</html>");
		htmlPart.setContent(htmlContent.toString(), "text/html;charset=UTF-8");
		htmlPart.setHeader("Content-Type", "text/html;charset=UTF-8");
		htmlPart.setDisposition(Part.INLINE);
		content.addBodyPart(htmlPart);
		idx = 0;
		
		if (docsPath != null) {
			for (String docPath : docsPath) {
				InputStream is = null;
				FileOutputStream fos = null;
				String docName = PathUtils.getName(docPath);
				
				try {
					final Document doc = OKMDocument.getInstance().getProperties(null, docPath);
					is = OKMDocument.getInstance().getContent(null, docPath, false);
					final File tmpAttch = tmpAttachments.get(idx++);
					fos = new FileOutputStream(tmpAttch);
					IOUtils.copy(is, fos);
					fos.flush();
					
					// Document attachment part
					MimeBodyPart docPart = new MimeBodyPart();
					DataSource source = new FileDataSource(tmpAttch.getPath()) {
						public String getContentType() {
							return doc.getMimeType();
						}
					};
					
					docPart.setDataHandler(new DataHandler(source));
					docPart.setFileName(MimeUtility.encodeText(docName));
					docPart.setDisposition(Part.ATTACHMENT);
					content.addBodyPart(docPart);
				} finally {
					IOUtils.closeQuietly(is);
					IOUtils.closeQuietly(fos);
				}
			}
		}
		
		msg.setHeader("MIME-Version", "1.0");
		msg.setHeader("Content-Type", content.getContentType());
		msg.addHeader("Charset", "UTF-8");
		msg.setRecipients(Message.RecipientType.TO, to);
		msg.setSubject(subject, "UTF-8");
		msg.setSentDate(new Date());
		msg.setContent(content);
		msg.saveChanges();
		
		log.debug("create: {}", msg);
		return msg;
	}
	
	/**
	 * Create a mail from a Mail object
	 */
	public static MimeMessage create(String token, Mail mail) throws MessagingException, PathNotFoundException,
			AccessDeniedException, RepositoryException, IOException, DatabaseException {
		log.debug("create({})", mail);
		Session mailSession = getMailSession();
		MimeMessage msg = new MimeMessage(mailSession);
		
		if (mail.getFrom() != null) {
			InternetAddress from = new InternetAddress(mail.getFrom());
			msg.setFrom(from);
		} else {
			msg.setFrom();
		}
		
		InternetAddress[] to = new InternetAddress[mail.getTo().length];
		int i = 0;
		
		for (String strTo : mail.getTo()) {
			to[i++] = new InternetAddress(strTo);
		}
		
		// Build a multiparted mail with HTML and text content for better SPAM behaviour
		MimeMultipart content = new MimeMultipart();
		
		if (Mail.MIME_TEXT.equals(mail.getMimeType())) {
			// Text part
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setText(mail.getContent());
			textPart.setHeader("Content-Type", "text/plain");
			textPart.setDisposition(Part.INLINE);
			content.addBodyPart(textPart);
		} else if (Mail.MIME_HTML.equals(mail.getMimeType())) {
			// HTML Part
			MimeBodyPart htmlPart = new MimeBodyPart();
			StringBuilder htmlContent = new StringBuilder();
			htmlContent.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
			htmlContent.append("<html>\n<head>\n");
			htmlContent.append("<meta content=\"text/html;charset=UTF-8\" http-equiv=\"Content-Type\"/>\n");
			htmlContent.append("</head>\n<body>\n");
			htmlContent.append(mail.getContent());
			htmlContent.append("\n</body>\n</html>");
			htmlPart.setContent(htmlContent.toString(), "text/html");
			htmlPart.setHeader("Content-Type", "text/html");
			htmlPart.setDisposition(Part.INLINE);
			content.addBodyPart(htmlPart);
		} else {
			log.warn("Email does not specify content MIME type");
			
			// Text part
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setText(mail.getContent());
			textPart.setHeader("Content-Type", "text/plain");
			textPart.setDisposition(Part.INLINE);
			content.addBodyPart(textPart);
		}
		
		for (Document doc : mail.getAttachments()) {
			InputStream is = null;
			FileOutputStream fos = null;
			String docName = PathUtils.getName(doc.getPath());
			
			try {
				is = OKMDocument.getInstance().getContent(token, doc.getPath(), false);
				File tmp = File.createTempFile("okm", ".tmp");
				fos = new FileOutputStream(tmp);
				IOUtils.copy(is, fos);
				fos.flush();
				
				// Document attachment part
				MimeBodyPart docPart = new MimeBodyPart();
				DataSource source = new FileDataSource(tmp.getPath());
				docPart.setDataHandler(new DataHandler(source));
				docPart.setFileName(docName);
				docPart.setDisposition(Part.ATTACHMENT);
				content.addBodyPart(docPart);
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(fos);
			}
		}
		
		msg.setHeader("MIME-Version", "1.0");
		msg.setHeader("Content-Type", content.getContentType());
		msg.addHeader("Charset", "UTF-8");
		msg.setRecipients(Message.RecipientType.TO, to);
		msg.setSubject(mail.getSubject(), "UTF-8");
		msg.setSentDate(new Date());
		msg.setContent(content);
		msg.saveChanges();
		
		log.debug("create: {}", msg);
		return msg;
	}
	
	/**
	 * 
	 */
	private static Session getMailSession() {
		Session mailSession = null;
		
		try {
			InitialContext initialContext = new InitialContext();
			Object obj = initialContext.lookup(Config.JNDI_BASE + "mail/OpenKM");
			mailSession = (Session) PortableRemoteObject.narrow(obj, Session.class);
		} catch (javax.naming.NamingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mailSession;
	}
	
	/**
	 * Import messages
	 * http://www.jguru.com/faq/view.jsp?EID=26898
	 * 
	 * == Using Unique Identifier (UIDL) ==
	 * Mail server assigns an unique identifier for every email in the same account. You can get as UIDL
	 * for every email by MailInfo.UIDL property. To avoid receiving the same email twice, the best way is
	 * storing the UIDL of email retrieved to a text file or database. Next time before you retrieve email,
	 * compare your local uidl list with remote uidl. If this uidl exists in your local uidl list, don't
	 * receive it; otherwise receive it.
	 * 
	 * == Different property of UIDL in POP3 and IMAP4 ==
	 * UIDL is always unique in IMAP4 and it is always an incremental integer. UIDL in POP3 can be any valid
	 * asc-ii characters, and an UIDL may be reused by POP3 server if email with this UIDL has been deleted
	 * from the server. Hence you are advised to remove the uidl from your local uidl list if that uidl is
	 * no longer exist on the POP3 server.
	 * 
	 * == Remarks ==
	 * You should create different local uidl list for different email account, because the uidl is only
	 * unique for the same account.
	 */
	public static String importMessages(String token, MailAccount ma) throws PathNotFoundException,
			ItemExistsException, VirusDetectedException, AccessDeniedException, RepositoryException, DatabaseException,
			UserQuotaExceededException, ExtensionException, AutomationException {
		log.debug("importMessages({}, {})", new Object[] { token, ma });
		Session session = Session.getDefaultInstance(getProperties());
		String exceptionMessage = null;
		
		try {
			// Open connection
			Store store = session.getStore(ma.getMailProtocol());
			store.connect(ma.getMailHost(), ma.getMailUser(), ma.getMailPassword());
			
			Folder folder = store.getFolder(ma.getMailFolder());
			folder.open(Folder.READ_WRITE);
			Message messages[] = null;
			
			if (folder instanceof IMAPFolder) {
				// IMAP folder UIDs begins at 1 and are supposed to be sequential.
				// Each folder has its own UIDs sequence, not is a global one.
				messages = ((IMAPFolder) folder).getMessagesByUID(ma.getMailLastUid() + 1, UIDFolder.LASTUID);
			} else {
				messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));	
			}
			
			for (int i = 0; i < messages.length; i++) {
				Message msg = messages[i];
				log.info(i + ": " + msg.getFrom()[0] + " " + msg.getSubject() + " " + msg.getContentType());
				log.info("Received: " + msg.getReceivedDate());
				log.info("Sent: " + msg.getSentDate());
				log.debug("{} -> {} - {}", new Object[] { i, msg.getSubject(), msg.getReceivedDate() });
				com.openkm.bean.Mail mail = messageToMail(msg);
				
				if (ma.getMailFilters().isEmpty()) {
					log.debug("Import in compatibility mode");
					String mailPath = getUserMailPath(ma.getUser());
					importMail(token, mailPath, true, folder, msg, ma, mail);
				} else {
					for (MailFilter mf : ma.getMailFilters()) {
						log.debug("MailFilter: {}", mf);
						
						if (checkRules(mail, mf.getFilterRules())) {
							String mailPath = mf.getPath();
							importMail(token, mailPath, mf.isGrouping(), folder, msg, ma, mail);
						}
					}
				}
				
				// Set message as seen
				if (ma.isMailMarkSeen()) {
					msg.setFlag(Flags.Flag.SEEN, true);
				} else {
					msg.setFlag(Flags.Flag.SEEN, false);
				}
				
				// Delete read mail if requested
				if (ma.isMailMarkDeleted()) {
					msg.setFlag(Flags.Flag.DELETED, true);
				}
				
				// Set lastUid
				if (folder instanceof IMAPFolder) {
					long msgUid = ((IMAPFolder) folder).getUID(msg);
					log.info("Message UID: {}", msgUid);
					ma.setMailLastUid(msgUid);
					MailAccountDAO.update(ma);
				}
			}
			
			// Close connection
			log.debug("Expunge: {}", ma.isMailMarkDeleted());
			folder.close(ma.isMailMarkDeleted());
			store.close();
		} catch (NoSuchProviderException e) {
			log.error(e.getMessage(), e);
			exceptionMessage = e.getMessage();
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
			exceptionMessage = e.getMessage();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			exceptionMessage = e.getMessage();
		}
		
		log.debug("importMessages: {}", exceptionMessage);
		return exceptionMessage;
	}
	
	/**
	 * Convert Mime Message to Mail
	 */
	public static Mail messageToMail(Message msg) throws MessagingException, IOException {
		com.openkm.bean.Mail mail = new com.openkm.bean.Mail();
		Calendar receivedDate = Calendar.getInstance();
		Calendar sentDate = Calendar.getInstance();
		
		// Can be void
		if (msg.getReceivedDate() != null) {
			receivedDate.setTime(msg.getReceivedDate());
		}
		
		// Can be void
		if (msg.getSentDate() != null) {
			sentDate.setTime(msg.getSentDate());
		}
		
		String body = getText(msg);
		
		// log.info("getText: "+body);
		if (body.charAt(0) == 'H') {
			mail.setMimeType(MimeTypeConfig.MIME_HTML);
		} else if (body.charAt(0) == 'T') {
			mail.setMimeType(MimeTypeConfig.MIME_TEXT);
		} else {
			mail.setMimeType(MimeTypeConfig.MIME_UNDEFINED);
		}
		
		String content = body.substring(1);
		
		// Need to replace 0x00 because PostgreSQL does not accept string containing 0x00
		content = FormatUtil.fixUTF8(content);
		
		// Need to remove Unicode surrogate because of MySQL => SQL Error: 1366, SQLState: HY000
		content = FormatUtil.trimUnicodeSurrogates(content);
		
		mail.setContent(content);
		
		if (msg.getFrom().length > 0) {
			mail.setFrom(MimeUtility.decodeText(msg.getFrom()[0].toString()));
		}
		
		mail.setSize(msg.getSize());
		mail.setSubject((msg.getSubject() == null || msg.getSubject().isEmpty()) ? NO_SUBJECT : msg.getSubject());
		mail.setTo(addressToString(msg.getRecipients(Message.RecipientType.TO)));
		mail.setCc(addressToString(msg.getRecipients(Message.RecipientType.CC)));
		mail.setBcc(addressToString(msg.getRecipients(Message.RecipientType.BCC)));
		mail.setReceivedDate(receivedDate);
		mail.setSentDate(sentDate);
		
		return mail;
	}
	
	/**
	 * Convert Outlook Message to Mail
	 */
	public static Mail messageToMail(com.auxilii.msgparser.Message msg) throws MessagingException, IOException {
		com.openkm.bean.Mail mail = new com.openkm.bean.Mail();
		Calendar receivedDate = Calendar.getInstance();
		Calendar sentDate = Calendar.getInstance();
		
		// Can be void
		if (msg.getDate() != null) {
			receivedDate.setTime(msg.getDate());
		}
		
		// Can be void
		if (msg.getCreationDate() != null) {
			sentDate.setTime(msg.getCreationDate());
		}
		
		if (msg.getBodyRTF() != null) {
			SimpleRTF2HTMLConverter converter = new SimpleRTF2HTMLConverter();
			mail.setMimeType(MimeTypeConfig.MIME_HTML);
			mail.setContent(converter.rtf2html(msg.getBodyRTF()));
		} else if (msg.getBodyHTML() != null) {
			mail.setMimeType(MimeTypeConfig.MIME_HTML);
			mail.setContent(msg.getBodyHTML());
		} else if (msg.getBodyText() != null) {
			mail.setMimeType(MimeTypeConfig.MIME_TEXT);
			mail.setContent(msg.getBodyText());				
		} else {
			mail.setMimeType(MimeTypeConfig.MIME_UNDEFINED);
		}
		
		if (msg.getToRecipient() != null) {
			mail.setTo(new String[] { msg.getToRecipient().getToName() + " <" + msg.getToRecipient().getToEmail() + ">" });
		}
		
		mail.setSize(mail.getContent().length());
		mail.setSubject((msg.getSubject() == null || msg.getSubject().isEmpty()) ? NO_SUBJECT : msg.getSubject());
		mail.setFrom(msg.getFromName() + " <" + msg.getFromEmail() + ">");
		mail.setCc(recipientToString(msg.getCcRecipients()));
		mail.setBcc(recipientToString(msg.getBccRecipients()));
		mail.setReceivedDate(receivedDate);
		mail.setSentDate(sentDate);
		
		return mail;
	}

	/**
	 * Import mail into OpenKM repository
	 */
	public static void importMail(String token, String mailPath, boolean grouping, Folder folder, Message msg,
			MailAccount ma, com.openkm.bean.Mail mail) throws DatabaseException, RepositoryException,
			AccessDeniedException, ItemExistsException, PathNotFoundException, MessagingException,
			VirusDetectedException, UserQuotaExceededException, IOException, ExtensionException, AutomationException {
		OKMRepository okmRepository = OKMRepository.getInstance();
		String path = grouping ? createGroupPath(token, mailPath, mail.getReceivedDate()) : mailPath;
		
		if (ma.getMailProtocol().equals(MailAccount.PROTOCOL_POP3)
				|| ma.getMailProtocol().equals(MailAccount.PROTOCOL_POP3S)) {
			mail.setPath(path + "/" + ((POP3Folder) folder).getUID(msg) + "-" +
				PathUtils.escape((msg.getSubject() == null || msg.getSubject().isEmpty()) ? NO_SUBJECT : msg.getSubject()));
		} else {
			mail.setPath(path + "/" + ((IMAPFolder) folder).getUID(msg) + "-" +
				PathUtils.escape((msg.getSubject() == null || msg.getSubject().isEmpty()) ? NO_SUBJECT : msg.getSubject()));
		}
		
		String newMailPath = PathUtils.getParent(mail.getPath()) + "/" + PathUtils.escape(PathUtils.getName(mail.getPath()));
		log.debug("newMailPath: {}", newMailPath);
		
		if (!okmRepository.hasNode(token, newMailPath)) {
			if (Config.REPOSITORY_NATIVE) {
				new DbMailModule().create(token, mail, ma.getUser());
			} else {
				new JcrMailModule().create(token, mail, ma.getUser());
			}
			
			try {
				addAttachments(token, mail, msg, ma.getUser());
			} catch (UnsupportedMimeTypeException e) {
				log.warn(e.getMessage(), e);
			} catch (FileSizeExceededException e) {
				log.warn(e.getMessage(), e);
			} catch (UserQuotaExceededException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Check mail import rules
	 */
	public static boolean checkRules(com.openkm.bean.Mail mail, Set<MailFilterRule> filterRules) {
		log.info("checkRules({}, {})", mail, filterRules);
		boolean ret = true;
		
		for (MailFilterRule fr : filterRules) {
			log.info("FilterRule: {}", fr);
			
			if (fr.isActive()) {
				if (MailFilterRule.FIELD_FROM.equals(fr.getField())) {
					if (MailFilterRule.OPERATION_CONTAINS.equals(fr.getOperation())) {
						ret &= mail.getFrom().toLowerCase().contains(fr.getValue().toLowerCase());
					} else if (MailFilterRule.OPERATION_EQUALS.equals(fr.getOperation())) {
						ret &= mail.getFrom().equalsIgnoreCase(fr.getValue());
					}
				} else if (MailFilterRule.FIELD_TO.equals(fr.getField())) {
					if (MailFilterRule.OPERATION_CONTAINS.equals(fr.getOperation())) {
						for (int j = 0; j < mail.getTo().length; j++) {
							ret &= mail.getTo()[j].toLowerCase().contains(fr.getValue().toLowerCase());
						}
					} else if (MailFilterRule.OPERATION_EQUALS.equals(fr.getOperation())) {
						for (int j = 0; j < mail.getTo().length; j++) {
							ret &= mail.getTo()[j].equalsIgnoreCase(fr.getValue());
						}
					}
				} else if (MailFilterRule.FIELD_SUBJECT.equals(fr.getField())) {
					if (MailFilterRule.OPERATION_CONTAINS.equals(fr.getOperation())) {
						ret &= mail.getSubject().toLowerCase().contains(fr.getValue().toLowerCase());
					} else if (MailFilterRule.OPERATION_EQUALS.equals(fr.getOperation())) {
						ret &= mail.getSubject().equalsIgnoreCase(fr.getValue());
					}
				} else if (MailFilterRule.FIELD_CONTENT.equals(fr.getField())) {
					if (MailFilterRule.OPERATION_CONTAINS.equals(fr.getOperation())) {
						ret &= mail.getContent().toLowerCase().contains(fr.getValue().toLowerCase());
					} else if (MailFilterRule.OPERATION_EQUALS.equals(fr.getOperation())) {
						ret &= mail.getContent().equalsIgnoreCase(fr.getValue());
					}
				}
			}
			
			log.info("FilterRule: {}", ret);
		}
		
		log.info("checkRules: {}", ret);
		return ret;
	}
	
	/**
	 * Create mail path
	 */
	private static String createGroupPath(String token, String mailPath, Calendar receivedDate)
			throws DatabaseException, RepositoryException, AccessDeniedException, ItemExistsException,
			PathNotFoundException, ExtensionException, AutomationException {
		log.debug("createGroupPath({}, {})", new Object[] { mailPath, receivedDate });
		OKMRepository okmRepository = OKMRepository.getInstance();
		String path = mailPath + "/" + receivedDate.get(Calendar.YEAR);
		OKMFolder okmFolder = OKMFolder.getInstance();
		
		if (!okmRepository.hasNode(token, path)) {
			com.openkm.bean.Folder fld = new com.openkm.bean.Folder();
			fld.setPath(path);
			okmFolder.create(token, fld);
		}
		
		path += "/" + (receivedDate.get(Calendar.MONTH) + 1);
		
		if (!okmRepository.hasNode(token, path)) {
			com.openkm.bean.Folder fld = new com.openkm.bean.Folder();
			fld.setPath(path);
			okmFolder.create(token, fld);
		}
		
		path += "/" + receivedDate.get(Calendar.DAY_OF_MONTH);
		
		if (!okmRepository.hasNode(token, path)) {
			com.openkm.bean.Folder fld = new com.openkm.bean.Folder();
			fld.setPath(path);
			okmFolder.create(token, fld);
		}
		
		log.debug("createGroupPath: {}", path);
		return path;
	}
	
	/**
	 * Get text from message
	 */
	private static String getText(Part p) throws MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			Object obj = p.getContent();
			String str = NO_BODY;
			
			if (obj instanceof InputStream) {
				InputStream is  = (InputStream) obj;
				StringWriter writer = new StringWriter();
				IOUtils.copy(is, writer, "UTF-8");
				str = writer.toString();
			} else {
				str = (String) obj;
			}
			
			if (p.isMimeType("text/html")) {
				return "H" + str;
			} else if (p.isMimeType("text/plain")) {
				return "T" + str;
			} else {
				// Otherwise let's set as text/plain
				return "T" + str;
			}
		} else if (p.isMimeType("multipart/alternative")) {
			// prefer html over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = "T" + NO_BODY;
			// log.info("Mime Parts: {}", mp.getCount());
			
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				
				if (bp.isMimeType("text/plain")) {
					text = getText(bp);
				} else if (bp.isMimeType("text/html")) {
					text = getText(bp);
					break;
				} else {
					text = getText(bp);
				}
			}
			
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				
				if (s != null)
					return s;
			}
		}
		
		return "T" + NO_BODY;
	}
	
	/**
	 * Add attachments to an imported mail.
	 */
	public static void addAttachments(String token, com.openkm.bean.Mail mail, Part p, String userId)
			throws MessagingException, IOException, UnsupportedMimeTypeException, FileSizeExceededException,
			UserQuotaExceededException, VirusDetectedException, ItemExistsException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException, ExtensionException, AutomationException {
		if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			int count = mp.getCount();
			
			for (int i = 1; i < count; i++) {
				BodyPart bp = mp.getBodyPart(i);
				
				if (bp.getFileName() != null) {
					String name = MimeUtility.decodeText(bp.getFileName());
					String fileName = FileUtils.getFileName(name);
					String fileExtension = FileUtils.getFileExtension(name);
					String testName = name;
					
					// Test if already exists a document with the same name in the mail
					for (int j = 1; OKMRepository.getInstance().hasNode(token, mail.getPath() + "/" + testName); j++) {
						// log.info("Trying with: {}", testName);
						testName = fileName + " (" + j + ")." + fileExtension;
					}
					
					Document attachment = new Document();
					String mimeType = MimeTypeConfig.mimeTypes.getContentType(bp.getFileName().toLowerCase());
					attachment.setMimeType(mimeType);
					attachment.setPath(mail.getPath() + "/" + testName);
					InputStream is = bp.getInputStream();
					
					if (Config.REPOSITORY_NATIVE) {
						new DbDocumentModule().create(token, attachment, is, bp.getSize(), userId);
					} else {
						new JcrDocumentModule().create(token, attachment, is, userId);
					}
					
					is.close();
				}
			}
		}
	}
	
	/**
	 * Conversion from array of Addresses to array of Strings.
	 */
	private static String[] addressToString(Address[] addresses) {
		ArrayList<String> list = new ArrayList<String>();
		
		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				list.add(addresses[i].toString());
			}
		}
		
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	/**
	 * Conversion from array of Recipient to array of Strings.
	 */
	private static String[] recipientToString(List<RecipientEntry> recipEntries) {
		ArrayList<String> list = new ArrayList<String>();
		
		if (recipEntries != null) {
			for (RecipientEntry re : recipEntries) {
				list.add(re.getToName() + " <" + re.getToEmail() + ">");
			}
		}
		
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	/**
	 * 
	 */
	public static String getUserMailPath(String user) {
		return "/" + Repository.MAIL + "/" + user;
	}
	
	/**
	 * User tinyurl service as url shorter Depends on commons-httpclient:commons-httpclient:jar:3.0 because of
	 * org.apache.jackrabbit:jackrabbit-webdav:jar:1.6.4
	 */
	public static String getTinyUrl(String fullUrl) throws HttpException, IOException {
		HttpClient httpclient = new HttpClient();
		
		// Prepare a request object
		HttpMethod method = new GetMethod("http://tinyurl.com/api-create.php");
		method.setQueryString(new NameValuePair[] { new NameValuePair("url", fullUrl) });
		httpclient.executeMethod(method);
		InputStreamReader isr = new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8");
		StringWriter sw = new StringWriter();
		int c;
		while ((c = isr.read()) != -1)
			sw.write(c);
		isr.close();
		method.releaseConnection();
		
		return sw.toString();
	}
	
	/**
	 * Test IMAP connection
	 */
	public static void testConnection(MailAccount ma) throws IOException {
		log.debug("testConnection({})", ma);
		Session session = Session.getDefaultInstance(getProperties());
		Store store = null;
		Folder folder = null;
		
		try {
			store = session.getStore(ma.getMailProtocol());
			store.connect(ma.getMailHost(), ma.getMailUser(), ma.getMailPassword());
			folder = store.getFolder(ma.getMailFolder());
			folder.open(Folder.READ_WRITE);
			folder.close(false);
		} catch (NoSuchProviderException e) {
			throw new IOException(e.getMessage());
		} catch (MessagingException e) {
			throw new IOException(e.getMessage());
		} finally {
			// Try to close folder
			if (folder != null && folder.isOpen()) {
				try {
					folder.close(false);
				} catch (MessagingException e) {
					throw new IOException(e.getMessage());
				}
			}
			
			// Try to close store
			if (store != null) {
				try {
					store.close();
				} catch (MessagingException e) {
					throw new IOException(e.getMessage());
				}
			}
		}
		
		log.debug("testConnection: void");
	}
	
	/**
	 * Generate HTML with mail object data and contents
	 */
	public static String mail2html(Mail mail) throws ConversionException {
		HashMap<String, String> hm = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < mail.getTo().length - 1; i++) {
			sb.append(mail.getTo()[i]).append(", ");
		}
		
		sb.append(mail.getTo()[mail.getTo().length - 1]);
		hm.put("mailTo", sb.toString());
		hm.put("mailFrom", mail.getFrom());
		hm.put("mailSubject", mail.getSubject());
		hm.put("mailContent", mail.getContent());
		StringWriter sw = new StringWriter();
		InputStreamReader isr = null;
		
		try {
			isr = new InputStreamReader(MailUtils.class.getResourceAsStream("mail.ftl"));
			Template tpl = new Template("mail", isr, TemplateUtils.getConfig());
			tpl.process(hm, sw);
		} catch (IOException e) {
			throw new ConversionException("IOException: " + e.getMessage(), e);
		} catch (TemplateException e) {
			throw new ConversionException("TemplateException: " + e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(sw);
			IOUtils.closeQuietly(isr);
		}
		
		return sw.toString();
	}
	
	/**
	 * Convert string with mails to list.
	 */
	public static List<String> parseMailList(String mails) {
		List<String> mailList = new ArrayList<String>();
		
		if (mails != null && !mails.isEmpty()) {
			for (StringTokenizer st = new StringTokenizer(mails, ","); st.hasMoreTokens(); ) {
				String mail = st.nextToken().trim();
				
				if (mail.matches(MAIL_REGEX)) {
					mailList.add(mail);
				}
			}
		}
		
		return  mailList;
	}
}
