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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.widget.richtext.RichTextToolbar;

/**
 * Notes popup
 * 
 * @author jllort
 */
public class NotesPopup extends DialogBox {
	private VerticalPanel newNotePanel;
	private Button cancel;
	private Button add;
	public RichTextArea richTextArea;
	private RichTextToolbar richTextToolbar;
	private Grid gridRichText;
	private Status status;
	
	/**
	 * NotesPopup
	 */
	public NotesPopup() {
		// Establishes auto-close when click outside
		super(false, true);
		setText(Main.i18n("general.menu.edit.add.note"));
		
		// Status
		status = new Status(this);
		status.setStyleName("okm-StatusPopup");
		
		newNotePanel = new VerticalPanel();
		
		richTextArea = new RichTextArea();
		richTextArea.setSize("100%", "14em");
		richTextToolbar = new RichTextToolbar(richTextArea);
		
		gridRichText = new Grid(2, 1);
		gridRichText.setStyleName("RichTextToolbar");
		gridRichText.addStyleName("okm-Input");
		gridRichText.setWidget(0, 0, richTextToolbar);
		gridRichText.setWidget(1, 0, richTextArea);
		newNotePanel.add(gridRichText);
		
		add = new Button(Main.i18n("button.add"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addNote();
			}
		});
		add.setStyleName("okm-AddButton");
		
		cancel = new Button(Main.i18n("button.cancel"), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				reset();
				hide();
			}
		});
		cancel.setStyleName("okm-NoButton");
		
		HorizontalPanel hPanel = new HorizontalPanel();
		hPanel.add(add);
		hPanel.add(new HTML("&nbsp;"));
		hPanel.add(cancel);
		newNotePanel.add(hPanel);
		
		newNotePanel.setCellHorizontalAlignment(hPanel, HasAlignment.ALIGN_CENTER);
		newNotePanel.setCellHeight(hPanel, "25");
		newNotePanel.setCellVerticalAlignment(hPanel, HasAlignment.ALIGN_MIDDLE);
		
		hide();
		setWidget(newNotePanel);
	}
	
	/**
	 * addNote
	 */
	private void addNote() {
		if (Main.get().mainPanel.desktop.browser.fileBrowser.isDocumentSelected()) {
			Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.notes.addNote(richTextArea.getHTML());
		} else if (Main.get().mainPanel.desktop.browser.fileBrowser.isFolderSelected()) {
			Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.notes.addNote(richTextArea.getHTML());
		} else if (Main.get().mainPanel.desktop.browser.fileBrowser.isMailSelected()) {
			Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.notes.addNote(richTextArea.getHTML());
		}
		hide();
	}
	
	/**
	 * reset
	 */
	private void reset() {
		richTextArea.setText("");
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		setText(Main.i18n("general.menu.edit.add.note"));
		add.setHTML(Main.i18n("button.add"));
		cancel.setHTML(Main.i18n("button.cancel"));
		richTextToolbar.langRefresh();
	}
}