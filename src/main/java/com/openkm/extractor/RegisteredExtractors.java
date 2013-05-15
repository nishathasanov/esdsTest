/**
 * ESDS, Open Document Management System (http://www.openkm.com)
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

package com.openkm.extractor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.openkm.bean.Document;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.module.db.stuff.PersistentFile;
import com.openkm.module.jcr.JcrRepositoryModule;
import com.openkm.module.jcr.stuff.apache.JackrabbitTextExtractor;
import com.openkm.util.UserActivity;

/**
 * @author pavila
 */
public class RegisteredExtractors {
	private static Logger log = LoggerFactory.getLogger(RegisteredExtractors.class);
	private static Map<String, TextExtractor> engine = new HashMap<String, TextExtractor>();
	private static JackrabbitTextExtractor jte = new JackrabbitTextExtractor();
	private static final int MIN_EXTRACTION = 16;
	private static final boolean EXPERIMENTAL = true;
	
	/**
	 * Initialize text extractors from REGISTERED_TEXT_EXTRACTORS
	 */
	public static synchronized void init() {
		log.info("Initializing text extractors");
		
		if (EXPERIMENTAL) {
			for (String clazz : Config.REGISTERED_TEXT_EXTRACTORS) {
				try {
					Object obj = Class.forName(clazz).newInstance();
					
					if (obj instanceof TextExtractor) {
						TextExtractor te = (TextExtractor) obj;
						
						for (String contType : te.getContentTypes()) {
							log.info("Registering {} for '{}'", te.getClass().getCanonicalName(), contType);
							engine.put(contType, te);
						}
					} else {
						log.warn("Unknown text extractor class: {}", clazz);
					}
				} catch (ClassNotFoundException e) {
					log.warn("Extractor class not found: {}", clazz, e);
				} catch (LinkageError e) {
					log.warn("Extractor dependency not found: {}", clazz, e);
				} catch (IllegalAccessException e) {
					log.warn("Extractor constructor not accessible: {}", clazz, e);
				} catch (InstantiationException e) {
					log.warn("Extractor instantiation failed: {}", clazz, e);
				}
			}
		} else {
			jte = new JackrabbitTextExtractor(Config.REGISTERED_TEXT_EXTRACTORS);
		}
	}
	
	/**
	 * Return registered content types
	 */
	public static String[] getContentTypes() {
		if (EXPERIMENTAL) {
			return engine.keySet().toArray(new String[engine.keySet().size()]);
		} else {
			return jte.getContentTypes();
		}
	}
	
	/**
	 * Return guessed text extractor
	 */
	public static TextExtractor getTextExtractor(String mimeType) {
		if (EXPERIMENTAL) {
			return engine.get(mimeType);
		} else {
			return null;
		}
	}
	
	/**
	 * Check for registered text extractor
	 */
	public static boolean isRegistered(String className) {
		List<String> classes = new ArrayList<String>();
		
		if (Config.MANAGED_TEXT_EXTRACTION || Config.REPOSITORY_NATIVE) {
			classes = Config.REGISTERED_TEXT_EXTRACTORS;
		} else {
			try {
				RepositoryConfig rc = JcrRepositoryModule.getRepositoryConfig();
				WorkspaceConfig wc = rc.getWorkspaceConfig(rc.getDefaultWorkspaceName());
				SearchConfig sc = wc.getSearchConfig();
				
				if (sc != null) {
					String tfClasses = (String) sc.getParameters().get("textFilterClasses");
					StringTokenizer tokenizer = new StringTokenizer(tfClasses, ", \t\n\r\f");
					
					while (tokenizer.hasMoreTokens()) {
						String clazz = tokenizer.nextToken();
						classes.add(clazz);
					}
				}
			} catch (ConfigurationException e) {
				log.warn(e.getMessage(), e);
			}
		}
			
		for (String name : classes) {
			if (name.equals(className)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Extract text to be indexed
	 */
	public static String getText(String docPath, String mimeType, String encoding, InputStream isContent) 
			throws IOException {
		log.debug("getText({}, {}, {}, {})", new Object[] { docPath, mimeType, encoding, isContent });
		String failureMessage = "Unknown error";
		boolean failure = false;
		String text = null;
		
		try {
			text = getText(mimeType, encoding, isContent);
			
			// Check for minimum text extraction size
			if (text.length() < MIN_EXTRACTION) {
				failureMessage = "Too few text extracted";
				failure = true;
			}
		} catch (Exception e) {
			log.warn("Text extraction failure: {}", e.getMessage());
			failureMessage = e.getMessage();
			failure = true;
		}
		
		if (failure) {
			throw new IOException(failureMessage);
		}
		
		log.debug("getText: {}", text);
		return text;
	}
	
	/**
	 * Extract text to be indexed
	 */
	public static String getText(String mimeType, String encoding, InputStream isContent) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(isContent);
		String text = null;
		
		if (EXPERIMENTAL) {
			TextExtractor te = engine.get(mimeType);
			
			if (te != null) {
				if (mimeType.startsWith("text/") && encoding == null) {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(bis);
					CharsetMatch cm = detector.detect();
					encoding = cm.getName();
				}
				
				Reader rd = te.extractText(bis, mimeType, encoding);
				text = IOUtils.toString(rd);
			} else {
				throw new IOException("Full text indexing of '" + mimeType + "' is not supported");
			}
		} else {
			Reader rd = jte.extractText(bis, mimeType, encoding);
			text = IOUtils.toString(rd);
		}
		
		IOUtils.closeQuietly(bis);
		return text;
	}
	
	//
	// JCR Methods
	//
	
	/**
	 * Extract text to be indexed
	 */
	private static String getJcrText(javax.jcr.Node node, String mimeType, String encoding, InputStream isContent)
			throws javax.jcr.ValueFormatException, javax.jcr.PathNotFoundException, javax.jcr.RepositoryException,
			IOException {
		log.debug("getJcrText({}, {}, {}, {})", new Object[] { node, mimeType, encoding, isContent });
		String text = null;
		
		try {
			text = getText(node.getPath(), mimeType, encoding, isContent);
		} catch (IOException e) {
			if (node != null) {
				log.warn("There was a problem extracting text from '{}'", node.getPath());
				UserActivity.log(node.getSession().getUserID(), "MISC_TEXT_EXTRACTION_FAILURE", node.getUUID(), node.getPath(), e.getMessage());
			}
		}
		
		log.debug("getJcrText: {}", text);
		return text;
	}
	
	/**
	 * EXPERIMENTAL
	 */
	public static void index(javax.jcr.Node docNode, javax.jcr.Node contNode, String mimeType)
			throws javax.jcr.ValueFormatException, javax.jcr.PathNotFoundException, javax.jcr.RepositoryException,
			IOException {
		InputStream in = null;
		String text = null;
		
		try {
			in = contNode.getProperty(JcrConstants.JCR_DATA).getStream();
			text = getJcrText(docNode, mimeType, "UTF-8", in);
			
			try {
				contNode.setProperty(Document.TEXT, text);
			} catch (Exception e) {
				log.warn("Text extraction failure - {}: {}", e.getClass().getName(), e.getMessage());
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	//
	// DB Methods
	//
	
	/**
	 * Extract text to be indexed
	 */
	@SuppressWarnings("unused")
	private static String getDbText(String docUuid, String mimeType, String encoding, InputStream isContent)
			throws IOException, PathNotFoundException, DatabaseException {
		log.debug("getDbText({}, {}, {}, {})", new Object[] { docUuid, mimeType, encoding, isContent });
		String text = null;
		
		try {
			text = getText(null, mimeType, encoding, isContent);
		} catch (IOException e) {
			if (docUuid != null) {
				String nodePath = NodeBaseDAO.getInstance().getPathFromUuid(docUuid);
				log.warn("There was a problem extracting text from '{}'", nodePath);
				UserActivity.log(Config.SYSTEM_USER, "MISC_TEXT_EXTRACTION_FAILURE", docUuid, nodePath, e.getMessage());
			}
		}
		
		log.debug("getDbText: {}", text);
		return text;
	}
	
	/**
	 * Sample lazy text extraction.
	 * 
	 * @see com.openkm.module.db.stuff.LazyField
	 */
	public static String getText(PersistentFile persistentFile) throws IOException {
		InputStream isContent = null;
		String text = null;
		
		try {
			isContent = persistentFile.getInputStream();
			text = getText("text/plain", "UTF-8", isContent);
			return text;
		} finally {
			IOUtils.closeQuietly(isContent);
		}
	}
}
