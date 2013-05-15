package com.openkm.extension.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.openkm.api.OKMDocument;
import com.openkm.api.OKMFolder;
import com.openkm.api.OKMRepository;
import com.openkm.automation.AutomationException;
import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.FileSizeExceededException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.MimeTypeConfig;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UnsupportedMimeTypeException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.VirusDetectedException;
import com.openkm.dao.extension.DropboxDAO;
import com.openkm.dropbox.Dropbox;
import com.openkm.extension.core.ExtensionException;
import com.openkm.extension.frontend.client.bean.GWTDropboxAccount;
import com.openkm.extension.frontend.client.bean.GWTDropboxEntry;
import com.openkm.extension.frontend.client.bean.GWTDropboxStatusListener;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.frontend.client.OKMException;
import com.openkm.frontend.client.constants.service.ErrorCode;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.servlet.frontend.OKMRemoteServiceServlet;
import com.openkm.util.FileUtils;
import com.openkm.util.GWTUtil;
import com.openkm.util.PathUtils;

/**
 * DropboxServlet
 * 
 * @author sochoa
 */
public class DropboxServlet extends OKMRemoteServiceServlet implements OKMDropboxService {
	private static Logger log = LoggerFactory.getLogger(DropboxServlet.class);
	private static final long serialVersionUID = 1L;
	private static final String CATEGORY_DOCUMENT = "document";
	private static final String CATEGORY_FOLDER = "folder";
	private Dropbox dropbox;
	private static final Map<String, List<GWTDropboxStatusListener>> statusMap = new HashMap<String, List<GWTDropboxStatusListener>>();
	
	@Override
	public String authorize() throws OKMException {
		try {
			dropbox = new Dropbox();
		} catch (DropboxUnlinkedException e) {
			return null; // Case not allows application
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		}
		
		return dropbox.getInfo().url;
	}
	
	@Override
	public GWTDropboxAccount access() throws OKMException {
		GWTDropboxAccount gwtDropboxAccount = new GWTDropboxAccount();
		
		try {
			String usrId = getThreadLocalRequest().getRemoteUser();
			
			if (dropbox == null) {
				if (DropboxDAO.getInstance().findByPk(usrId) != null) {
					dropbox = new Dropbox();
				} else {
					return null;
				}
			}
			
			Account account;
			account = dropbox.access(usrId);
			gwtDropboxAccount.setCountry(account.country);
			gwtDropboxAccount.setDisplayName(account.displayName);
			gwtDropboxAccount.setQuota(account.quota);
			gwtDropboxAccount.setQuotaNormal(account.quotaNormal);
			gwtDropboxAccount.setQuotaShared(account.quotaShared);
			gwtDropboxAccount.setReferralLink(account.referralLink);
			gwtDropboxAccount.setUid(account.uid);
		} catch (DropboxUnlinkedException e) {
			return null;
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Database), e.getMessage());
		}
		
		return gwtDropboxAccount;
	}
	
	@Override
	public void exportDocument(String path, String uuid) throws OKMException {
		InputStream is = null;
		
		try {
			String docPath = OKMRepository.getInstance().getNodePath(null, uuid);
			Document doc = OKMDocument.getInstance().getProperties(null, docPath);
			is = OKMDocument.getInstance().getContent(null, docPath, true);
			String dbPath = path + "/" + PathUtils.getName(docPath);
			dropbox.uploadFile(dbPath, is, doc.getActualVersion().getSize());
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PathNotFound), e.getMessage());
		} catch (AccessDeniedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_AccessDenied), e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Repository), e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Database), e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_IO), e.getMessage());
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	@Override
	public void exportFolder(String path, String uuid) throws OKMException {
		try {
			// Clean status list
			if (statusMap.containsKey(getThreadLocalRequest().getRemoteUser())) {
				statusMap.remove(getThreadLocalRequest().getRemoteUser());
			}
			
			exportFolderToDropbox(path, uuid);
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PathNotFound), e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Repository), e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Database), e.getMessage());
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PrincipalAdapter), e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_IO), e.getMessage());
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Parse), e.getMessage());
		} catch (NoSuchGroupException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_NoSuchGroup), e.getMessage());
		}
	}
	
	@Override
	public List<GWTDropboxEntry> search(String query, String category) throws OKMException {
		List<GWTDropboxEntry> list = new ArrayList<GWTDropboxEntry>();
		
		try {
			List<Entry> listEntry = dropbox.search(query);
			
			for (Entry entry : listEntry) {
				GWTDropboxEntry gwtDropboxEntry = copy(entry);
				
				if (category.equals(CATEGORY_FOLDER) && entry.isDir) {
					list.add(gwtDropboxEntry);
				} else if (category.equals(CATEGORY_DOCUMENT) && !entry.isDir) {
					// Change MIME Type to defined into OpenKM
					gwtDropboxEntry.setMimeType(MimeTypeConfig.mimeTypes.getContentType(entry.fileName()));
					list.add(gwtDropboxEntry);
				} else if (category.equals("")) {
					// Change MIME Type to defined into OpenKM
					gwtDropboxEntry.setMimeType(MimeTypeConfig.mimeTypes.getContentType(entry.fileName()));
					list.add(gwtDropboxEntry);
				}
			}
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		}
		
		return list;
	}
	
	@Override
	public void importDocument(GWTDropboxEntry gwtDropboxEntry, String path) throws OKMException {
		File tmp = null;
		FileOutputStream fos = null;
		InputStream is = null;
		
		try {
			tmp = FileUtils.createTempFile();
			fos = new FileOutputStream(tmp);
			dropbox.downloadFile(gwtDropboxEntry.getPath(), fos);
			fos.flush();
			is = new FileInputStream(tmp);
			Document doc = new Document();
			doc.setPath(path + "/" + gwtDropboxEntry.getFileName());
			doc = OKMDocument.getInstance().create(null, doc, is);
			addToStatus(doc); // status log
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_IO), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PathNotFound), e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Repository), e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Database), e.getMessage());
		} catch (FileSizeExceededException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_FileSizeExceeded), e.getMessage());
		} catch (UserQuotaExceededException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_UserQuoteExceed), e.getMessage());
		} catch (VirusDetectedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Virus), e.getMessage());
		} catch (AccessDeniedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_AccessDenied), e.getMessage());
		} catch (UnsupportedMimeTypeException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_UnsupportedMimeType), e.getMessage());
		} catch (ItemExistsException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_ItemExists), e.getMessage());
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Extension), e.getMessage());
		} catch (AutomationException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Automation), e.getMessage());
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PrincipalAdapter), e.getMessage());
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Parse), e.getMessage());
		} catch (NoSuchGroupException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_NoSuchGroup), e.getMessage());
		} finally {
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(is);
			FileUtils.deleteQuietly(tmp);
		}
	}
	
	@Override
	public void importFolder(GWTDropboxEntry gwtDropboxEntry, String path) throws OKMException {
		try {
			// Clean status list
			if (statusMap.containsKey(getThreadLocalRequest().getRemoteUser())) {
				statusMap.remove(getThreadLocalRequest().getRemoteUser());
			}
			
			importFolderToDropbox(gwtDropboxEntry, path);
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PathNotFound), e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Database), e.getMessage());
		} catch (AccessDeniedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_AccessDenied), e.getMessage());
		} catch (AutomationException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Automation), e.getMessage());
		} catch (ItemExistsException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_ItemExists), e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Repository), e.getMessage());
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Extension), e.getMessage());
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_PrincipalAdapter), e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_IO), e.getMessage());
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Parse), e.getMessage());
		} catch (NoSuchGroupException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_NoSuchGroup), e.getMessage());
		}
	}
	
	@Override
	public GWTDropboxEntry getRootDropbox() throws OKMException {
		GWTDropboxEntry gwtDropboxEntry = new GWTDropboxEntry();
		
		try {
			Entry entry = dropbox.getRoot();
			gwtDropboxEntry = copy(entry);
			gwtDropboxEntry.setChildren(true);
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		}
		
		return gwtDropboxEntry;
	}
	
	@Override
	public List<GWTDropboxEntry> getChildren(String parentPath) throws OKMException {
		List<GWTDropboxEntry> list = new ArrayList<GWTDropboxEntry>();
		
		try {
			List<Entry> listEntry = dropbox.getClildren(parentPath);
			for (Entry entry : listEntry) {
				if (entry.isDir) {
					GWTDropboxEntry gwtDropboxEntry = copy(entry);
					list.add(gwtDropboxEntry);
				}
			}
		} catch (DropboxUnlinkedException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxServerException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxIOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		} catch (DropboxException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMDropboxService, ErrorCode.CAUSE_Dropbox), e.getMessage());
		}
		
		return list;
	}
	
	/**
	 * copy from dropbox entry to GWTDropboxEntry
	 */
	private GWTDropboxEntry copy(Entry e) throws DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException {
		GWTDropboxEntry dropboxEntry = new GWTDropboxEntry();
		
		dropboxEntry.setBytes(e.bytes);
		dropboxEntry.setClientMTime(e.clientMtime);
		dropboxEntry.setHash(e.hash);
		dropboxEntry.setIcon(e.icon);
		dropboxEntry.setDeleted(e.isDeleted);
		dropboxEntry.setDir(e.isDir);
		dropboxEntry.setMimeType(e.mimeType);
		dropboxEntry.setModified(e.modified);
		dropboxEntry.setPath(e.path);
		dropboxEntry.setRev(e.rev);
		dropboxEntry.setRoot(e.root);
		dropboxEntry.setSize(e.size);
		dropboxEntry.setThumbExists(e.thumbExists);
		dropboxEntry.setFileName(e.fileName());
		dropboxEntry.setParentPath(e.parentPath());
		dropboxEntry.setChildren(dropbox.ischildren(e));
		
		return dropboxEntry;
	}
	
	@Override
	public List<GWTDropboxStatusListener> statusListener() throws OKMException {
		List<GWTDropboxStatusListener> doneList = new ArrayList<GWTDropboxStatusListener>(getStatusList());
		getStatusList().removeAll(doneList);
		return doneList;
	}
	
	/**
	 * exportFolderToDropbox
	 */
	private void exportFolderToDropbox(String path, String uuid) throws PathNotFoundException, RepositoryException,
			DatabaseException, DropboxUnlinkedException, DropboxServerException, DropboxIOException, DropboxException,
			PrincipalAdapterException, IOException, ParseException, NoSuchGroupException, OKMException {
		String fldPath = OKMRepository.getInstance().getNodePath(null, uuid);
		if (!path.equals("/")) {
			path += "/";
		}
		
		path += PathUtils.getName(fldPath);
		Entry entry = dropbox.createFolder(path);
		
		// export folders
		for (Folder folder : OKMFolder.getInstance().getChildren(null, fldPath)) {
			if (OKMFolder.getInstance().getChildren(null, folder.getPath()) != null) {
				exportFolderToDropbox(entry.path, folder.getUuid());
				addToStatus(folder); // status log
			}
		}
		
		// export documents
		for (Document document : OKMDocument.getInstance().getChildren(null, fldPath)) {
			exportDocument(entry.path, document.getUuid());
			addToStatus(document); // status log
		}
	}
	
	/**
	 * importFolderToDropbox
	 */
	private void importFolderToDropbox(GWTDropboxEntry gwtDropboxEntry, String path) throws PathNotFoundException,
			ItemExistsException, AccessDeniedException, RepositoryException, DatabaseException, ExtensionException,
			AutomationException, OKMException, DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException, PrincipalAdapterException, IOException, ParseException, NoSuchGroupException {
		Folder folder = new Folder();
		folder.setPath(path + "/" + gwtDropboxEntry.getFileName());
		folder = OKMFolder.getInstance().create(null, folder);
		addToStatus(folder); // status log
		
		for (Entry entry : dropbox.getClildren(gwtDropboxEntry.getPath())) {
			GWTDropboxEntry dropboxEntry = copy(entry);
			
			if (dropboxEntry.isDir()) {
				importFolderToDropbox(dropboxEntry, folder.getPath());
			} else {
				importDocument(dropboxEntry, folder.getPath());
			}
		}
		
	}
	
	/**
	 * addToStatus
	 */
	private void addToStatus(Folder folder) throws PrincipalAdapterException, IOException, ParseException,
			NoSuchGroupException, PathNotFoundException, RepositoryException, DatabaseException {
		GWTDropboxStatusListener dsl = new GWTDropboxStatusListener();
		dsl.setFolder(GWTUtil.copy(folder, null));
		getStatusList().add(dsl);
	}
	
	/**
	 * addToStatus
	 */
	private void addToStatus(Document doc) throws PrincipalAdapterException, IOException, ParseException, NoSuchGroupException,
			PathNotFoundException, RepositoryException, DatabaseException {
		GWTDropboxStatusListener dsl = new GWTDropboxStatusListener();
		dsl.setDocument(GWTUtil.copy(doc, null));
		getStatusList().add(dsl);
	}
	
	/**
	 * getStatusList
	 */
	private synchronized List<GWTDropboxStatusListener> getStatusList() {
		List<GWTDropboxStatusListener> status = null;
		String user = getThreadLocalRequest().getRemoteUser();
		
		if (statusMap.containsKey(user)) {
			status = statusMap.get(user);
		} else {
			status = new ArrayList<GWTDropboxStatusListener>();
			statusMap.put(user, status);
		}
		
		return status;
	}
}
