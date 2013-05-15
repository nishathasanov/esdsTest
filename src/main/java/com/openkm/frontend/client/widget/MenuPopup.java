/**
 *  ESDS, Open Document Management System (http://www.openkm.com)
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

package com.openkm.frontend.client.widget;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.bean.ToolBarOption;

/**
 * Root menu popup
 * 
 * @author jllort
 *
 */
public class MenuPopup extends PopupPanel {
	public VerticalPanel panel;
	public MenuBase menu;
	
	public MenuPopup(MenuBase menu) {
		// Establishes auto-close when click outside
		super(true, true);
		
		this.menu = menu;
		panel = new VerticalPanel();
		panel.add(menu);
		setWidget(panel);
	}
	
	/**
	 * Refresh language values
	 */
	public void langRefresh() {
		menu.langRefresh();
	}
	
	/**
	 * setOptions
	 * 
	 * @param toolBarOption
	 */
	public void setOptions(ToolBarOption toolBarOption) {
		menu.setOptions(toolBarOption);
	}
	
	/**
	 * Disables all menu options
	 */
	public void disableAllOptions(){
		menu.disableAllOptions();
	}
	
	/**
	 * enableAddPropertyGroup
	 */
	public void enableAddPropertyGroup() {
		menu.enableAddPropertyGroup();
	}
	
	/**
	 * disableAddPropertyGroup
	 */
	public void disableAddPropertyGroup() {
		menu.disableAddPropertyGroup();
	}
}