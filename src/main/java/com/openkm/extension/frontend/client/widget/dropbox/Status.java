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

package com.openkm.extension.frontend.client.widget.dropbox;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.openkm.extension.frontend.client.util.OKMBundleResources;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;

/**
 * Status
 * 
 * @author jllort
 * 
 */
public class Status extends PopupPanel {
	private HorizontalPanel hPanel;
	private HTML msg;
	private HTML space;
	private Image image;
	private Widget widget;
	
	private boolean flag_exporting = false;
	private boolean flag_importing = false;
	private boolean flag_search = false;
	private boolean flag_getChilds = false;
	
	/**
	 * The status
	 */
	public Status() {
		super(false, true);
		hPanel = new HorizontalPanel();
		image = new Image(OKMBundleResources.INSTANCE.indicator());
		msg = new HTML("");
		space = new HTML("");
		
		hPanel.add(image);
		hPanel.add(msg);
		hPanel.add(space);
		
		hPanel.setCellVerticalAlignment(image, HasAlignment.ALIGN_MIDDLE);
		hPanel.setCellVerticalAlignment(msg, HasAlignment.ALIGN_MIDDLE);
		hPanel.setCellHorizontalAlignment(image, HasAlignment.ALIGN_CENTER);
		hPanel.setCellWidth(image, "30px");
		hPanel.setCellWidth(space, "7px");
		
		hPanel.setHeight("25px");
		
		msg.setStyleName("okm-NoWrap");
		
		super.hide();
		setWidget(hPanel);
	}
	
	/**
	 * Refreshing the panel
	 */
	public void refresh() {
		if (flag_exporting || flag_importing || flag_search || flag_getChilds) {
			Widget widgetBaseToCenter = Main.get().mainPanel;
			if (flag_getChilds && widget!=null) {
				widgetBaseToCenter = widget;
			} 
			
			int	left = widgetBaseToCenter.getAbsoluteLeft() + (widgetBaseToCenter.getOffsetWidth() - 200) / 2;
			int	top = widgetBaseToCenter.getAbsoluteTop() + (widgetBaseToCenter.getOffsetHeight() - 40) / 2;
			
			if (left > 0 && top > 0) {
				setPopupPosition(left, top);
				super.show();
			}
		} else {
			super.hide();
		}
	}
	
	
	
	/**
	 * Sets the exporting log flag
	 */
	public void setExporting() {
		msg.setHTML(GeneralComunicator.i18nExtension("status.dropbox.exporting"));
		flag_exporting = true;
		refresh();
	}
	
	/**
	 * Unset the exporting log flag
	 */
	public void unsetExporting() {
		flag_exporting = false;
		refresh();
	}
	
	/**
	 * Sets the importing log flag
	 */
	public void setImporting() {
		msg.setHTML(GeneralComunicator.i18nExtension("status.dropbox.importing"));
		flag_importing = true;
		refresh();
	}
	
	/**
	 * Unset the importing log flag
	 */
	public void unsetImporting() {
		flag_importing = false;
		refresh();
	}
	
	/**
	 * Sets the search log flag
	 */
	public void setSearch() {
		msg.setHTML(GeneralComunicator.i18nExtension("status.dropbox.search"));
		flag_search = true;
		refresh();
	}
	
	/**
	 * Unset the search log flag
	 */
	public void unsetSearch() {
		flag_search = false;
		refresh();
	}
	
	/**
	 * Sets the get childs flag
	 */
	public void setGetChilds(Widget widget) {
		this.widget = widget;
		msg.setHTML(Main.i18n("status.dropbox.refresh.folder"));
		flag_getChilds = true;
		refresh();
	}
	
	/**
	 * Unset the get childs flag
	 */
	public void unsetGetChilds() {
		flag_getChilds = false;
		refresh();
	}
	
}