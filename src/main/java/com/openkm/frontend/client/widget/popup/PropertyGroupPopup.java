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

package com.openkm.frontend.client.widget.popup;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTDocument;
import com.openkm.frontend.client.bean.GWTFolder;
import com.openkm.frontend.client.bean.GWTMail;
import com.openkm.frontend.client.bean.GWTPropertyGroup;
import com.openkm.frontend.client.bean.form.GWTFormElement;
import com.openkm.frontend.client.service.OKMPropertyGroupService;
import com.openkm.frontend.client.service.OKMPropertyGroupServiceAsync;
import com.openkm.frontend.client.widget.filebrowser.FileBrowser;
import com.openkm.frontend.client.widget.form.FormManager;

/**
 * PropertyGroupPopup popup
 * 
 * @author jllort
 */
public class PropertyGroupPopup extends DialogBox {
	private final OKMPropertyGroupServiceAsync propertyGroupService = (OKMPropertyGroupServiceAsync) GWT
			.create(OKMPropertyGroupService.class);
	
	public static final int PHASE_NONE = 0;
	public static final int PHASE_SELECT = 1;
	public static final int PHASE_SHOW_PROPERTIES = 2;
	public static final int PHASE_PROPERTIES_ADDED = 3;
	
	private FlexTable table;
	private HorizontalPanel hPanel;
	private Button cancel;
	private Button add;
	private ListBox listBox;
	private String path;
	private FormManager manager;
	private HTML propertyGroupName;
	private FlexTable propertyGroupTable;
	private boolean groupsLoaded = false;
	private int phase = PHASE_NONE;
	private Status status;
	
	/**
	 * PropertyGroupPopup popup
	 */
	public PropertyGroupPopup() {
		// Establishes auto-close when click outside
		super(false, true);
		setText(Main.i18n("group.label"));
		
		// Status
		status = new Status(this);
		status.setStyleName("okm-StatusPopup");
		
		table = new FlexTable();
		table.setCellPadding(4);
		table.setCellSpacing(0);
		table.setWidth("100%");
		hPanel = new HorizontalPanel();
		manager = new FormManager();
		
		propertyGroupTable = manager.getTable();
		propertyGroupTable.setWidth("100%");
		
		cancel = new Button(Main.i18n("button.cancel"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (Main.get().mainPanel.desktop.browser.fileBrowser.isMassive()) {
					Main.get().mainPanel.topPanel.toolBar.executeRefresh();
				}
				groupsLoaded = false;
				hide();
			}
		});
		
		add = new Button(Main.i18n("button.add"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addGroup();
			}
		});
		
		listBox = new ListBox();
		listBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent arg0) {
				if (listBox.getSelectedIndex() > 0) {
					add.setEnabled(true);
				} else {
					add.setEnabled(false);
				}
			}
		});
		
		listBox.setStyleName("okm-Select");
		
		HorizontalPanel grpNamePanel = new HorizontalPanel();
		propertyGroupName = new HTML("");
		grpNamePanel.add(propertyGroupName);
		grpNamePanel.setWidth("100%");
		grpNamePanel.setCellHorizontalAlignment(propertyGroupName, HasAlignment.ALIGN_CENTER);
		
		cancel.setStyleName("okm-NoButton");
		add.setStyleName("okm-AddButton");
		add.setEnabled(false);
		
		hPanel.add(cancel);
		hPanel.add(new HTML("&nbsp;&nbsp;"));
		hPanel.add(add);
		
		hPanel.setCellHorizontalAlignment(cancel, VerticalPanel.ALIGN_CENTER);
		hPanel.setCellHorizontalAlignment(add, VerticalPanel.ALIGN_CENTER);
		
		table.setWidget(0, 0, listBox);
		table.setWidget(1, 0, grpNamePanel);
		table.setWidget(2, 0, propertyGroupTable);
		table.setWidget(3, 0, hPanel);
		
		table.getCellFormatter().setStyleName(1, 0, "okm-Security-Title");
		table.getCellFormatter().addStyleName(1, 0, "okm-Security-Title-RightBorder");
		
		table.getCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
		table.getCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
		table.getCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_CENTER);
		table.getCellFormatter().setHorizontalAlignment(3, 0, HasAlignment.ALIGN_CENTER);
		
		super.hide();
		setWidget(table);
	}
	
	/**
	 * Enables close button
	 */
	public void enableClose() {
		cancel.setEnabled(true);
		Main.get().mainPanel.setVisible(true); // Shows main panel when all
												// widgets are loaded
	}
	
	/**
	 * Language refresh
	 */
	public void langRefresh() {
		setText(Main.i18n("group.label"));
		cancel.setText(Main.i18n("button.cancel"));
		add.setText(Main.i18n("button.add"));
	}
	
	/**
	 * reset
	 */
	public void reset() {
		switchPhase(PHASE_SELECT);
		if (!groupsLoaded) {
			groupsLoaded = true;
			getAllGroups(); // Gets all groups
		}
	}
	
	/**
	 * drawPhase
	 */
	private void switchPhase(int phase) {
		this.phase = phase;
		switch (phase) {
			case PHASE_PROPERTIES_ADDED:
				listBox.removeItem(listBox.getSelectedIndex());
				listBox.setSelectedIndex(0);
				// not break because continues with phase select
			case PHASE_SELECT:
				table.getCellFormatter().setVisible(0, 0, true);
				table.getCellFormatter().setVisible(1, 0, false);
				table.getCellFormatter().setVisible(2, 0, false);
				add.setEnabled(false);
				break;
			
			case PHASE_SHOW_PROPERTIES:
				table.getCellFormatter().setVisible(0, 0, false);
				table.getCellFormatter().setVisible(1, 0, true);
				table.getCellFormatter().setVisible(2, 0, true);
				add.setEnabled(true);
				break;
		}
		center();
	}
	
	/**
	 * Gets all property groups
	 */
	private void getAllGroups() {
		if (!Main.get().mainPanel.desktop.browser.fileBrowser.isMassive()) {
			path = Main.get().mainPanel.topPanel.toolBar.getActualNodePath();
			if (!path.equals("")) {
				propertyGroupService.getAllGroups(path, new AsyncCallback<List<GWTPropertyGroup>>() {
					@Override
					public void onSuccess(List<GWTPropertyGroup> result) {
						listBox.clear();
						listBox.addItem("", ""); // Adds empty value
						
						for (Iterator<GWTPropertyGroup> it = result.iterator(); it.hasNext();) {
							GWTPropertyGroup group = it.next();
							listBox.addItem(group.getLabel(), group.getName());
						}
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Main.get().showError("GetAllGroups", caught);
					}
				});
			}
		} else {
			propertyGroupService.getAllGroups(new AsyncCallback<List<GWTPropertyGroup>>() {
				@Override
				public void onSuccess(List<GWTPropertyGroup> result) {
					listBox.clear();
					listBox.addItem("", ""); // Adds empty value
					
					for (Iterator<GWTPropertyGroup> it = result.iterator(); it.hasNext();) {
						GWTPropertyGroup group = it.next();
						listBox.addItem(group.getLabel(), group.getName());
					}
				}
				
				@Override
				public void onFailure(Throwable caught) {
					Main.get().showError("GetAllGroups", caught);
				}
			});
		}
	}
	
	/**
	 * Add a group to a document
	 */
	private void addGroup() {
		if (listBox.getSelectedIndex() > 0) {
			final String grpName = listBox.getValue(listBox.getSelectedIndex());
			if (phase == PHASE_SHOW_PROPERTIES) {
				if (manager.getValidationProcessor().validate()) {
					status.setFlagAddPropertyGroup();
					propertyGroupService.addGroup(path, grpName, new AsyncCallback<Object>() {
						@Override
						public void onSuccess(Object result) {
							// Adding properties
							propertyGroupService.setProperties(path, grpName,
									manager.updateFormElementsValuesWithNewer(), new AsyncCallback<Object>() {
										@Override
										public void onSuccess(Object result) {
											refreshingActualNode(manager.updateFormElementsValuesWithNewer());
											switchPhase(PHASE_PROPERTIES_ADDED);
											status.unsetFlagAddPropertyGroup();
										}
										
										@Override
										public void onFailure(Throwable caught) {
											status.unsetFlagAddPropertyGroup();
											Main.get().showError("setProperties", caught);
										}
									});
						}
						
						@Override
						public void onFailure(Throwable caught) {
							status.unsetFlagAddPropertyGroup();
							Main.get().showError("AddGroup", caught);
						}
					});
				}
			} else {
				propertyGroupName.setHTML(listBox.getItemText(listBox.getSelectedIndex()));
				propertyGroupService.getPropertyGroupForm(grpName, new AsyncCallback<List<GWTFormElement>>() {
					@Override
					public void onSuccess(List<GWTFormElement> result) {
						manager.setFormElements(result);
						manager.edit();
						switchPhase(PHASE_SHOW_PROPERTIES);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Main.get().showError("getPropertyGroupForm", caught);
					}
				});
				
			}
		}
	}
	
	/**
	 * refreshingActualNode
	 */
	private void refreshingActualNode(List<GWTFormElement> formElements) {
		if (Main.get().mainPanel.desktop.browser.fileBrowser.isMassive()) {
			Main.get().mainPanel.topPanel.toolBar.executeRefresh(); // Case
																	// massive
																	// and could
																	// be
																	// affected
																	// several
																	// rows
		} else {
			Object node = Main.get().mainPanel.topPanel.toolBar.getActualNode();
			if (node != null) {
				if (Main.get().mainPanel.topPanel.toolBar.isNodeDocument()) {
					GWTDocument doc = (GWTDocument) Main.get().mainPanel.topPanel.toolBar.getActualNode();
					if (Main.get().mainPanel.desktop.browser.fileBrowser.isPanelSelected()
							&& Main.get().mainPanel.desktop.browser.fileBrowser.isDocumentSelected()) {
						Main.get().mainPanel.desktop.browser.fileBrowser
								.setFileBrowserAction(FileBrowser.ACTION_PROPERTY_GROUP_REFRESH_DOCUMENT);
						Main.get().mainPanel.desktop.browser.fileBrowser.refreshDocumentValues();
					}
					Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.setProperties(doc);
				} else if (Main.get().mainPanel.topPanel.toolBar.isNodeFolder()) {
					GWTFolder folder = (GWTFolder) Main.get().mainPanel.topPanel.toolBar.getActualNode();
					if (Main.get().mainPanel.desktop.browser.fileBrowser.isPanelSelected()
							&& Main.get().mainPanel.desktop.browser.fileBrowser.isFolderSelected()) {
						Main.get().mainPanel.desktop.browser.fileBrowser
								.setFileBrowserAction(FileBrowser.ACTION_PROPERTY_GROUP_REFRESH_FOLDER);
						Main.get().mainPanel.desktop.browser.fileBrowser.refreshFolderValues();
					}
					Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setProperties(folder);
				} else if (Main.get().mainPanel.topPanel.toolBar.isNodeMail()) {
					GWTMail mail = (GWTMail) Main.get().mainPanel.topPanel.toolBar.getActualNode();
					if (Main.get().mainPanel.desktop.browser.fileBrowser.isPanelSelected()
							&& Main.get().mainPanel.desktop.browser.fileBrowser.isMailSelected()) {
						Main.get().mainPanel.desktop.browser.fileBrowser
								.setFileBrowserAction(FileBrowser.ACTION_PROPERTY_GROUP_REFRESH_MAIL);
						Main.get().mainPanel.desktop.browser.fileBrowser.refreshMailValues();
					}
					Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.setProperties(mail);
				}
				// Case there's only one items (white) then
				// there's no item to be added and must disable addPropertyGroup
				if (listBox.getItemCount() == 1) {
					Main.get().mainPanel.topPanel.toolBar.disableAddPropertyGroup();
				}
			}
		}
	}
}
