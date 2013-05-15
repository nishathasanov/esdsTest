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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.widget.searchin.HasPropertyHandler;

/**
 * FolderSelectPopup
 * 
 * @author sochoa
 * 
 */
public class FolderSelectPopup extends DialogBox {
	private VerticalPanel vPanel;
	private HorizontalPanel hPanel;
	private ScrollPanel scrollDirectoryPanel;
	private VerticalPanel verticalDirectoryPanel;
	private FolderSelectTree folderSelectTree;
	private Button cancelButton;
	private Button actionButton;
	private HasPropertyHandler propertyHandler;
	
	/**
	 * FolderSelectPopup
	 */
	public FolderSelectPopup() {
		// Establishes auto-close when click outside
		super(false, true);
		
		vPanel = new VerticalPanel();
		vPanel.setWidth("450");
		vPanel.setHeight("350");
		hPanel = new HorizontalPanel();	
		
		setText(Main.i18n("search.folder.filter"));
		
		scrollDirectoryPanel = new ScrollPanel();
		scrollDirectoryPanel.setSize("440", "300");
		scrollDirectoryPanel.setStyleName("okm-Popup-text");
		verticalDirectoryPanel = new VerticalPanel();
		verticalDirectoryPanel.setSize("100%", "100%");
		folderSelectTree = new FolderSelectTree();
		folderSelectTree.setSize("100%", "100%");
		
		verticalDirectoryPanel.add(folderSelectTree);
		scrollDirectoryPanel.add(verticalDirectoryPanel);
		
		cancelButton = new Button(Main.i18n("button.cancel"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		
		actionButton = new Button(Main.i18n("button.select"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (propertyHandler != null) {
					propertyHandler.metadataValueChanged();
				}
				Dropbox.get().subMenuDropbox.export(folderSelectTree.getActualPath());
				hide();
			}
		});
		
		vPanel.add(scrollDirectoryPanel);
		vPanel.add(new HTML("<br>"));
		hPanel.add(cancelButton);
		HTML space = new HTML();
		space.setWidth("50");
		hPanel.add(space);
		hPanel.add(actionButton);
		vPanel.add(hPanel);
		vPanel.add(new HTML("<br>"));
		
		vPanel.setCellHorizontalAlignment(scrollDirectoryPanel, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(hPanel, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHeight(scrollDirectoryPanel, "300");
		
		cancelButton.setStyleName("okm-NoButton");
		actionButton.setStyleName("okm-YesButton");
		
		super.hide();
		setWidget(vPanel);
	}
	
	/**
	 * Language refresh
	 */
	public void langRefresh() {
		setText(Main.i18n("search.folder.filter"));
		cancelButton.setText(Main.i18n("button.cancel"));
		actionButton.setText(Main.i18n("button.select"));
	}
	
	/**
	 * Shows the popup
	 */
	public void show() {
		int left = (Window.getClientWidth() - 450) / 2;
		int top = (Window.getClientHeight() - 350) / 2;
		setPopupPosition(left, top);
		
		super.show();
		// Resets to initial tree value
		folderSelectTree.reset();
	}
	
	/**
	 * Enables or disables move button
	 */
	public void enable(boolean enable) {
		actionButton.setEnabled(enable);
	}	
}