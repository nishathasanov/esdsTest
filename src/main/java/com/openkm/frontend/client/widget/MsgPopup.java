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

package com.openkm.frontend.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTUINotification;

/**
 * Message popup
 * 
 * @author jllort
 * 
 */
public class MsgPopup extends DialogBox implements ClickHandler {
	// private PopupPanel panel;
	private VerticalPanel vPanel;
	private Button button;
	private ScrollPanel sPanel;
	private FlexTable table;
	private int lastId = -1;
	
	/**
	 * MsgPopup
	 */
	public MsgPopup() {
		// Establishes auto-close when click outside
		super(false, true);
		setTitle(Main.i18n("msg.title"));
		
		table = new FlexTable();
		table.setCellPadding(2);
		table.setCellSpacing(0);
		table.setWidth("100%");
		vPanel = new VerticalPanel();
		sPanel = new ScrollPanel();
		
		button = new Button(Main.i18n("button.close"), this);
		
		vPanel.setWidth("500px");
		vPanel.setHeight("240px");
		sPanel.setWidth("480px");
		sPanel.setHeight("200px");
		sPanel.setStyleName("okm-Popup-text");
		
		vPanel.add(new HTML("<br>"));
		sPanel.add(table);
		vPanel.add(sPanel);
		vPanel.add(new HTML("<br>"));
		vPanel.add(button);
		vPanel.add(new HTML("<br>"));
		
		vPanel.setCellHorizontalAlignment(table, VerticalPanel.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(sPanel, VerticalPanel.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(button, VerticalPanel.ALIGN_CENTER);
		
		center();
		button.setStyleName("okm-YesButton");
		table.setStyleName("okm-NoWrap");
		
		hide();
		setWidget(vPanel);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 */
	public void onClick(ClickEvent event) {
		hide();
	}
	
	/**
	 * Language refresh
	 */
	public void langRefresh() {
		button.setText(Main.i18n("button.close"));
		setTitle(Main.i18n("msg.title"));
	}
	
	/**
	 * reset
	 */
	public void reset() {
		while (table.getRowCount() > 0) {
			table.removeRow(0);
		}
	}
	
	/**
	 * Add message notification.
	 */
	public void add(GWTUINotification uin) {
		int row = table.getRowCount();
		DateTimeFormat dtf = DateTimeFormat.getFormat(Main.i18n("general.date.pattern"));
		table.setHTML(row, 0, "<b>" + dtf.format(uin.getDate()) + "</b>");
		table.setHTML(row, 1, uin.getMessage());
		table.getCellFormatter().setWidth(row, 1, "100%");
		table.getCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
		table.getCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
		if (uin.getId()>lastId) {
			if (uin.getAction() == GWTUINotification.ACTION_LOGOUT) {
				row++;
				int seconds = 240;
				HTML countDown = new HTML(Main.i18n("ui.logout") + " " + secondsToHTML(seconds));
				table.setWidget(row, 0, countDown);
				table.getFlexCellFormatter().setColSpan(row, 0, 2);
				table.getCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
				logout(countDown, seconds);
				center();
			}
			if (uin.isShow()) {
				center();
			}
		}
	}
	
	/**
	 * setLastUIId
	 * 
	 * @param id
	 */
	public void setLastUIId(int id) {
		this.lastId = id;
	}
	
	/**
	 * logout
	 */
	private void logout(final HTML countDown, final int seconds) {
		Timer timer = new Timer() {
			@Override
			public void run() {
				countDown.setHTML(Main.i18n("ui.logout") + " " + secondsToHTML(seconds));
				
				if (seconds > 1) {
					logout(countDown, seconds - 1);
				} else {
					hide();
					Main.get().logoutPopup.logout();
				}
			}
		};
		
		timer.schedule(1000);
	}
	
	/**
	 * secondsToHTML
	 */
	private String secondsToHTML(int seconds) {
		return "0" + (seconds / 60) + ":" + ((seconds % 60 > 9) ? (seconds % 60) : "0" + (seconds % 60));
	}
}