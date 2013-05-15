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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.extension.frontend.client.bean.GWTDropboxStatusListener;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.UtilComunicator;
import com.openkm.frontend.client.util.Util;

/**
 * StatusListenerPopup
 * 
 * @author jllort
 */
public class StatusListenerPopup extends DialogBox {
	public final static int ACTION_IMPORT = 0;
	public final static int ACTION_EXPORT = 1;
	
	private ScrollPanel scrollPanel;
	private VerticalPanel vPanel;
	private FlexTable table;
	public Button closeButton;
	
	public StatusListenerPopup() {
		// Establishes auto-close when click outside
		super(false, true);
		
		vPanel = new VerticalPanel();
		vPanel.setWidth("100%");
		
		table = new FlexTable();
		table.setCellPadding(2);
		table.setCellSpacing(0);
		table.setWidth("100%");
		
		scrollPanel = new ScrollPanel();
		scrollPanel.setStyleName("gwt-ScrollTable");
		scrollPanel.addStyleName("okm-Input");
		scrollPanel.setSize("600px", "250px");
		scrollPanel.add(table);
		
		// closeButton
		closeButton = new Button(GeneralComunicator.i18nExtension("button.close"));
		closeButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		closeButton.setStyleName("okm-YesButton");
		
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(scrollPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(closeButton);
		vPanel.add(UtilComunicator.vSpace("5"));
		
		vPanel.setCellHorizontalAlignment(scrollPanel, VerticalPanel.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(closeButton, VerticalPanel.ALIGN_CENTER);
		
		setWidget(vPanel);
	}
	
	/**
	 * reset
	 */
	public void reset(int action) {
		switch (action) {
			case ACTION_IMPORT:
				setText(GeneralComunicator.i18nExtension("dropbox.status.listener.import.title"));
				break;
			case ACTION_EXPORT:
				setText(GeneralComunicator.i18nExtension("dropbox.status.listener.export.title"));
				break;
		}
		
		table.removeAllRows();
		closeButton.setVisible(false);
	}
	
	/**
	 * add
	 */
	public void add(GWTDropboxStatusListener dsl) {
		int row = table.getRowCount();
		
		if (dsl.getFolder() != null) {
			if (dsl.getFolder().isHasChildren()) {
				table.setHTML(row, 0, Util.imageItemHTML("img/menuitem_childs.gif"));
			} else {
				table.setHTML(row, 0, Util.imageItemHTML("img/menuitem_empty.gif"));
			}
			
			HTML name = new HTML(dsl.getFolder().getPath());
			table.setWidget(row, 1, name);
			table.getFlexCellFormatter().setWidth(row, 0, "20");
			table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
			scrollPanel.ensureVisible(name);
		} else if (dsl.getDocument() != null) {
			table.setHTML(row, 0, Util.mimeImageHTML(dsl.getDocument().getMimeType()));
			HTML name = new HTML(dsl.getDocument().getPath());
			table.setWidget(row, 1, name);
			table.getFlexCellFormatter().setWidth(row, 0, "20");
			table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
			scrollPanel.ensureVisible(name);
		}
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		closeButton.setHTML(GeneralComunicator.i18nExtension("button.close"));
	}
}