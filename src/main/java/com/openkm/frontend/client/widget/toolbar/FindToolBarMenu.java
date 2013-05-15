/**
*  Copyright (c) 2006-2013  Paco Avila & Josep Llort
 *
 *  No bytes were intentionally harmed during the development of this application.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.frontend.client.widget.toolbar;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTProfileToolbar;
import com.openkm.frontend.client.bean.ToolBarOption;
import com.openkm.frontend.client.util.Util;

/**
 * FindToolBarMenu
 * 
 * @author jllort
 *
 */
public class FindToolBarMenu extends DialogBox {
	private ToolBarOption toolBarOption;
	private MenuBar dirMenu;
	private MenuItem findFolder;
	private MenuItem findDocument;
	
	/**
	 * ResizeToolBarMenu
	 */
	public FindToolBarMenu() {
		// Establishes auto-close when click outside
		super(true, true);
		toolBarOption = new ToolBarOption();
		
		dirMenu = new MenuBar(true);
		dirMenu.setStyleName("okm-SubMenuBar");
		findFolder = new MenuItem(Util.menuHTML("img/icon/actions/folder_find.gif", Main.i18n("general.menu.file.find.folder")), true, findFolderOKM);
		findDocument = new MenuItem(Util.menuHTML("img/icon/actions/document_find.png", Main.i18n("general.menu.file.find.document")), true, findDocumentOKM);
		
		dirMenu.addItem(findFolder);
		dirMenu.addItem(findDocument);
		
		setWidget(dirMenu);
	}
	
	// Command menu to find folder
	Command findFolderOKM = new Command() {
		public void execute() {
			if (toolBarOption.findFolderOption) {
				Main.get().mainPanel.topPanel.toolBar.executeFindFolder();
				hide();
			}
		}
	};
	
	// Command menu to find document
	Command findDocumentOKM = new Command() {
		public void execute() {
			if (toolBarOption.findDocumentOption) {
				Main.get().mainPanel.topPanel.toolBar.executeFindDocument();
				hide();
			}
		}
	};
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		findFolder.setHTML(Util.menuHTML("img/icon/actions/normal_size.png", Main.i18n("general.menu.file.find.folder")));
		findDocument.setHTML(Util.menuHTML("img/icon/actions/document_find.png", Main.i18n("general.menu.file.find.document")));
	}
	
	/**
	 * setOptions
	 * 
	 * @param toolBarOption
	 */
	public void setOptions(ToolBarOption toolBarOption) {
		this.toolBarOption = toolBarOption;
		evaluateMenuOptions();
	}
	
	/**
	 * disableAllOptions
	 */
	public void disableAllOptions() {
		toolBarOption = new ToolBarOption();
		evaluateMenuOptions();
	}
	
	/**
	 * evaluateMenuOptions
	 */
	public void evaluateMenuOptions(){
		if (toolBarOption.findFolderOption) {enable(findFolder);} else {disable(findFolder);}
		if (toolBarOption.findDocumentOption) {enable(findDocument);} else {disable(findDocument);}
	}
	
	/**
	 * setAvailableOption
	 * 
	 * @param option
	 */
	public void setAvailableOption(GWTProfileToolbar option) {
		if (!option.isFindFolderVisible()) {
			dirMenu.removeItem(findFolder);
		}
		if (!option.isFindDocumentVisible()) {
			dirMenu.removeItem(findDocument);
		}
	}
	
	/**
	 * Enables menu item
	 * 
	 * @param menuItem The menu item
	 */
	public void enable(MenuItem menuItem) {
		menuItem.addStyleName("okm-MenuItem");
		menuItem.removeStyleName("okm-MenuItem-strike");
	}
	
	/**
	 * Disable the menu item
	 * 
	 * @param menuItem The menu item
	 */
	public void disable(MenuItem menuItem) {
		menuItem.removeStyleName("okm-MenuItem");
		menuItem.addStyleName("okm-MenuItem-strike");
	}
}