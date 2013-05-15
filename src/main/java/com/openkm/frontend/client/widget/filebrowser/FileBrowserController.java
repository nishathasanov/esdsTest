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

package com.openkm.frontend.client.widget.filebrowser;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTProfileExplorer;
import com.openkm.frontend.client.util.OKMBundleResources;
import com.openkm.frontend.client.util.Util;

/**
 * FileBrowserController
 * 
 * @author jllort
 */
public class FileBrowserController extends Composite {
	
	private HorizontalPanel hPanel;
	private Image folder;
	private Image document;
	private Image mail;
	private Controller controller;
	
	/**
	 * FileBrowserController
	 */
	public FileBrowserController() {
		hPanel = new HorizontalPanel();
		
		// Folder
		folder = new Image(OKMBundleResources.INSTANCE.folder());
		folder.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (controller.isFolder()) {
					folder.setResource(OKMBundleResources.INSTANCE.folderDisabled());
					controller.setFolder(false);
				} else {
					folder.setResource(OKMBundleResources.INSTANCE.folder());
					controller.setFolder(true);
				}
				refreshFileBrowser();
			}
		});
		folder.setStyleName("okm-Hyperlink");
		// Document
		document = new Image(OKMBundleResources.INSTANCE.document());
		document.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (controller.isDocument()) {
					document.setResource(OKMBundleResources.INSTANCE.documentDisabled());
					controller.setDocument(false);
				} else {
					document.setResource(OKMBundleResources.INSTANCE.document());
					controller.setDocument(true);
				}
				refreshFileBrowser();
			}
		});
		document.setStyleName("okm-Hyperlink");
		// Mail
		mail = new Image(OKMBundleResources.INSTANCE.mail());
		mail.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (controller.isMail()) {
					mail.setResource(OKMBundleResources.INSTANCE.mailDisabled());
					controller.setMail(false);
				} else {
					mail.setResource(OKMBundleResources.INSTANCE.mail());
					controller.setMail(true);
				}
				refreshFileBrowser();
			}
		});
		mail.setStyleName("okm-Hyperlink");
		
		hPanel.setStyleName("gwt-controller");
		hPanel.setHeight("22");
		hPanel.setWidth("100%");
		
		initWidget(hPanel);
	}
	
	/**
	 * refreshFileBrowser
	 */
	private void refreshFileBrowser() {
		Main.get().mainPanel.desktop.browser.fileBrowser.refreshOnlyFileBrowser();
	}
	
	/**
	 * getController
	 * 
	 * @return
	 */
	public Controller getController() {
		return controller;
	}
	
	/**
	 * setController
	 * 
	 * @param controller
	 */
	public void setController(Controller controller) {
		this.controller = controller;
		if (controller.isFolder()) {
			folder.setResource(OKMBundleResources.INSTANCE.folder());
		} else {
			folder.setResource(OKMBundleResources.INSTANCE.folderDisabled());
		}
		if (controller.isDocument()) {
			document.setResource(OKMBundleResources.INSTANCE.document());
		} else {
			document.setResource(OKMBundleResources.INSTANCE.documentDisabled());
		}
		if (controller.isMail()) {
			mail.setResource(OKMBundleResources.INSTANCE.mail());
		} else {
			mail.setResource(OKMBundleResources.INSTANCE.mailDisabled());
		}
	}
	
	/**
	 * cleanAllByOpenFolderPath
	 */
	public void cleanAllByOpenFolderPath() {
		folder.setResource(OKMBundleResources.INSTANCE.folder());
		controller.setFolder(true);
		document.setResource(OKMBundleResources.INSTANCE.document());
		controller.setDocument(true);
		mail.setResource(OKMBundleResources.INSTANCE.mail());
		controller.setMail(true);
	}
	
	/**
	 * isFolder
	 * 
	 * @return
	 */
	public boolean isFolder() {
		return controller.isFolder();
	}
	
	/**
	 * isDocument
	 * 
	 * @return
	 */
	public boolean isDocument() {
		return controller.isDocument();
	}
	
	/**
	 * isMail
	 * 
	 * @return
	 */
	public boolean isMail() {
		return controller.isMail();
	}
	
	/**
	 * getSelectedRowId
	 * 
	 * @return
	 */
	public String getSelectedRowId() {
		return controller.getSelectedRowId();
	}
	
	/**
	 * setSelectedRowId
	 * 
	 * @param selectedRowId
	 */
	public void setSelectedRowId(String selectedRowId) {
		controller.setSelectedRowId(selectedRowId);
	}
	
	/**
	 * setProfileExplorer
	 * 
	 * @param profileExplorer
	 */
	public void setProfileExplorer(GWTProfileExplorer profileExplorer) {
		if (profileExplorer.isTypeFilterEnabled()) {
			hPanel.add(Util.hSpace("5"));
			hPanel.add(folder);
			hPanel.add(Util.hSpace("5"));
			hPanel.add(document);
			hPanel.add(Util.hSpace("5"));
			hPanel.add(mail);
			hPanel.add(Util.hSpace("5"));
			hPanel.setCellVerticalAlignment(folder, HasAlignment.ALIGN_MIDDLE);
			hPanel.setCellVerticalAlignment(document, HasAlignment.ALIGN_MIDDLE);
			hPanel.setCellVerticalAlignment(mail, HasAlignment.ALIGN_MIDDLE);
		}
		
		HTML space = new HTML("");
		hPanel.add(space);
		hPanel.setCellWidth(space, "100%");
	}
}