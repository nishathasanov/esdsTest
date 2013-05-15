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

package com.openkm.servlet.frontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.api.OKMDocument;
import com.openkm.api.OKMRepository;
import com.openkm.bean.Document;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.MimeTypeConfig;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.frontend.client.OKMException;
import com.openkm.frontend.client.constants.service.ErrorCode;
import com.openkm.util.ArchiveUtils;
import com.openkm.util.FileUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.WebUtils;
import com.openkm.util.impexp.RepositoryExporter;
import com.openkm.util.impexp.TextInfoDecorator;

/**
 * Documento download servlet
 */
public class DownloadServlet extends OKMHttpServlet {
	private static Logger log = LoggerFactory.getLogger(DownloadServlet.class);
	private static final long serialVersionUID = 1L;
	private static final boolean exportZip = true;
	private static final boolean exportJar = false;
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("service({}, {})", request, response);
		request.setCharacterEncoding("UTF-8");
		String path = request.getParameter("path");
		String uuid = request.getParameter("uuid");
		String[] uuidList = request.getParameterValues("uuidList");
		String[] pathList = request.getParameterValues("pathList");
		String checkout = request.getParameter("checkout");
		String ver = request.getParameter("ver");
		boolean export = request.getParameter("export") != null;
		boolean inline = request.getParameter("inline") != null;
		File tmp = File.createTempFile("okm", ".tmp");
		Document doc = null;
		InputStream is = null;
		updateSessionManager(request);
		
		try {
			// Now an document can be located by UUID
			if (uuid != null && !uuid.equals("")) {
				path = OKMRepository.getInstance().getNodePath(null, uuid);
			} else if (path != null) {
				path = new String(path.getBytes("ISO-8859-1"), "UTF-8");
			}
			
			if (export) {
				if (exportZip) {
					String fileName = "export.zip";
					
					// Get document
					FileOutputStream os = new FileOutputStream(tmp);
					
					if (path != null) {
						exportFolderAsZip(path, os);
						fileName = PathUtils.getName(path) + ".zip";
					} else if (uuidList != null || pathList != null) {
						// Export into a zip file multiple documents
						List<String> paths = new ArrayList<String>();
						
						if (uuidList != null) {
							for (String uuidElto : uuidList) {
								String foo = new String(uuidElto.getBytes("ISO-8859-1"), "UTF-8");
								paths.add(OKMRepository.getInstance().getNodePath(null, foo));
							}
						} else if (pathList != null) {
							for (String pathElto : pathList) {
								String foo = new String(pathElto.getBytes("ISO-8859-1"), "UTF-8");
								paths.add(foo);
							}
						}
						
						fileName = PathUtils.getName(PathUtils.getParent(paths.get(0)));
						exportDocumentsAsZip(paths, os, fileName);
						fileName += ".zip";
					}
					
					os.flush();
					os.close();
					is = new FileInputStream(tmp);
					
					// Send document
					WebUtils.sendFile(request, response, fileName, MimeTypeConfig.MIME_ZIP, inline, is);
				} else if (exportJar) {
					// Get document
					FileOutputStream os = new FileOutputStream(tmp);
					exportFolderAsJar(path, os);
					os.flush();
					os.close();
					is = new FileInputStream(tmp);
					
					// Send document
					String fileName = PathUtils.getName(path) + ".jar";
					WebUtils.sendFile(request, response, fileName, "application/x-java-archive", inline, is);
					
				}
			} else {
				// Get document
				doc = OKMDocument.getInstance().getProperties(null, path);
				
				if (ver != null && !ver.equals("")) {
					is = OKMDocument.getInstance().getContentByVersion(null, path, ver);
				} else {
					is = OKMDocument.getInstance().getContent(null, path, checkout != null);
				}
				
				// Send document
				String fileName = PathUtils.getName(doc.getPath());
				WebUtils.sendFile(request, response, fileName, doc.getMimeType(), inline, is);
			}
		} catch (PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new ServletException(new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDownloadService,
					ErrorCode.CAUSE_PathNotFound), e.getMessage()));
		} catch (RepositoryException e) {
			log.warn(e.getMessage(), e);
			throw new ServletException(new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDownloadService,
					ErrorCode.CAUSE_Repository), e.getMessage()));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new ServletException(new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDownloadService, ErrorCode.CAUSE_IO),
					e.getMessage()));
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new ServletException(new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDownloadService,
					ErrorCode.CAUSE_Database), e.getMessage()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ServletException(new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDownloadService,
					ErrorCode.CAUSE_General), e.getMessage()));
		} finally {
			IOUtils.closeQuietly(is);
			FileUtils.deleteQuietly(tmp);
		}
		
		log.debug("service: void");
	}
	
	/**
	 * Generate a zip file from a repository folder path
	 */
	private void exportFolderAsZip(String fldPath, OutputStream os) throws PathNotFoundException, AccessDeniedException,
			RepositoryException, ArchiveException, ParseException, NoSuchGroupException, IOException, DatabaseException,
			MessagingException {
		log.debug("exportFolderAsZip({}, {})", fldPath, os);
		StringWriter out = new StringWriter();
		File tmp = null;
		
		try {
			tmp = FileUtils.createTempDir();
			
			// Export files
			RepositoryExporter.exportDocuments(null, fldPath, tmp, false, false, out, new TextInfoDecorator(fldPath));
			
			// Zip files
			ArchiveUtils.createZip(tmp, PathUtils.getName(fldPath), os);
		} catch (IOException e) {
			log.error("Error exporting zip", e);
			throw e;
		} finally {
			IOUtils.closeQuietly(out);
			
			if (tmp != null) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(tmp);
				} catch (IOException e) {
					log.error("Error deleting temporal directory", e);
					throw e;
				}
			}
		}
		
		log.debug("exportFolderAsZip: void");
	}
	
	/**
	 * Generate a zip file from a list of documents
	 */
	private void exportDocumentsAsZip(List<String> paths, OutputStream os, String zipname) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, ArchiveException, ParseException, NoSuchGroupException, IOException,
			DatabaseException {
		log.debug("exportDocumentsAsZip({}, {})", paths, os);
		StringWriter out = new StringWriter();
		File tmp = null;
		
		try {
			tmp = FileUtils.createTempDir();
			File fsPath = new File(tmp.getPath());
			
			// Export files
			for (String sourcePath : paths) {
				String destPath = fsPath.getPath() + File.separator + PathUtils.getName(sourcePath).replace(':', '_');
				RepositoryExporter.exportDocument(null, destPath, sourcePath, false, false, out, new TextInfoDecorator(
						sourcePath));
			}
			
			// Zip files
			ArchiveUtils.createZip(tmp, zipname, os);
		} catch (IOException e) {
			log.error("Error exporting zip", e);
			throw e;
		} finally {
			IOUtils.closeQuietly(out);
			
			if (tmp != null) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(tmp);
				} catch (IOException e) {
					log.error("Error deleting temporal directory", e);
					throw e;
				}
			}
		}
		
		log.debug("exportDocumentsAsZip: void");
	}
	
	/**
	 * Generate a jar file from a repository folder path
	 */
	private void exportFolderAsJar(String fldPath, OutputStream os) throws PathNotFoundException, AccessDeniedException,
			RepositoryException, ArchiveException, ParseException, NoSuchGroupException, IOException, DatabaseException,
			MessagingException {
		log.debug("exportFolderAsJar({}, {})", fldPath, os);
		StringWriter out = new StringWriter();
		File tmp = null;
		
		try {
			tmp = FileUtils.createTempDir();
			
			// Export files
			RepositoryExporter.exportDocuments(null, fldPath, tmp, false, false, out, new TextInfoDecorator(fldPath));
			
			// Jar files
			ArchiveUtils.createJar(tmp, PathUtils.getName(fldPath), os);
		} catch (IOException e) {
			log.error("Error exporting jar", e);
			throw e;
		} finally {
			IOUtils.closeQuietly(out);
			
			if (tmp != null) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(tmp);
				} catch (IOException e) {
					log.error("Error deleting temporal directory", e);
					throw e;
				}
			}
		}
		
		log.debug("exportFolderAsJar: void");
	}
}
