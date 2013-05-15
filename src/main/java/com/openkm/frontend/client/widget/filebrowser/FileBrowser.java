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

package com.openkm.frontend.client.widget.filebrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollTableImages;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.FileToUpload;
import com.openkm.frontend.client.bean.GWTDocument;
import com.openkm.frontend.client.bean.GWTFolder;
import com.openkm.frontend.client.bean.GWTMail;
import com.openkm.frontend.client.bean.GWTProfileExplorer;
import com.openkm.frontend.client.bean.GWTProfileFileBrowser;
import com.openkm.frontend.client.bean.ToolBarOption;
import com.openkm.frontend.client.constants.ui.UIDesktopConstants;
import com.openkm.frontend.client.constants.ui.UIFileUploadConstants;
import com.openkm.frontend.client.extension.event.HasDocumentEvent;
import com.openkm.frontend.client.extension.event.HasFolderEvent;
import com.openkm.frontend.client.extension.event.HasMailEvent;
import com.openkm.frontend.client.extension.event.handler.DocumentHandlerExtension;
import com.openkm.frontend.client.extension.event.handler.FolderHandlerExtension;
import com.openkm.frontend.client.extension.event.handler.MailHandlerExtension;
import com.openkm.frontend.client.extension.event.hashandler.HasDocumentHandlerExtension;
import com.openkm.frontend.client.extension.event.hashandler.HasFolderHandlerExtension;
import com.openkm.frontend.client.extension.event.hashandler.HasMailHandlerExtension;
import com.openkm.frontend.client.service.OKMDocumentService;
import com.openkm.frontend.client.service.OKMDocumentServiceAsync;
import com.openkm.frontend.client.service.OKMFolderService;
import com.openkm.frontend.client.service.OKMFolderServiceAsync;
import com.openkm.frontend.client.service.OKMMailService;
import com.openkm.frontend.client.service.OKMMailServiceAsync;
import com.openkm.frontend.client.service.OKMMassiveService;
import com.openkm.frontend.client.service.OKMMassiveServiceAsync;
import com.openkm.frontend.client.service.OKMNotifyService;
import com.openkm.frontend.client.service.OKMNotifyServiceAsync;
import com.openkm.frontend.client.util.OKMBundleResources;
import com.openkm.frontend.client.util.Util;
import com.openkm.frontend.client.widget.ConfirmPopup;
import com.openkm.frontend.client.widget.MenuPopup;
import com.openkm.frontend.client.widget.OriginPanel;
import com.openkm.frontend.client.widget.filebrowser.menu.CategoriesMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.MailMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.PersonalMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.TaxonomyMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.TemplatesMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.ThesaurusMenu;
import com.openkm.frontend.client.widget.filebrowser.menu.TrashMenu;
import com.openkm.frontend.client.widget.foldertree.FolderSelectPopup;

/**
 * File browser panel
 * 
 * @author jllort
 */
public class FileBrowser extends Composite implements OriginPanel, HasDocumentEvent, HasFolderEvent, HasMailEvent,
		HasDocumentHandlerExtension, HasFolderHandlerExtension, HasMailHandlerExtension {
	public static final int STATUS_SIZE = 26;
	
	// Refreshing next controler values
	private static final int GET_NONE = -1;
	private static final int GET_FOLDERS = 0;
	private static final int GET_DOCUMENTS = 1;
	private static final int GET_MAILS = 2;
	private static final int GET_ENDS = 3;
	
	// Definitions of fileBrowser actions
	public static final int ACTION_NONE = -1;
	public static final int ACTION_SECURITY_REFRESH_FOLDER = 0;
	public static final int ACTION_SECURITY_REFRESH_DOCUMENT = 1;
	public static final int ACTION_RENAME = 2;
	public static final int ACTION_SECURITY_REFRESH_MAIL = 3;
	public static final int ACTION_PROPERTY_GROUP_REFRESH_FOLDER = 4;
	public static final int ACTION_PROPERTY_GROUP_REFRESH_DOCUMENT = 5;
	public static final int ACTION_PROPERTY_GROUP_REFRESH_MAIL = 6;
	
	// Number of columns
	private int numberOfColumns = 0;
	
	private final OKMFolderServiceAsync folderService = (OKMFolderServiceAsync) GWT.create(OKMFolderService.class);
	private final OKMDocumentServiceAsync documentService = (OKMDocumentServiceAsync) GWT
			.create(OKMDocumentService.class);
	private final OKMNotifyServiceAsync notifyService = (OKMNotifyServiceAsync) GWT.create(OKMNotifyService.class);
	private final OKMMailServiceAsync mailService = (OKMMailServiceAsync) GWT.create(OKMMailService.class);
	private final OKMMassiveServiceAsync massiveService = (OKMMassiveServiceAsync) GWT.create(OKMMassiveService.class);
	
	private Image separator;
	public VerticalPanel panel;
	public ExtendedScrollTable table;
	private FixedWidthFlexTable headerTable;
	private FixedWidthGrid dataTable;
	private FilePath filePath;
	public MenuPopup taxonomyMenuPopup;
	public MenuPopup categoriesMenuPopup;
	public MenuPopup thesaurusMenuPopup;
	public MenuPopup trashMenuPopup;
	public MenuPopup templatesMenuPopup;
	public MenuPopup personalMenuPopup;
	public MenuPopup mailMenuPopup;
	public MenuPopup massiveOperationsMenuPopup;
	public Status status;
	private com.openkm.frontend.client.widget.popup.Status massiveStatus;
	private FileTextBox fileTextBox;
	private String fldId;
	private boolean panelSelected = false; // Indicates if panel is selected
	private String selectedRowId = ""; // Used to continue selecting the same
										// row before resfreshing the same
										// directory
	private String initialRowValueName = ""; // Used on rename to preserve
												// initial value name
	private GWTFolder tmpFolder;
	public int fileBrowserAction = ACTION_NONE; // To control rename and create
												// folder actions
	private int actualView = UIDesktopConstants.NAVIGATOR_TAXONOMY; // Used to
																	// indicate
																	// the
																	// actual
																	// view
	private HashMap<String, Controller> viewValues;
	private int numberOfFolders = 0;
	private int numberOfDocuments = 0;
	private int numberOfMails = 0;
	private List<DocumentHandlerExtension> docHandlerExtensionList;
	private List<FolderHandlerExtension> folderHandlerExtensionList;
	private List<MailHandlerExtension> mailHandlerExtensionList;
	
	// columns index
	private GWTProfileFileBrowser profileFileBrowser;
	private int colNameIndex = 0;
	
	// Controller
	private FileBrowserController fBController;
	private int nextRefresh = GET_NONE;
	
	/**
	 * FileBrowser
	 */
	public FileBrowser() {
		// Sets the actual view and view values hashMap object
		actualView = UIDesktopConstants.NAVIGATOR_TAXONOMY;
		viewValues = new HashMap<String, Controller>();
		docHandlerExtensionList = new ArrayList<DocumentHandlerExtension>();
		folderHandlerExtensionList = new ArrayList<FolderHandlerExtension>();
		mailHandlerExtensionList = new ArrayList<MailHandlerExtension>();
		
		panel = new VerticalPanel();
		filePath = new FilePath();
		
		ScrollTableImages scrollTableImages = new ScrollTableImages() {
			/*
			 * (non-Javadoc)
			 * @see com.google.gwt.gen2.table.client.AbstractScrollTable.
			 * ScrollTableImages#scrollTableAscending()
			 */
			public AbstractImagePrototype scrollTableAscending() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/sort_asc.gif");
					}
					
					public Image createImage() {
						return new Image("img/sort_asc.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/sort_asc.gif\"/>";
					}
				};
			}
			
			/*
			 * (non-Javadoc)
			 * @see com.google.gwt.gen2.table.client.AbstractScrollTable.
			 * ScrollTableImages#scrollTableDescending()
			 */
			public AbstractImagePrototype scrollTableDescending() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/sort_desc.gif");
					}
					
					public Image createImage() {
						return new Image("img/sort_desc.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/sort_desc.gif\"/>";
					}
				};
			}
			
			/*
			 * (non-Javadoc)
			 * @see com.google.gwt.gen2.table.client.AbstractScrollTable.
			 * ScrollTableImages#scrollTableFillWidth()
			 */
			public AbstractImagePrototype scrollTableFillWidth() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/fill_width.gif");
					}
					
					public Image createImage() {
						return new Image("img/fill_width.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/fill_width.gif\"/>";
					}
				};
			}
		};
		headerTable = new FixedWidthFlexTable();
		dataTable = new FixedWidthGrid();
		table = new ExtendedScrollTable(dataTable, headerTable, scrollTableImages);
		table.setSize("540", "140");
		table.setCellSpacing(0);
		table.setCellPadding(0);
		
		// eliminat aqui
		
		headerTable.addStyleName("okm-DisableSelect");
		table.addStyleName("okm-Input");
		
		taxonomyMenuPopup = new MenuPopup(new TaxonomyMenu());
		taxonomyMenuPopup.setStyleName("okm-FileBrowser-MenuPopup");
		categoriesMenuPopup = new MenuPopup(new CategoriesMenu());
		categoriesMenuPopup.setStyleName("okm-Tree-MenuPopup");
		thesaurusMenuPopup = new MenuPopup(new ThesaurusMenu());
		thesaurusMenuPopup.setStyleName("okm-Tree-MenuPopup");
		trashMenuPopup = new MenuPopup(new TrashMenu());
		trashMenuPopup.setStyleName("okm-Tree-MenuPopup");
		templatesMenuPopup = new MenuPopup(new TemplatesMenu());
		templatesMenuPopup.setStyleName("okm-Tree-MenuPopup");
		personalMenuPopup = new MenuPopup(new PersonalMenu());
		personalMenuPopup.setStyleName("okm-Tree-MenuPopup");
		mailMenuPopup = new MenuPopup(new MailMenu());
		mailMenuPopup.setStyleName("okm-Tree-MenuPopup");
		massiveOperationsMenuPopup = new MenuPopup(new MassiveOperationsMenu());
		massiveOperationsMenuPopup.setStyleName("okm-Tree-MenuPopup");
		status = new Status(this);
		status.setStyleName("okm-StatusPopup");
		massiveStatus = new com.openkm.frontend.client.widget.popup.Status(this);
		massiveStatus.setStyleName("okm-StatusPopup");
		fileTextBox = new FileTextBox();
		separator = new Image("img/transparent_pixel.gif");
		
		separator.setSize("100%", "4px");
		separator.setStyleName("okm-FileBrowser-Separator");
		
		panel.add(filePath);
		panel.add(separator);
		panel.add(table);
		panel.setSize("100%", "100%");
		panel.setCellHeight(filePath, "22");
		panel.setCellHeight(separator, "4");
		panel.setCellWidth(filePath, "100%");
		panel.setCellWidth(separator, "100%");
		panel.setCellVerticalAlignment(table, VerticalPanel.ALIGN_TOP);
		panel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
		initWidget(panel);
	}
	
	/**
	 * Resets the file browser values
	 */
	public void reset() {
		selectedRowId = "";
		table.reset();
	}
	
	/**
	 * Refresh languague values
	 */
	public void langRefresh() {
		filePath.langRefresh();
		taxonomyMenuPopup.langRefresh();
		thesaurusMenuPopup.langRefresh();
		trashMenuPopup.langRefresh();
		personalMenuPopup.langRefresh();
		templatesMenuPopup.langRefresh();
		mailMenuPopup.langRefresh();
	}
	
	/**
	 * Refresh the panel
	 * 
	 * @param fldId The path id
	 */
	public void refresh(String fldId) {
		// Try catch to prevent non controled error which stop filebrowser and not send finish signal to folder tree
		try {
			Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.resetNumericFolderValues();
			numberOfFolders = 0;
			numberOfDocuments = 0;
			numberOfMails = 0;
			// Because its asyncronous the getFolderChilds when finishes calls the
			// getDocumentChilds(flId)
			// to be sure refresh forlder before document files
			// and each time refresh file browser content needs to reset values
			table.reset();
			this.fldId = fldId;
			
			// Preparing for refreshing
			removeAllRows();
			enableDefaultTableSorter(true);
			nextRefresh = GET_FOLDERS;
			nextRefresh();
			
			// On initialization fldId==null
			if (fldId != null) {
				filePath.setPath(fldId);
			}
		} catch (Exception e) {
			Main.get().activeFolderTree.fileBrowserRefreshDone();
		}
	}
	
	/**
	 * nextRefresh
	 */
	private void nextRefresh() {
		switch (nextRefresh) {
			case GET_NONE:
				break;
			case GET_FOLDERS:
				if (fBController.getController().isFolder()) {
					getFolderChilds(fldId);
				} else {
					Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfFolders(0);
					nextRefresh = GET_DOCUMENTS;
					nextRefresh();
				}
				break;
			case GET_DOCUMENTS:
				if (fBController.getController().isDocument()) {
					getDocumentChilds(fldId);
				} else {
					Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfDocuments(0);
					nextRefresh = GET_MAILS;
					nextRefresh();
				}
				break;
			case GET_MAILS:
				if (fBController.getController().isMail()) {
					getMailChilds(fldId);
				} else {
					Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfMails(0);
					nextRefresh = GET_NONE;
				}
				break;
			
			case GET_ENDS:
				selectSelectedRowInTable();
				if (table.isSorted()) {
					table.refreshSort();
				}
				nextRefresh = GET_NONE;
				Main.get().activeFolderTree.fileBrowserRefreshDone();
				break;
		}
	}
	
	/**
	 * Removes all rows except the first
	 */
	private void removeAllRows() {
		// Purge all rows
		while (dataTable.getRowCount() > 0) {
			dataTable.removeRow(0);
		}
		
		table.getDataTable().resize(0, numberOfColumns);
	}
	
	/**
	 * Adds a folder to the panel
	 * 
	 * @param folder The folder to add
	 */
	private void addRow(GWTFolder folder) {
		table.addRow(folder);
	}
	
	/**
	 * Adds a new folder Normally executed from directory tree
	 * 
	 * @param folder The folder
	 */
	public void addFolder(GWTFolder folder) {
		if (fBController.isFolder()) {
			table.addRow(folder);
		}
		Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderChilds();
	}
	
	/**
	 * Adds a document to the panel
	 * 
	 * @param doc The doc to add
	 */
	private void addRow(GWTDocument doc) {
		table.addRow(doc);
	}
	
	/**
	 * Adds a mail to the panel
	 * 
	 * @param doc The doc to add
	 */
	private void addRow(GWTMail mail) {
		table.addRow(mail);
	}
	
	/**
	 * Refresh the folder childs and call after the documentChilds refresh
	 */
	final AsyncCallback<List<GWTFolder>> callbackGetFolderChilds = new AsyncCallback<List<GWTFolder>>() {
		public void onSuccess(List<GWTFolder> result) {
			// Try catch to prevent non controled error which stop filebrowser and not send finish signal to folder tree
			try {
				List<GWTFolder> folderList = result;
				numberOfFolders = folderList.size();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfFolders(numberOfFolders);
				
				for (GWTFolder folder : folderList) {
					addRow(folder);
				}
				
				Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderChilds();
				nextRefresh = GET_DOCUMENTS;
				nextRefresh();
			} catch (Exception e) {
				Main.get().activeFolderTree.fileBrowserRefreshDone();
			}
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderChilds();
			Main.get().showError("GetFolderChilds", caught);
			Main.get().activeFolderTree.fileBrowserRefreshDone();
		}
	};
	
	/**
	 * Refresh the document childs
	 */
	final AsyncCallback<List<GWTDocument>> callbackGetDocumentChilds = new AsyncCallback<List<GWTDocument>>() {
		public void onSuccess(List<GWTDocument> result) {
			// Try catch to prevent non controled error which stop filebrowser and not send finish signal to folder tree
			try {
				List<GWTDocument> documentList = result;
				numberOfDocuments = result.size();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfDocuments(numberOfDocuments);
				
				for (GWTDocument doc : documentList) {
					addRow(doc);
				}
				
				Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentChilds();
				nextRefresh = GET_MAILS;
				nextRefresh();
			} catch (Exception e) {
				Main.get().activeFolderTree.fileBrowserRefreshDone();
			}
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentChilds();
			Main.get().showError("GetDocumentChilds", caught);
			Main.get().activeFolderTree.fileBrowserRefreshDone();
		}
	};
	
	/**
	 * Refresh the document childs
	 */
	final AsyncCallback<List<GWTMail>> callbackGetMailChilds = new AsyncCallback<List<GWTMail>>() {
		public void onSuccess(List<GWTMail> result) {
			// Try catch to prevent non controled error which stop filebrowser and not send finish signal to folder tree
			try {
				List<GWTMail> mailList = result;
				numberOfMails = result.size();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setNumberOfMails(numberOfMails);
				
				for (GWTMail mail : mailList) {
					addRow(mail);
				}
				
				Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailChilds();
				nextRefresh = GET_ENDS;
				nextRefresh();
			} catch (Exception e) {
				Main.get().activeFolderTree.fileBrowserRefreshDone();
			}
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailChilds();
			Main.get().showError("GetMailChilds", caught);
			Main.get().activeFolderTree.fileBrowserRefreshDone();
		}
	};
	
	/**
	 * selectSelectedRowInTable
	 */
	private void selectSelectedRowInTable() {
		// If selectedRow > 0 must continue selecting the row ( after refreshing
		// )
		if (!selectedRowId.equals("")) {
			int selectedRow = table.findSelectedRowById(selectedRowId);
			
			if (selectedRow >= 0) {
				table.setSelectedRow(selectedRow);
				// Ensures selected row is visible before resfreshing
				// Must create a tmp widget to ensure row is visible and after
				// we restore values
				String tmpHTML = dataTable.getHTML(selectedRow, 0);
				HTML tmpWidget = new HTML("");
				dataTable.setWidget(selectedRow, 0, tmpWidget);
				// fileBrowserPanel.ensureVisible(tmpWidget); // TODO: El ensure
				// visible ha cambiado al ser un
				// ScrollTable !!
				dataTable.setHTML(selectedRow, 0, tmpHTML);
				
				setSelectedPanel(true);
				
				GWTDocument doc = table.getDocument();
				if (doc != null) {
					// Every time refreshing document properties can be changed
					// ( multi user activity for example )
					Main.get().mainPanel.desktop.browser.tabMultiple.enableTabDocument();
					Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.setProperties(doc);
					Main.get().mainPanel.topPanel.toolBar.checkToolButtonPermissions(doc,
							Main.get().activeFolderTree.getFolder());
				} else {
					GWTMail mail = table.getMail();
					if (mail != null) {
						// Every time refreshing document properties can be
						// changed ( multi user activity for example )
						Main.get().mainPanel.desktop.browser.tabMultiple.enableTabMail();
						Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.setProperties(mail);
						Main.get().mainPanel.topPanel.toolBar.checkToolButtonPermissions(mail,
								Main.get().activeFolderTree.getFolder());
					} else {
						GWTFolder folder = table.getFolder();
						if (folder != null) {
							// Every time refreshing folder properties can be
							// changed ( multi user activity for example )
							Main.get().mainPanel.desktop.browser.tabMultiple.enableTabFolder();
							Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setProperties(folder);
							Main.get().mainPanel.topPanel.toolBar.checkToolButtonPermissions(folder,
									Main.get().activeFolderTree.getFolder(), FILE_BROWSER);
						}
					}
				}
			}
		}
		
		selectedRowId = ""; // Always initializes value
	}
	
	/**
	 * Deletes a document
	 */
	final AsyncCallback<Object> callbackDeleteDocument = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			Log.debug("FileBroser callbackDeleteDocument:");
			fireEvent(HasDocumentEvent.DOCUMENT_DELETED);
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentDelete();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentDelete();
			Main.get().showError("DeleteDocument", caught);
		}
	};
	
	/**
	 * Deletes a mail
	 */
	final AsyncCallback<Object> callbackDeleteMail = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			Log.debug("FileBroser callbackDeleteMail:");
			fireEvent(HasMailEvent.MAIL_DELETED);
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailDelete();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailDelete();
			Main.get().showError("DeleteMail", caught);
		}
	};
	
	/**
	 * Purge a document
	 */
	final AsyncCallback<Object> callbackPurgeDocument = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().workspaceUserProperties.getUserDocumentsSize();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentPurge();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentPurge();
			Main.get().showError("PurgeDocument", caught);
		}
	};
	
	/**
	 * Purge a mail
	 */
	final AsyncCallback<Object> callbackPurgeMail = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().workspaceUserProperties.getUserDocumentsSize();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailPurge();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailPurge();
			Main.get().showError("PurgeMail", caught);
		}
	};
	
	/**
	 * Deletes a folder
	 */
	final AsyncCallback<Object> callbackDeleteFolder = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			fireEvent(HasFolderEvent.FOLDER_DELETED);
			// Deletes folder from tree for consistence view
			Main.get().activeFolderTree.removeDeleted(((GWTFolder) table.getFolder()).getPath());
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderDelete();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderDelete();
			Main.get().showError("DeleteFolder", caught);
		}
	};
	
	/**
	 * Purges a folder
	 */
	final AsyncCallback<Object> callbackPurgeFolder = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			// Deletes folder from tree for consistence view
			Main.get().activeFolderTree.removeDeleted(((GWTFolder) table.getFolder()).getPath());
			// int row = table.getSelectedRow();
			table.delete();
			// table.decrementHiddenIndexValues(row);
			mantainSelectedRow();
			Main.get().workspaceUserProperties.getUserDocumentsSize();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderPurge();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderPurge();
			Main.get().showError("PurgeFolder", caught);
		}
	};
	
	/**
	 * Document checkout
	 */
	final AsyncCallback<Object> callbackCheckOut = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			
			// Marks flag to ensure all RPC calls has finished before document
			// download
			Main.get().mainPanel.dashboard.userDashboard.setPendingCheckoutDocumentFlag();
			Main.get().mainPanel.dashboard.userDashboard.getUserCheckedOutDocuments();
			table.downloadDocument(true);
			
			// Document download is made after finishing refresh although
			// there's RPC call in
			// getUserCheckedOutDocuments we suppose refresh it'll be more
			// slower, and download
			// must be done after last RPC call is finished
			refresh(fldId);
			
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			Main.get().showError("CheckOut", caught);
		}
	};
	
	/**
	 * Document checkout
	 */
	final AsyncCallback<List<String>> callbackMassiveCheckOut = new AsyncCallback<List<String>>() {
		public void onSuccess(List<String> result) {
			mantainSelectedRow();
			
			// Marks flag to ensure all RPC calls has finished before document
			// download
			Main.get().mainPanel.dashboard.userDashboard.setPendingCheckoutDocumentFlag();
			Main.get().mainPanel.dashboard.userDashboard.getUserCheckedOutDocuments();
			table.downloadDocuments(true, result);
			
			// Document download is made after finishing refresh although
			// there's RPC call in
			// getUserCheckedOutDocuments we suppose refresh it'll be more
			// slower, and download
			// must be done after last RPC call is finished
			refresh(fldId);
			
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			Main.get().showError("CheckOut", caught);
		}
	};
	
	/**
	 * Document cancel checkout
	 */
	final AsyncCallback<Object> callbackCancelCheckOut = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			refresh(fldId);
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			Main.get().mainPanel.dashboard.userDashboard.getUserCheckedOutDocuments();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			Main.get().showError("CancelCheckOut", caught);
		}
	};

	/**
	 * Document force cancel checkout
	 */
	final AsyncCallback<Object> callbackForceCancelCheckOut = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			refresh(fldId);
			Main.get().mainPanel.dashboard.userDashboard.getUserCheckedOutDocuments();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCheckout();
			Main.get().showError("Force CancelCheckOut", caught);
		}
	};
	
	/**
	 * Document lock
	 */
	final AsyncCallback<Object> callbackLock = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagLock();
			refresh(fldId);
			Main.get().mainPanel.dashboard.userDashboard.getUserLockedDocuments();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagLock();
			Main.get().showError("Lock", caught);
		}
	};
	
	/**
	 * Document cancel lock
	 */
	final AsyncCallback<Object> callbackUnLock = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagUnLock();
			refresh(fldId);
			Main.get().mainPanel.dashboard.userDashboard.getUserLockedDocuments();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagUnLock();
			Main.get().showError("UnLock", caught);
		}
	};
	
	/**
	 * Document force cancel lock
	 */
	final AsyncCallback<Object> callbackForceUnLock = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			mantainSelectedRow();
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagUnLock();
			refresh(fldId);
			Main.get().mainPanel.dashboard.userDashboard.getUserLockedDocuments();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagUnLock();
			Main.get().showError("Force UnLock", caught);
		}
	};
	
	/**
	 * Document rename
	 */
	final AsyncCallback<GWTDocument> callbackDocumentRename = new AsyncCallback<GWTDocument>() {
		public void onSuccess(GWTDocument result) {
			GWTDocument doc = result;
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentRename();
			dataTable.setHTML(table.getSelectedRow(), colNameIndex, doc.getName());
			
			if (table.getDocument() != null) {
				table.setDocument(doc);
			}
			
			mantainSelectedRow();
			hideRename();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagDocumentRename();
			Main.get().showError("DocumentRename", caught);
		}
	};
	
	/**
	 * Folder rename
	 */
	final AsyncCallback<GWTFolder> callbackFolderRename = new AsyncCallback<GWTFolder>() {
		public void onSuccess(GWTFolder result) {
			GWTFolder folder = result;
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderRename();
			dataTable.setHTML(table.getSelectedRow(), colNameIndex, folder.getName());
			table.setFolder(folder);
			mantainSelectedRow();
			hideRename();
			Main.get().activeFolderTree.renameRenamed(tmpFolder.getPath(), folder);
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagFolderRename();
			Main.get().showError("FolderRename", caught);
		}
	};
	
	/**
	 * Document rename
	 */
	final AsyncCallback<GWTMail> callbackMailRename = new AsyncCallback<GWTMail>() {
		public void onSuccess(GWTMail result) {
			GWTMail mail = result;
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailRename();
			dataTable.setHTML(table.getSelectedRow(), colNameIndex, mail.getSubject());
			
			if (table.getMail() != null) {
				table.setMail(mail);
			}
			
			mantainSelectedRow();
			hideRename();
			refresh(fldId);
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailRename();
			Main.get().showError("MailRename", caught);
		}
	};
	
	/**
	 * Gets actual folder row selectd
	 */
	final AsyncCallback<GWTFolder> callbackGetFolder = new AsyncCallback<GWTFolder>() {
		public void onSuccess(GWTFolder result) {
			switch (fileBrowserAction) {
				case ACTION_SECURITY_REFRESH_FOLDER:
					GWTFolder gWTFolder = result;
					table.setFolder(gWTFolder);
					Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setProperties(gWTFolder);
					Main.get().activeFolderTree.refreshChildValues(gWTFolder);
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetFolder();
					fileBrowserAction = ACTION_NONE;
					break;
				case ACTION_PROPERTY_GROUP_REFRESH_FOLDER:
					table.setFolder(result);
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetFolder();
					fileBrowserAction = ACTION_NONE;
					break;
			}
		}
		
		public void onFailure(Throwable caught) {
			fileBrowserAction = ACTION_NONE; // Ensures on error folder action
												// be restores
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetFolder();
			Main.get().showError("GetFolder", caught);
		}
	};
	
	/**
	 * Gets actual document row selected
	 */
	final AsyncCallback<GWTDocument> callbackGetDocument = new AsyncCallback<GWTDocument>() {
		public void onSuccess(GWTDocument result) {
			switch (fileBrowserAction) {
				case ACTION_SECURITY_REFRESH_DOCUMENT:
					GWTDocument gWTDocument = result;
					table.setDocument(gWTDocument);
					Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.setProperties(gWTDocument);
					Main.get().mainPanel.topPanel.toolBar.checkToolButtonPermissions(gWTDocument,
							Main.get().activeFolderTree.getFolder());
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetDocument();
					fileBrowserAction = ACTION_NONE;
					break;
				case ACTION_PROPERTY_GROUP_REFRESH_DOCUMENT:
					table.setDocument(result);
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetDocument();
					fileBrowserAction = ACTION_NONE;
					break;
			}
		}
		
		public void onFailure(Throwable caught) {
			fileBrowserAction = ACTION_NONE; // Ensures on error folder action
												// be restores
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagGetDocument();
			Main.get().showError("GetDocument", caught);
		}
	};
	
	/**
	 * Gets actual mail row selected
	 */
	final AsyncCallback<GWTMail> callbackGetMailProperties = new AsyncCallback<GWTMail>() {
		public void onSuccess(GWTMail result) {
			switch (fileBrowserAction) {
				case ACTION_SECURITY_REFRESH_MAIL:
					GWTMail gWTMail = result;
					table.setMail(gWTMail);
					Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.setProperties(gWTMail);
					Main.get().mainPanel.topPanel.toolBar.checkToolButtonPermissions(gWTMail,
							Main.get().activeFolderTree.getFolder());
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailProperties();
					fileBrowserAction = ACTION_NONE;
					break;
				case ACTION_PROPERTY_GROUP_REFRESH_MAIL:
					table.setMail(result);
					Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailProperties();
					fileBrowserAction = ACTION_NONE;
					break;
			}
		}
		
		public void onFailure(Throwable caught) {
			fileBrowserAction = ACTION_NONE; // Ensures on error folder action
												// be restores
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagMailProperties();
			Main.get().showError("GetMail", caught);
		}
	};
	
	/**
	 * Adds a subscription
	 */
	final AsyncCallback<Object> callbackAddSubscription = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			if (table.isDocumentSelected() && table.getDocument() != null) {
				table.getDocument().setSubscribed(true);
				Main.get().mainPanel.dashboard.userDashboard.getUserSubscribedDocuments();
			} else if (table.isFolderSelected() && table.getFolder() != null) {
				table.getFolder().setSubscribed(true);
				Main.get().activeFolderTree.refreshChildValues((GWTFolder) table.getFolder());
				Main.get().mainPanel.dashboard.userDashboard.getUserSubscribedFolders();
			}
			mantainSelectedRow();
			refresh(fldId);
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagAddSubscription();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagAddSubscription();
			Main.get().showError("AddSubcription", caught);
		}
	};
	
	/**
	 * Removes a subscription
	 */
	final AsyncCallback<Object> callbackRemoveSubscription = new AsyncCallback<Object>() {
		public void onSuccess(Object result) {
			if (table.isDocumentSelected() && table.getDocument() != null) {
				table.getDocument().setSubscribed(false);
				Main.get().mainPanel.dashboard.userDashboard.getUserSubscribedDocuments();
			} else if (table.isFolderSelected() && table.getFolder() != null) {
				table.getFolder().setSubscribed(false);
				Main.get().activeFolderTree.refreshChildValues((GWTFolder) table.getFolder());
				Main.get().mainPanel.dashboard.userDashboard.getUserSubscribedFolders();
			}
			mantainSelectedRow();
			refresh(fldId);
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagRemoveSubscription();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagRemoveSubscription();
			Main.get().showError("RemoveSubcription", caught);
		}
	};
	
	/**
	 * Gets the folder childs list from the server
	 * 
	 * @param fldId The path id
	 */
	public void getFolderChilds(String fldId) {
		// In thesaurus and categories view must not be showed folders only
		// documents
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagFolderChilds();
		if (Main.get().mainPanel.desktop.navigator.getStackIndex() == UIDesktopConstants.NAVIGATOR_CATEGORIES) {
			folderService.getCategorizedChilds(fldId, callbackGetFolderChilds);
		} else if (Main.get().mainPanel.desktop.navigator.getStackIndex() == UIDesktopConstants.NAVIGATOR_THESAURUS) {
			folderService.getThesaurusChilds(fldId, callbackGetFolderChilds);
		} else {
			folderService.getChilds(fldId, true, callbackGetFolderChilds);
		}
	}
	
	/**
	 * Gets the document childs list from the server
	 * 
	 * @param fldId The path id
	 */
	public void getDocumentChilds(String fldId) {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagDocumentChilds();
		documentService.getChilds(fldId, callbackGetDocumentChilds);
	}
	
	/**
	 * Gets the mail childs list from the server
	 * 
	 * @param fldId The path id
	 */
	public void getMailChilds(String fldId) {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagMailChilds();
		mailService.getChilds(fldId, callbackGetMailChilds);
	}
	
	/**
	 * Gets the actual folder (actualItem) and refresh all information on it
	 */
	public void refreshFolderValues() {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagGetFolder();
		folderService.getProperties(((GWTFolder) table.getFolder()).getPath(), callbackGetFolder);
	}
	
	/**
	 * Gets the actual folder (actualItem) and refresh all information on it
	 */
	public void refreshDocumentValues() {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagGetDocument();
		documentService.get(((GWTDocument) table.getDocument()).getPath(), callbackGetDocument);
	}
	
	/**
	 * Gets the actual folder (actualItem) and refresh all information on it
	 */
	public void refreshMailValues() {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagMailProperties();
		mailService.getProperties(((GWTMail) table.getMail()).getPath(), callbackGetMailProperties);
	}
	
	/**
	 * Show the browser menu
	 */
	public void showMenu() {
		MenuPopup menuPopup = getActualMenuPopup();
		
		// For all menus except trash
		if (menuPopup != null) {
			menuPopup.setPopupPosition(table.getMouseX(), table.getMouseY());
			
			if (!table.isDocumentSelected() && !table.isFolderSelected() && !table.isMailSelected()) {
				menuPopup.disableAllOptions();
			}
			
			menuPopup.show();
		}
	}
	
	/**
	 * setOptions
	 * 
	 * @param toolBarOption
	 */
	public void setOptions(ToolBarOption toolBarOption) {
		MenuPopup menuPopup = getActualMenuPopup();
		
		if (menuPopup != null) {
			menuPopup.setOptions(toolBarOption);
		}
	}
	
	/**
	 * disableAllOptions
	 */
	public void disableAllOptions() {
		MenuPopup menuPopup = getActualMenuPopup();
		
		if (menuPopup != null) {
			menuPopup.disableAllOptions();
		}
	}
	
	/**
	 * enableAddPropertyGroup
	 */
	public void enableAddPropertyGroup() {
		MenuPopup menuPopup = getActualMenuPopup();
		
		if (menuPopup != null) {
			menuPopup.enableAddPropertyGroup();
		}
	}
	
	/**
	 * disableAddPropertyGroup
	 */
	public void disableAddPropertyGroup() {
		MenuPopup menuPopup = getActualMenuPopup();
		
		if (menuPopup != null) {
			menuPopup.disableAddPropertyGroup();
		}
	}
	
	/**
	 * getActualMenuPopup
	 */
	private MenuPopup getActualMenuPopup() {
		MenuPopup menuPopup = null;
		
		// The browser menu depends on actual view
		switch (actualView) {
			case UIDesktopConstants.NAVIGATOR_TAXONOMY:
				menuPopup = taxonomyMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_CATEGORIES:
				menuPopup = categoriesMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_THESAURUS:
				menuPopup = thesaurusMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_TRASH:
				menuPopup = trashMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_TEMPLATES:
				menuPopup = templatesMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_PERSONAL:
				menuPopup = personalMenuPopup;
				break;
			
			case UIDesktopConstants.NAVIGATOR_MAIL:
				menuPopup = mailMenuPopup;
				break;
		}
		
		return menuPopup;
	}
	
	/**
	 * Show a previos message to confirm delete
	 */
	public void confirmDelete() {
		if (Main.get().mainPanel.desktop.browser.fileBrowser.isMassive()) {
			Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_DELETE_MASSIVE);
			Main.get().confirmPopup.center();
		} else {
			if (table.isDocumentSelected() && table.getDocument() != null) {
				Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_DELETE_DOCUMENT);
				Main.get().confirmPopup.show();
			} else if (table.isFolderSelected() && table.getFolder() != null) {
				Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_DELETE_FOLDER);
				Main.get().confirmPopup.show();
			}
			if (table.isMailSelected() && table.getMail() != null) {
				Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_DELETE_MAIL);
				Main.get().confirmPopup.show();
			}
		}
	}
	
	/**
	 * Deletes file or document on file browser
	 */
	public void delete() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Log.debug("FileBroser delete:" + table.getDocument().getPath());
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagDocumentDelete();
			documentService.delete(table.getDocument().getPath(), callbackDeleteDocument);
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Log.debug("FileBroser delete:" + table.getFolder().getPath());
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagFolderDelete();
			folderService.delete(table.getFolder().getPath(), callbackDeleteFolder);
		}
		if (table.isMailSelected() && table.getMail() != null) {
			Log.debug("FileBroser delete:" + table.getMail().getPath());
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagMailDelete();
			mailService.delete(table.getMail().getPath(), callbackDeleteMail);
		}
	}
	
	/**
	 * Adds a subscription to document or folder
	 */
	public void addSubscription() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagAddSubscription();
			notifyService.subscribe(table.getDocument().getPath(), callbackAddSubscription);
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagAddSubscription();
			notifyService.subscribe(table.getFolder().getPath(), callbackAddSubscription);
		}
	}
	
	/**
	 * Adds a subscription to document or folder
	 */
	public void removeSubscription() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagRemoveSubscription();
			notifyService.unsubscribe(table.getDocument().getPath(), callbackRemoveSubscription);
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagRemoveSubscription();
			notifyService.unsubscribe(table.getFolder().getPath(), callbackRemoveSubscription);
		}
	}
	
	/**
	 * Deletes folder or document on file browser before is moved
	 */
	public void deleteMovedOrMoved() {
		if (table.isDocumentSelected()) {
			if (table.getDocument() != null) {
				table.delete();
				mantainSelectedRow();
				refresh(fldId);
			}
		} else if (table.isFolderSelected()) {
			Main.get().activeFolderTree.removeDeleted(((GWTFolder) table.getFolder()).getPath());
			table.delete();
			mantainSelectedRow();
			refresh(fldId);
			
		} else if (table.isMailSelected()) {
			table.delete();
			mantainSelectedRow();
			refresh(fldId);
		}
	}
	
	/**
	 * Move file or folder on file browser
	 */
	public void move() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToMove(table.getDocument());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToMove(table.getFolder());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToMove(table.getMail());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		}
	}
	
	/**
	 * Copy file or folder on file browser
	 */
	public void copy() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToCopy(table.getDocument());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToCopy(table.getFolder());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToCopy(table.getMail());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		}
	}
	
	/**
	 * Copy file or folder on file browser
	 */
	public void createFromTemplate() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToCreateFromTemplate(table.getDocument());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		}
	}
	
	/**
	 * Restore file or document on file browser ( only trash mode )
	 */
	public void restore() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToRestore(table.getDocument());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToRestore(table.getFolder());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().activeFolderTree.folderSelectPopup.setEntryPoint(FolderSelectPopup.ENTRYPOINT_BROWSER);
			Main.get().activeFolderTree.folderSelectPopup.setToRestore(table.getMail());
			Main.get().activeFolderTree.showDirectorySelectPopup();
		}
	}
	
	/**
	 * Confirm purge action
	 */
	public void confirmPurge() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_PURGE_DOCUMENT);
			Main.get().confirmPopup.show();
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_PURGE_FOLDER);
			Main.get().confirmPopup.show();
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_PURGE_DOCUMENT);
			Main.get().confirmPopup.show();
		}
	}
	
	/**
	 * Purge file or document on file browser ( only trash mode )
	 */
	public void purge() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagDocumentPurge();
			documentService.purge(table.getDocument().getPath(), callbackPurgeDocument);
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagFolderPurge();
			folderService.purge(table.getFolder().getPath(), callbackPurgeFolder);
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagMailPurge();
			mailService.purge(table.getMail().getPath(), callbackPurgeMail);
		}
	}
	
	/**
	 * Document checkout
	 */
	public void checkout() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCheckout();
			documentService.checkout(table.getDocument().getPath(), callbackCheckOut);
			
		}
	}

	/**
	 * Document massive checkout
	 * @author danilo
	 */
	public void massiveCheckout() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCheckout();
			massiveService.checkout(table.getAllSelectedPaths(), callbackMassiveCheckOut);
		}
	}
	
	/**
	 * Execute checkin
	 */
	public void checkin() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			FileToUpload fileToUpload = new FileToUpload();
			fileToUpload.setFileUpload(new FileUpload());
			fileToUpload.setPath(Main.get().mainPanel.desktop.browser.fileBrowser.getPath());
			fileToUpload.setAction(UIFileUploadConstants.ACTION_UPDATE);
			fileToUpload.setEnableAddButton(false);
			fileToUpload.setEnableImport(false);
			Main.get().fileUpload.addPendingFileToUpload(fileToUpload);
			
		}
	}
	
	/**
	 * Document cancel checkout
	 */
	public void cancelCheckout() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCheckout();
			documentService.cancelCheckout(table.getDocument().getPath(), callbackCancelCheckOut);
		}
	}
	
	/**
	 * Document massive cancel checkout
	 * @author danilo
	 */
	public void massiveCancelCheckout() {
		Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCheckout();
		massiveService.cancelCheckout(table.getAllSelectedPaths(), callbackCancelCheckOut);
	}
	
	/**
	 * Document cancel checkout
	 */
	public void forceCancelCheckout() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCheckout();
			documentService.forceCancelCheckout(table.getDocument().getPath(), callbackForceCancelCheckOut);
		}
	}
	
	/**
	 * Document lock
	 */
	public void lock() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagLock();
			documentService.lock(table.getDocument().getPath(), callbackLock);
		}
	}
	
	/**
	 * Document unlock
	 */
	public void unlock() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagUnLock();
			documentService.unlock(table.getDocument().getPath(), callbackUnLock);
		}
	}
	
	/**
	 * Document force unlock
	 */
	public void forceUnlock() {
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagUnLock();
			documentService.forceUnlock(table.getDocument().getPath(), callbackForceUnLock);
		}
	}
	
	/**
	 * Document and folder rename
	 */
	public void rename(String newName) {
		fileBrowserAction = ACTION_NONE;
		if (table.isDocumentSelected() && table.getDocument() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagDocumentRename();
			documentService.rename(table.getDocument().getPath(), newName, callbackDocumentRename);
		} else if (table.isFolderSelected() && table.getFolder() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagFolderRename();
			folderService.rename(table.getFolder().getPath(), newName, callbackFolderRename);
		} else if (table.isMailSelected() && table.getMail() != null) {
			Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagMailRename();
			mailService.rename(table.getMail().getPath(), newName, callbackMailRename);
		}
	}
	
	/**
	 * Gets the document path
	 */
	public String getPath() {
		if (table.getDocument() != null) {
			return table.getDocument().getPath();
		} else if (table.getFolder() != null) {
			return table.getFolder().getPath();
		} else if (table.getMail() != null) {
			return table.getMail().getPath();
		} else {
			return null;
		}
	}
	
	/**
	 * Maintain the selected row after refresh
	 */
	public void mantainSelectedRow() {
		selectedRowId = table.getSelectedId();
	}
	
	/**
	 * refreshOnlyFileBrowser
	 */
	public void refreshOnlyFileBrowser() {
		mantainSelectedRow();
		refresh(fldId);
	}
	
	/**
	 * Mantain the selected row by Path
	 * 
	 * @param path
	 */
	public void mantainSelectedRowByPath(String path) {
		selectedRowId = path;
	}
	
	/**
	 * cleanAllFilteringValues
	 */
	public void cleanAllFilteringValues() {
		fBController.cleanAllByOpenFolderPath();
	}
	
	/**
	 * Deselects the selected row
	 */
	public void deselecSelectedRow() {
		table.deselecSelectedRow();
		selectedRowId = "";
	}
	
	/**
	 * Return true or false if it's a selected row
	 * 
	 * @return True or false selected row
	 */
	public boolean isSelectedRow() {
		return table.isSelectedRow();
	}
	
	/**
	 * Sets the selected row Id value
	 * 
	 * @param selectedRowId The selected row Id value ( doc or folder ) path
	 */
	public void setSelectedRowId(String selectedRowId) {
		this.selectedRowId = selectedRowId;
	}
	
	/**
	 * Show the rename text Box
	 */
	public void rename() {
		if (table.isDocumentSelected() || table.isFolderSelected() || table.isMailSelected()) {
			Main.get().mainPanel.disableKeyShorcuts(); // Disables key shortcuts
														// while renaming
			fileBrowserAction = ACTION_RENAME;
			fileTextBox.reset();
			fileTextBox.setAction(FileTextBox.ACTION_RENAME);
			initialRowValueName = dataTable.getText(table.getSelectedRow(), colNameIndex);
			fileTextBox.setText(initialRowValueName);
			dataTable.setWidget(table.getSelectedRow(), colNameIndex, fileTextBox);
			dataTable.getCellFormatter().removeStyleName(table.getSelectedRow(), colNameIndex, "okm-DisableSelect");
			fileTextBox.setFocus();
			table.setAction(ExtendedScrollTable.ACTION_RENAMING);
			
			if (table.isFolderSelected() && table.getFolder() != null) {
				tmpFolder = table.getFolder();
			}
		}
	}
	
	/**
	 * Hide the rename text Box
	 */
	public void hideRename() {
		hideRename(table.getSelectedRow());
	}
	
	/**
	 * Hides the rename text box ( selected row )
	 * 
	 * @param selectedRow The selected row
	 */
	public void hideRename(int selectedRow) {
		fileBrowserAction = ACTION_NONE;
		dataTable.setHTML(selectedRow, colNameIndex, initialRowValueName);
		initialRowValueName = "";
		table.resetAction();
		Main.get().mainPanel.enableKeyShorcuts(); // Enables general keys
													// applications
	}
	
	/**
	 * Save changes to the actual view
	 */
	public void changeView(int view) {
		if (table.getSelectedRow() >= 0) {
			fBController.setSelectedRowId(table.getSelectedId());
		} else {
			fBController.setSelectedRowId(null);
		}
		// Saves actual view values on hashMap
		switch (actualView) {
			case UIDesktopConstants.NAVIGATOR_TAXONOMY:
				viewValues.put("view_root:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_TRASH:
				viewValues.put("view_trash:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_TEMPLATES:
				viewValues.put("view_templates:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_PERSONAL:
				viewValues.put("view_my_documents:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_MAIL:
				viewValues.put("view_mail:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_CATEGORIES:
				viewValues.put("view_categories:controller", fBController.getController());
				break;
			
			case UIDesktopConstants.NAVIGATOR_THESAURUS:
				viewValues.put("view_thesaurus:controller", fBController.getController());
				break;
		}
		
		if (table.getSelectedRow() > 0) {
			table.resetSelectedRows();
		}
		// Reset values
		reset();
		
		// Restores view values from hashMap
		switch (view) {
			case UIDesktopConstants.NAVIGATOR_TAXONOMY:
				if (viewValues.containsKey("view_root:controller")) {
					fBController.setController(viewValues.get("view_root:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_TRASH:
				if (viewValues.containsKey("view_trash:controller")) {
					fBController.setController(viewValues.get("view_trash:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_TEMPLATES:
				if (viewValues.containsKey("view_templates:controller")) {
					fBController.setController(viewValues.get("view_templates:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_PERSONAL:
				if (viewValues.containsKey("view_my_documents:controller")) {
					fBController.setController(viewValues.get("view_my_documents:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_MAIL:
				if (viewValues.containsKey("view_mail:controller")) {
					fBController.setController(viewValues.get("view_mail:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_CATEGORIES:
				if (viewValues.containsKey("view_categories:controller")) {
					fBController.setController(viewValues.get("view_categories:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
			
			case UIDesktopConstants.NAVIGATOR_THESAURUS:
				if (viewValues.containsKey("view_thesaurus:controller")) {
					fBController.setController(viewValues.get("view_thesaurus:controller"));
				} else {
					fBController.setController(createDefaultController());
				}
				break;
		}
		// Restores selectedRowId
		if (fBController.getSelectedRowId() != null && !fBController.getSelectedRowId().equals("")) {
			setSelectedRowId(fBController.getSelectedRowId());
		}
		actualView = view;
	}
	
	/**
	 * Indicates if panel is selected
	 * 
	 * @return The value of panel ( selected )
	 */
	public boolean isPanelSelected() {
		return panelSelected;
	}
	
	/**
	 * Sets the selected panel value
	 * 
	 * @param selected The selected panel value
	 */
	public void setSelectedPanel(boolean selected) {
		// Before other operations must change panel selected value
		panelSelected = selected;
		
		if (selected) {
			switch (actualView) {
				case UIDesktopConstants.NAVIGATOR_TAXONOMY:
				case UIDesktopConstants.NAVIGATOR_CATEGORIES:
				case UIDesktopConstants.NAVIGATOR_THESAURUS:
				case UIDesktopConstants.NAVIGATOR_TEMPLATES:
				case UIDesktopConstants.NAVIGATOR_PERSONAL:
				case UIDesktopConstants.NAVIGATOR_MAIL:
					Main.get().activeFolderTree.setSelectedPanel(false);
					break;
				
				case UIDesktopConstants.NAVIGATOR_TRASH:
					Main.get().activeFolderTree.setSelectedPanel(false);
					break;
			}
			
			panel.setStyleName("okm-PanelSelected");
		} else {
			panel.removeStyleName("okm-PanelSelected");
		}
		
	}
	
	/**
	 * Refresh for security changes on actual selected row icon color ) and
	 * folder / document properties, it only refresh the actual
	 */
	public void securityRefresh() {
		if (isFolderSelected()) {
			fileBrowserAction = ACTION_SECURITY_REFRESH_FOLDER;
			refreshFolderValues();
		} else if (isDocumentSelected()) {
			fileBrowserAction = ACTION_SECURITY_REFRESH_DOCUMENT;
			refreshDocumentValues();
		} else if (isMailSelected()) {
			fileBrowserAction = ACTION_SECURITY_REFRESH_MAIL;
			refreshMailValues();
		}
	}
	
	/**
	 * Sets the home
	 */
	public void setHome() {
		if (isDocumentSelected()) {
			Main.get().mainPanel.topPanel.mainMenu.bookmark.confirmSetHome(getDocument().getUuid(), getDocument()
					.getPath(), true);
		} else if (isFolderSelected()) {
			Main.get().mainPanel.topPanel.mainMenu.bookmark.confirmSetHome(getFolder().getUuid(),
					getFolder().getPath(), false);
		}
	}
	
	/**
	 * isFolderSelected
	 */
	public boolean isFolderSelected() {
		return table.isFolderSelected();
	}
	
	/**
	 * isDocumentSelected
	 */
	public boolean isDocumentSelected() {
		return table.isDocumentSelected();
	}
	
	/**
	 * isMailSelected
	 */
	public boolean isMailSelected() {
		return table.isMailSelected();
	}
	
	/**
	 * getFolder
	 */
	public GWTFolder getFolder() {
		return table.getFolder();
	}
	
	/**
	 * getDocument
	 * 
	 * @return
	 */
	public GWTDocument getDocument() {
		return table.getDocument();
	}
	
	/**
	 * getMail
	 * 
	 * @return
	 */
	public GWTMail getMail() {
		return table.getMail();
	}
	
	/**
	 * Export a folder
	 */
	public void exportFolderToFile() {
		if (table.isFolderSelected()) {
			Util.downloadFileByUUID(getFolder().getUuid(), "export");
		}
	}
	
	/**
	 * addNoteIconToSelectedRow
	 */
	public void addNoteIconToSelectedRow() {
		table.addNoteIconToSelectedRow();
	}
	
	/**
	 * deleteNoteIconToSelectedRow
	 */
	public void deleteNoteIconToSelectedRow() {
		table.deleteNoteIconToSelectedRow();
	}
	
	/**
	 * hasRows
	 * 
	 * @return has rows
	 */
	public boolean hasRows() {
		return table.hasRows();
	}
	
	/**
	 * selectAllMassive
	 */
	public void selectAllMassive() {
		table.selectAllMassive();
	}
	
	/**
	 * selectAllFoldersMassive
	 */
	public void selectAllFoldersMassive() {
		table.selectAllFoldersMassive();
	}
	
	/**
	 * selectAllDocumentsMassive
	 */
	public void selectAllDocumentsMassive() {
		table.selectAllDocumentsMassive();
	}
	
	/**
	 * selectAllMailsMassive
	 */
	public void selectAllMailsMassive() {
		table.selectAllMailsMassive();
	}
	
	/**
	 * removeAllMassive
	 */
	public void removeAllMassive() {
		table.removeAllMassive();
	}
	
	/**
	 * isMassive
	 * 
	 * @return
	 */
	public boolean isMassive() {
		return table.isMassive();
	}
	
	/**
	 * getAllSelectedPaths
	 * 
	 * @return
	 */
	public List<String> getAllSelectedPaths() {
		return table.getAllSelectedPaths();
	}
	
	/**
	 * deleteMasive
	 */
	public void deleteMasive() {
		massiveStatus.setFlagDelete();
		massiveService.delete(Main.get().mainPanel.desktop.browser.fileBrowser.getAllSelectedPaths(),
				new AsyncCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						massiveStatus.unsetFlagDelete();
						Main.get().mainPanel.topPanel.toolBar.executeRefresh();
					}
					
					@Override
					public void onFailure(Throwable caught) {
						massiveStatus.unsetFlagDelete();
						Main.get().showError("delete", caught);
					}
				});
	}
	
	/**
	 * setProfileFileBrowser
	 * 
	 * @param profileFileBrowser
	 */
	public void setProfileFileBrowser(GWTProfileFileBrowser profileFileBrowser, GWTProfileExplorer profileExplorer) {
		this.profileFileBrowser = profileFileBrowser;
		
		fBController = new FileBrowserController();
		fBController.setController(createDefaultController());
		
		int row = 0;
		if (profileExplorer.isTypeFilterEnabled()) {
			fBController.setProfileExplorer(profileExplorer);
			headerTable.setWidget(row, 0, fBController);
			headerTable.getFlexCellFormatter().setVerticalAlignment(row++, 0, HasAlignment.ALIGN_TOP);
		}
		
		int col = 0;
		if (profileFileBrowser.isStatusVisible()) {
			headerTable.setHTML(row, col, "&nbsp;");
			table.setColumnWidth(col, 60);
			table.setPreferredColumnWidth(col, 60);
			table.setColumnSortable(col++, false);
		}
		if (profileFileBrowser.isMassiveVisible()) {
			headerTable.setHTML(row, col, "&nbsp;");
			
			final Image massive = new Image(OKMBundleResources.INSTANCE.massive());
			massive.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					massiveOperationsMenuPopup.setPopupPosition(massive.getAbsoluteLeft(),
							massive.getAbsoluteTop() + 15);
					massiveOperationsMenuPopup.menu.evaluateMenuOptions();
					massiveOperationsMenuPopup.show();
				}
			});
			massive.setStyleName("okm-Hyperlink");
			
			headerTable.setWidget(row, col, massive);
			table.setColumnWidth(col, 30);
			table.setPreferredColumnWidth(col, 30);
			table.setColumnSortable(col, false);
			table.setColMassiveIndex(col); // Setting real massive column index
			headerTable.getCellFormatter().setHorizontalAlignment(row, col++, HasAlignment.ALIGN_CENTER);
		}
		if (profileFileBrowser.isIconVisible()) {
			headerTable.setHTML(row, col, "&nbsp;");
			table.setColumnWidth(col, 25);
			table.setPreferredColumnWidth(col++, 25);
		}
		if (profileFileBrowser.isNameVisible()) {
			headerTable.setHTML(row, col, Main.i18n("filebrowser.name"));
			colNameIndex = col;
			table.setColumnWidth(col++, 150);
		}
		if (profileFileBrowser.isSizeVisible()) {
			headerTable.setHTML(row, col, Main.i18n("filebrowser.size"));
			table.setColumnWidth(col++, 100);
		}
		if (profileFileBrowser.isLastModifiedVisible()) {
			headerTable.setHTML(row, col, Main.i18n("filebrowser.date.update"));
			table.setColumnWidth(col, 150);
			table.setPreferredColumnWidth(col++, 150);
		}
		if (profileFileBrowser.isAuthorVisible()) {
			headerTable.setHTML(row, col, Main.i18n("filebrowser.author"));
			table.setColumnWidth(col++, 110);
		}
		if (profileFileBrowser.isVersionVisible()) {
			headerTable.setHTML(row, col, Main.i18n("filebrowser.version"));
			table.setColumnWidth(col++, 90);
		}
		headerTable.setHTML(row, col++, ""); // used to store data
		numberOfColumns = col;
		table.setColDataIndex(numberOfColumns - 1); // Columns starts with value
													// 0
		table.setProfileFileBrowser(profileFileBrowser);
		if (profileExplorer.isTypeFilterEnabled()) {
			headerTable.getFlexCellFormatter().setColSpan((row - 1), 0, numberOfColumns);
		} else {
			enableDefaultTableSorter(true); // If pagination is not visible
											// default sorter shold be enabled
		}
	}
	
	/**
	 * enableDefaultTableSorter
	 * 
	 * @param sortable
	 */
	private void enableDefaultTableSorter(boolean sortable) {
		int col = 0;
		if (profileFileBrowser.isStatusVisible()) {
			col++;
		}
		if (profileFileBrowser.isMassiveVisible()) {
			col++;
		}
		if (profileFileBrowser.isIconVisible()) {
			table.setColumnSortable(col++, sortable);
		}
		if (profileFileBrowser.isNameVisible()) {
			table.setColumnSortable(col++, sortable);
		}
		if (profileFileBrowser.isSizeVisible()) {
			table.setColumnSortable(col++, sortable);
		}
		if (profileFileBrowser.isLastModifiedVisible()) {
			table.setColumnSortable(col++, sortable);
		}
		if (profileFileBrowser.isAuthorVisible()) {
			table.setColumnSortable(col++, sortable);
		}
		if (profileFileBrowser.isVersionVisible()) {
			table.setColumnSortable(col++, sortable);
		}
	}
	
	/**
	 * createDefaultController
	 * 
	 * @return
	 */
	private Controller createDefaultController() {
		Controller controller = new Controller();
		return controller;
	}
	
	/**
	 * setFileBrowserAction
	 * 
	 * @param fileBrowserAction
	 */
	public void setFileBrowserAction(int fileBrowserAction) {
		this.fileBrowserAction = fileBrowserAction;
	}
	
	/**
	 * addDocumentHandlerExtension
	 * 
	 * @param handlerExtension
	 */
	public void addDocumentHandlerExtension(DocumentHandlerExtension handlerExtension) {
		docHandlerExtensionList.add(handlerExtension);
	}
	
	@Override
	public void addFolderHandlerExtension(FolderHandlerExtension handlerExtension) {
		folderHandlerExtensionList.add(handlerExtension);
	}
	
	@Override
	public void addMailHandlerExtension(MailHandlerExtension handlerExtension) {
		mailHandlerExtensionList.add(handlerExtension);
	}
	
	@Override
	public void fireEvent(DocumentEventConstant event) {
		for (DocumentHandlerExtension handlerExtension : docHandlerExtensionList) {
			handlerExtension.onChange(event);
		}
	}
	
	@Override
	public void fireEvent(FolderEventConstant event) {
		for (FolderHandlerExtension handlerExtension : folderHandlerExtensionList) {
			handlerExtension.onChange(event);
		}
	}
	
	@Override
	public void fireEvent(MailEventConstant event) {
		for (MailHandlerExtension handlerExtension : mailHandlerExtensionList) {
			handlerExtension.onChange(event);
		}
	}
}