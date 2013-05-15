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

package com.openkm.extension.frontend.client.widget.dropbox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.UtilComunicator;

/**
 * Confirm panel
 * 
 * @author sochoa
 *
 */
public class ConfirmPopup extends DialogBox {
	public static final int NO_ACTION 				= 0;
	public static final int CONFIRM_EXPORT_DOCUMENT	= 1;
	public static final int CONFIRM_EXPORT_FOLDER		= 2;
	
	private VerticalPanel vPanel;
	private HorizontalPanel hPanel;
	private HTML text;
	private Button cancelButton;
	private Button acceptButton;
	private int action = 0;
	
	/**
	 * Confirm popup
	 */
	public ConfirmPopup() {
		// Establishes auto-close when click outside
		super(false,true);
		
		setText(GeneralComunicator.i18nExtension("confirm.label"));
		
		vPanel = new VerticalPanel();
		hPanel = new HorizontalPanel();
		text = new HTML();
		text.setStyleName("okm-NoWrap");
		
		cancelButton = new Button(GeneralComunicator.i18n("button.cancel"), new ClickHandler() { 
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		
		acceptButton = new Button(GeneralComunicator.i18n("button.accept"), new ClickHandler() { 
			@Override
			public void onClick(ClickEvent event) {
				execute();
				hide();
			}
		});

		vPanel.setWidth("300px");
		vPanel.setHeight("50px");
		cancelButton.setStyleName("okm-NoButton");
		acceptButton.setStyleName("okm-YesButton");

		text.setHTML("");
		
		hPanel.add(cancelButton);
		hPanel.add(UtilComunicator.hSpace("5"));
		hPanel.add(acceptButton);
		
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(text);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(hPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		
		vPanel.setCellHorizontalAlignment(text, VerticalPanel.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(hPanel, VerticalPanel.ALIGN_CENTER);

		super.hide();
		setWidget(vPanel);
	}
	
	/**
	 * Execute the confirmed action
	 */
	private void execute() {
		switch (action) {
			case CONFIRM_EXPORT_DOCUMENT :
			case CONFIRM_EXPORT_FOLDER :
				Dropbox.get().folderSelectPopup.show();				
				break;
		}
		action = NO_ACTION; // Resets action value
	}
	
	/**
	 * Sets the action to be confirmed
	 * 
	 * @param action The action to be confirmed
	 */
	public void setConfirm(int action) {
		this.action = action;
		switch (action) {
			case CONFIRM_EXPORT_DOCUMENT :
				text.setHTML(GeneralComunicator.i18nExtension("dropbox.confirm.export.document"));
				break;
			case CONFIRM_EXPORT_FOLDER :
				text.setHTML(GeneralComunicator.i18nExtension("dropbox.confirm.export.folder"));
				break;
		}
	}
	
	/**
	 * Language refresh
	 */
	public void langRefresh() {
		setText(GeneralComunicator.i18n("confirm.label"));
		cancelButton.setText(GeneralComunicator.i18n("button.cancel"));
		acceptButton.setText(GeneralComunicator.i18n("button.accept"));
	}
	
	/**
	 * Shows de popup
	 */
	public void show(){
		setText(GeneralComunicator.i18n("confirm.label"));
		int left = (Window.getClientWidth()-300)/2;
		int top = (Window.getClientHeight()-125)/2;
		setPopupPosition(left,top);
		super.show();
	}
}