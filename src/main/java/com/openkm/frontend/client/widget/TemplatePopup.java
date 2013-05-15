/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
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

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTDocument;
import com.openkm.frontend.client.bean.GWTPropertyGroup;
import com.openkm.frontend.client.service.OKMDocumentService;
import com.openkm.frontend.client.service.OKMDocumentServiceAsync;
import com.openkm.frontend.client.service.OKMPropertyGroupService;
import com.openkm.frontend.client.service.OKMPropertyGroupServiceAsync;
import com.openkm.frontend.client.util.CommonUI;
import com.openkm.frontend.client.util.Util;
/**
 * TemplatePopup
 * s
 * @author jllort
 *
 */
public class TemplatePopup extends DialogBox {
	private final OKMDocumentServiceAsync documentService = (OKMDocumentServiceAsync) GWT.create(OKMDocumentService.class);
	private final OKMPropertyGroupServiceAsync propertyGroupService = (OKMPropertyGroupServiceAsync) GWT.create(OKMPropertyGroupService.class);
	
	private VerticalPanel vPanel;
	private HorizontalPanel hPanel;
	private HorizontalPanel hButtonPanel;
	private HTML nameText;
	private TextBox name;
	private Button cancel;
	private Button create;
	private GWTDocument doc;
	private String dstFldPath;
	private boolean open = false;
	
	public TemplatePopup() {
		// Establishes auto-close when click outside
		super(false,true);
		
		setText(Main.i18n("template.new.document.title"));
		
		// Name
		hPanel = new HorizontalPanel();
		nameText = new HTML(Main.i18n("template.new.document.name"));
		name = new TextBox();
		name.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (name.getText().length()>0) {
					if (KeyCodes.KEY_ENTER == event.getNativeKeyCode()) {
						create();
					}
					create.setEnabled(true);
				} else {
					create.setEnabled(false);
				}
			}
		});
		name.setWidth("250");
		name.setStyleName("okm-Input");
		
		hPanel.add(nameText);
		hPanel.add(Util.hSpace("5"));
		hPanel.add(name);
		
		hPanel.setCellVerticalAlignment(nameText, HasAlignment.ALIGN_MIDDLE);
		hPanel.setCellVerticalAlignment(name, HasAlignment.ALIGN_MIDDLE);
		
		// Buttons
		cancel =  new Button(Main.i18n("button.cancel"));
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		cancel.setStyleName("okm-NoButton");
		
		create =  new Button(Main.i18n("button.create"));
		create.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				create.setEnabled(false);
				create();
			}
		});
		create.setStyleName("okm-AddButton");
		
		hButtonPanel = new HorizontalPanel();
		hButtonPanel.add(cancel);
		hButtonPanel.add(Util.hSpace("5"));
		hButtonPanel.add(create);
		
		vPanel = new VerticalPanel();
		vPanel.setWidth("100%");
		vPanel.add(Util.vSpace("5"));
		vPanel.add(hPanel);
		vPanel.add(Util.vSpace("5"));
		vPanel.add(hButtonPanel);
		vPanel.add(Util.vSpace("5"));
		
		vPanel.setCellHorizontalAlignment(hPanel, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(hButtonPanel, HasAlignment.ALIGN_CENTER);
		
		setWidget(vPanel);
	}
	
	/**
	 * reset
	 */
	public void reset(GWTDocument doc, String dstFldpath, boolean openFldPath) {
		this.doc = doc;
		this.dstFldPath = dstFldpath;
		this.open = openFldPath;
		name.setText(doc.getName());
		create.setEnabled(true);
	}
	
	/**
	 * create
	 */
	public void create() {
		propertyGroupService.getGroups(doc.getPath(), new AsyncCallback<List<GWTPropertyGroup>>() {
			@Override
			public void onSuccess(List<GWTPropertyGroup> result) {
				// Has property groups and mime type to fill fields
				if ((doc.getMimeType().equals("application/pdf") || 
					doc.getMimeType().equals("text/html") || 
					(doc.getMimeType().equals("application/vnd.oasis.opendocument.text") && doc.getName().endsWith("odt"))) && 
					result.size()>0) {
					Main.get().templateWizardPopup.start(doc.getPath(), dstFldPath + "/" + name.getText(), open);
					hide();
				} else {
					Main.get().mainPanel.desktop.browser.fileBrowser.status.setFlagCreateFromTemplate();
					documentService.createFromTemplate(doc.getPath(), dstFldPath, name.getText(), new AsyncCallback<GWTDocument>() {
						@Override
						public void onSuccess(GWTDocument result) {
							Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCreateFromTemplate();
							
							// If are in same stack view is not needed all path sequence ( create from menu )
							if (open) {
								CommonUI.openPath(result.getParentPath(), result.getPath());
							} else {
								Main.get().mainPanel.desktop.browser.fileBrowser.mantainSelectedRowByPath(result.getPath());
								Main.get().mainPanel.desktop.browser.fileBrowser.refresh(Main.get().activeFolderTree.getActualPath());
							}
							
							Main.get().workspaceUserProperties.getUserDocumentsSize();
							hide();
						}
						
						@Override
						public void onFailure(Throwable caught) {
							Main.get().mainPanel.desktop.browser.fileBrowser.status.unsetFlagCreateFromTemplate();
							Main.get().showError("createFromTemplate", caught);
						}
					});
				}
			}
			@Override
			public void onFailure(Throwable caught) {
				Main.get().showError("getAllGroups", caught);
			}
		});
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		setText(Main.i18n("template.new.document.title"));
		nameText.setHTML(Main.i18n("template.new.document.name"));
	}
}