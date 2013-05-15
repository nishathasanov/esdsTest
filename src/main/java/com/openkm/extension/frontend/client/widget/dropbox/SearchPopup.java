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

package com.openkm.extension.frontend.client.widget.dropbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.extension.frontend.client.bean.GWTDropboxEntry;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.extension.frontend.client.service.OKMDropboxServiceAsync;
import com.openkm.extension.frontend.client.widget.base.ColoredFlexTable;
import com.openkm.frontend.client.bean.GWTFolder;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.NavigatorComunicator;
import com.openkm.frontend.client.extension.comunicator.UtilComunicator;

/**
 * SearchPopup
 * 
 * @author jllort
 */
public class SearchPopup extends DialogBox {
	private final OKMDropboxServiceAsync dropboxService = (OKMDropboxServiceAsync) GWT.create(OKMDropboxService.class);
	private static final String CATEGORY_DOCUMENT = "document";
	private static final String CATEGORY_FOLDER = "folder";
	
	private VerticalPanel vPanel;
	private HorizontalPanel hSearchPanel;
	private HorizontalPanel hButtonPanel;
	private ColoredFlexTable table;
	private ScrollPanel scrollPanel;
	private TextBox name;
	private ListBox typeList;
	private Button cancelButton;
	private Button importButton;
	private Map<String, GWTDropboxEntry> data;
	private int selectedRow = -1;
	
	/**
	 * SearchPopup
	 */
	public SearchPopup() {
		super(false, true);
		
		setText(GeneralComunicator.i18nExtension("dropbox.search"));
		vPanel = new VerticalPanel();
		vPanel.setWidth("100%");
		
		table = new ColoredFlexTable();
		table.setWidth("100%");
		table.setCellPadding(2);
		table.setCellSpacing(0);
		table.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (selectedRow >= 0) {
					table.setStyleRow(selectedRow, false);
				}
				selectedRow = table.getCellForEvent(event).getRowIndex();
				table.setStyleRow(selectedRow, true);
				importButton.setEnabled(true);
			}
		});
		table.addDoubleClickHandler(new DoubleClickHandler() {
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				if (selectedRow >= 0) {
					if (selectedRow >= 0) {
						table.setStyleRow(selectedRow, false);
					}
					executeImport();
					table.setStyleRow(selectedRow, true);
					hide();
				}
			}
		});
		
		scrollPanel = new ScrollPanel(table);
		scrollPanel.setPixelSize(690, 250);
		scrollPanel.setStyleName("okm-Popup-text");
		
		hSearchPanel = new HorizontalPanel();
		name = new TextBox();
		name.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				executeSearch();
			}
		});
		name.setWidth("540px");
		name.setStyleName("okm-Input");
		
		typeList = new ListBox();
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.all"), "");
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.document"), CATEGORY_DOCUMENT);
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.folder"), CATEGORY_FOLDER);
		typeList.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				executeSearch();
			}
		});
		typeList.setStyleName("okm-Input");
		
		hSearchPanel.add(UtilComunicator.hSpace("5"));
		hSearchPanel.add(name);
		hSearchPanel.add(UtilComunicator.hSpace("5"));
		hSearchPanel.add(typeList);
		
		// Buttons panel
		cancelButton = new Button(GeneralComunicator.i18n("button.cancel"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		cancelButton.setStyleName("okm-NoButton");
		importButton = new Button(GeneralComunicator.i18nExtension("button.import"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				executeImport();
				hide();
			}
		});
		importButton.setStyleName("okm-YesButton");
		
		hButtonPanel = new HorizontalPanel();
		hButtonPanel.add(cancelButton);
		hButtonPanel.add(new HTML("&nbsp;"));
		hButtonPanel.add(importButton);
		
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(hSearchPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(scrollPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(hButtonPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		
		vPanel.setCellHorizontalAlignment(hSearchPanel, HasAlignment.ALIGN_LEFT);
		vPanel.setCellHorizontalAlignment(scrollPanel, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(hButtonPanel, HasAlignment.ALIGN_CENTER);
		
		setWidget(vPanel);
	}
	
	/**
	 * executeSearch
	 */
	private void executeSearch() {
		if (name.getText().length() >= 3) {
			String category = "";
			
			if (typeList.getSelectedIndex() > 0) {
				category = typeList.getValue(typeList.getSelectedIndex());
			}
			
			String query = name.getText();
			Dropbox.get().status.setSearch();
			dropboxService.search(query, category, new AsyncCallback<List<GWTDropboxEntry>>() {
				@Override
				public void onSuccess(List<GWTDropboxEntry> result) {
					importButton.setEnabled(false);
					table.removeAllRows();
					data = new HashMap<String, GWTDropboxEntry>();
					
					for (GWTDropboxEntry gwtDropboxEntry : result) {
						int row = table.getRowCount();
						
						if (gwtDropboxEntry.isDir()) {
							if (gwtDropboxEntry.isChildren()) {
								table.setHTML(row, 0, UtilComunicator.imageItemHTML("img/menuitem_childs.gif"));
							} else {
								table.setHTML(row, 0, UtilComunicator.imageItemHTML("img/menuitem_empty.gif"));
							}
							
						} else {
							table.setHTML(row, 0, UtilComunicator.mimeImageHTML(gwtDropboxEntry.getMimeType()));
						}
						
						table.setHTML(row, 1, gwtDropboxEntry.getPath());
						table.setHTML(row, 2, gwtDropboxEntry.getRev());
						table.getCellFormatter().setWidth(row, 0, "20");
						table.getCellFormatter().setWidth(row, 1, "100%");
						table.getCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
						table.getCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_LEFT);
						table.getCellFormatter().setVisible(row, 2, false);
						data.put(gwtDropboxEntry.getRev(), gwtDropboxEntry);
					}
					
					Dropbox.get().status.unsetSearch();
				}
				
				@Override
				public void onFailure(Throwable caught) {
					GeneralComunicator.showError("search", caught);
					Dropbox.get().status.unsetSearch();
				}
			});
		} else {
			table.removeAllRows();
			importButton.setEnabled(false);
		}
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		setText(GeneralComunicator.i18nExtension("dropbox.search"));
		cancelButton.setHTML(GeneralComunicator.i18n("button.cancel"));
		importButton.setHTML(GeneralComunicator.i18nExtension("button.import"));
		typeList.clear();
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.all"), "");
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.document"), CATEGORY_DOCUMENT);
		typeList.addItem(GeneralComunicator.i18nExtension("dropbox.type.folder"), CATEGORY_FOLDER);
	}
	
	/**
	 * reset
	 */
	public void reset() {
		selectedRow = -1;
		table.removeAllRows();
		name.setText("");
		importButton.setEnabled(false);
		typeList.setSelectedIndex(0);
		name.setFocus(true);
	}
	
	/**
	 * executeImport
	 */
	private void executeImport() {
		if (selectedRow >= 0) {
			GWTDropboxEntry gwtDropboxEntry = data.get(table.getHTML(selectedRow, 2));
			
			// The actual folder selected in navigator view
			GWTFolder folder = NavigatorComunicator.getFolder();
			
			if (gwtDropboxEntry.isDir()) {
				Dropbox.get().status.setImporting();
				Dropbox.get().startStatusListener(StatusListenerPopup.ACTION_IMPORT);
				dropboxService.importFolder(gwtDropboxEntry, folder.getPath(), new AsyncCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						Dropbox.get().status.unsetImporting();
						GeneralComunicator.refreshUI();
						Dropbox.get().stopStatusListener();
					}
					
					@Override
					public void onFailure(Throwable caught) {
						GeneralComunicator.showError("importFolder", caught);
						Dropbox.get().status.unsetImporting();
						Dropbox.get().stopStatusListener();
					}
				});
			} else {
				Dropbox.get().status.setImporting();
				dropboxService.importDocument(gwtDropboxEntry, folder.getPath(), new AsyncCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						Dropbox.get().status.unsetImporting();
						GeneralComunicator.refreshUI();
					}
					
					@Override
					public void onFailure(Throwable caught) {
						GeneralComunicator.showError("importDocument", caught);
						Dropbox.get().status.unsetImporting();
					}
				});
			}
		}
	}
}