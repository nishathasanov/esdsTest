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
package com.openkm.frontend.client.widget.properties;

import java.util.Collection;

import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTFolder;
import com.openkm.frontend.client.bean.GWTMail;
import com.openkm.frontend.client.bean.GWTPermission;
import com.openkm.frontend.client.constants.ui.UIDesktopConstants;
import com.openkm.frontend.client.util.Util;
import com.openkm.frontend.client.widget.properties.CategoryManager.CategoryToRemove;
import com.openkm.frontend.client.widget.properties.KeywordManager.KeywordToRemove;
import com.openkm.frontend.client.widget.thesaurus.ThesaurusSelectPopup;

/**
 * Mail
 * 
 * @author jllort
 *
 */
public class Mail extends Composite {
	private FlexTable table;
	private FlexTable tableProperties;
	private ScrollPanel scrollPanel;
	private GWTMail mail;
	private CategoryManager categoryManager;
	private KeywordManager keywordManager;
	private boolean remove = true;
	private boolean visible = true;
	
	/**
	 * Mail
	 */
	public Mail() {
		mail = new GWTMail();
		categoryManager = new CategoryManager(CategoryManager.ORIGIN_MAIL);
		keywordManager = new KeywordManager(ThesaurusSelectPopup.MAIL_PROPERTIES);
		table = new FlexTable();
		tableProperties = new FlexTable();		
		scrollPanel = new ScrollPanel(table);
		
		tableProperties.setHTML(0, 0, "<b>"+Main.i18n("mail.uuid")+"</b>");
		tableProperties.setHTML(0, 1, "");
		tableProperties.setHTML(1, 0, "<b>"+Main.i18n("mail.name")+"</b>");
		tableProperties.setHTML(1, 1, "");
		tableProperties.setHTML(2, 0, "<b>"+Main.i18n("mail.folder")+"</b>");
		tableProperties.setHTML(3, 1, "");
		tableProperties.setHTML(3, 0, "<b>"+Main.i18n("mail.size")+"</b>");
		tableProperties.setHTML(4, 1, "");
		tableProperties.setHTML(4, 0, "<b>"+Main.i18n("mail.created")+"</b>");
		tableProperties.setHTML(5, 1, "");
		tableProperties.setHTML(6, 0, "<b>"+Main.i18n("mail.mimetype")+"</b>");
		tableProperties.setHTML(6, 1, "");
		tableProperties.setHTML(7, 0, "<b>"+Main.i18n("mail.keywords")+"</b>");
		tableProperties.setHTML(7, 1, "");
		tableProperties.setHTML(8, 0, "<b>"+Main.i18n("mail.url")+"</b>");
		tableProperties.setWidget(8, 1, new HTML(""));
		tableProperties.setHTML(9, 0, "<b>"+Main.i18n("mail.webdav")+"</b>");
		tableProperties.setWidget(9, 1, new HTML(""));
		// Sets wordWrap for al rows
		for (int i=0; i<10; i++) {
			setRowWordWarp(i, 0, true, tableProperties);
		}
		tableProperties.getCellFormatter().setVerticalAlignment(7, 0, HasAlignment.ALIGN_TOP);
		tableProperties.setStyleName("okm-DisableSelect");
		
		// Sets the tagcloud
		keywordManager.getKeywordCloud().setWidth("350");
		
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.add(keywordManager.getKeywordCloudText());
		vPanel.add(keywordManager.getKeywordCloud());
		HTML space = new HTML("");
		vPanel.add(space);
		vPanel.setCellHeight(space, "10");
		vPanel.add(categoryManager.getPanelCategories());
		vPanel.add(categoryManager.getSubscribedCategoriesTable());
		
		table.setWidget(0, 0, tableProperties);
		table.setHTML(0, 1, "");
		table.setWidget(0, 2, vPanel);
		
		table.getFlexCellFormatter().setWidth(0, 1, "25");
		table.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
		table.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
		
		initWidget(scrollPanel);
	}
	
	/**
	 * get
	 * 
	 * @return
	 */
	public GWTMail get() {
		return mail;
	}
	
	/**
	 * set
	 * 
	 * @param mail
	 */
	public void set(GWTMail mail) {
		this.mail = mail;
		
		// URL clipboard button
		String url = Main.get().workspaceUserProperties.getApplicationURL();
		url += "?uuid=" + URL.encodeQueryString(URL.encodeQueryString(mail.getUuid()));
		tableProperties.setWidget(8, 1, new HTML("<div id=\"urlclipboardcontainer\"></div>\n"));
		Util.createURLClipboardButton(url);
		
		// Webdav button
		String webdavUrl = Main.get().workspaceUserProperties.getApplicationURL();
		String webdavPath = mail.getPath();
		
		// Replace only in case webdav fix is enabled
		if (Main.get().workspaceUserProperties.getWorkspace().isWebdavFix()) {
			webdavPath = webdavPath.replace("okm:", "okm_");
		}
		
		// Login case write empty folder
		if (!webdavUrl.equals("")) {
			// webdavPath = Util.encodePathElements(webdavPath);
			webdavUrl = webdavUrl.substring(0, webdavUrl.lastIndexOf('/')) + "/webdav" + webdavPath;	
		}
		
		tableProperties.setWidget(9, 1, new HTML("<div id=\"webdavclipboardcontainer\"></div>\n"));
		Util.createWebDavClipboardButton(webdavUrl);
		
		tableProperties.setHTML(0, 1, mail.getUuid());
		tableProperties.setHTML(1, 1, mail.getSubject());
		tableProperties.setHTML(2, 1, mail.getParentPath());
		tableProperties.setHTML(3, 1, Util.formatSize(mail.getSize()));
		DateTimeFormat dtf = DateTimeFormat.getFormat(Main.i18n("general.date.pattern"));
		tableProperties.setHTML(4, 1, dtf.format(mail.getCreated())+" "+Main.i18n("mail.by")+" "+mail.getAuthor());
		tableProperties.setHTML(6, 1, mail.getMimeType());
		tableProperties.setWidget(7, 1, keywordManager.getKeywordPanel());
		
		remove = ((mail.getPermissions() & GWTPermission.WRITE) == GWTPermission.WRITE && visible);
		
		// Enables or disables change keywords with user permissions and document is not check-out or locked
		if (remove)  {
			keywordManager.setVisible(true);
			categoryManager.setVisible(true);
		} else {
			keywordManager.setVisible(false);
			categoryManager.setVisible(false);
		}
		
		// Sets wordWrap for al rows
		for (int i=0; i<8; i++) {
			setRowWordWarp(i, 1, true, tableProperties);
		}
		
		// Some preoperties only must be visible on taxonomy or trash view
		int actualView = Main.get().mainPanel.desktop.navigator.getStackIndex();
		
		if (actualView == UIDesktopConstants.NAVIGATOR_TRASH) {
			tableProperties.getCellFormatter().setVisible(7,0,false);
			tableProperties.getCellFormatter().setVisible(7,1,false);
		} else {
			tableProperties.getCellFormatter().setVisible(7,0,true);
			tableProperties.getCellFormatter().setVisible(7,1,true);
		}
		
		// keywords
		keywordManager.reset();
		keywordManager.setObject(mail, remove);
		keywordManager.drawAll();
		
		// Categories
		categoryManager.removeAllRows();
		categoryManager.setObject(mail, remove);
		categoryManager.drawAll();
	}
	
	/**
	 * addKeyword document
	 */
	public void addKeyword(String keyword) {
		keywordManager.addKeyword(keyword);
	}
	
	/**
	 * removeKeyword document
	 */
	public void removeKeyword(String keyword) {
		keywordManager.removeKeyword(keyword);
	}
	
	/**
	 * removeKeyword
	 * 
	 * @param ktr
	 */
	public void removeKeyword(KeywordToRemove ktr) {
		keywordManager.removeKeyword(ktr);
	}
	
	/**
	 * addCategory document
	 */
	public void addCategory(GWTFolder category) {
		categoryManager.addCategory(category);
	}
	
	/**
	 * removeCategory document
	 */
	public void removeCategory(String UUID) {
		categoryManager.removeCategory(UUID);
	}
	
	/**
	 * removeCategory
	 * 
	 * @param category
	 */
	public void removeCategory(CategoryToRemove obj) {
		categoryManager.removeCategory(obj);
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		tableProperties.setHTML(0, 0, "<b>" + Main.i18n("mail.uuid")+"</b>");
		tableProperties.setHTML(1, 0, "<b>" + Main.i18n("mail.name")+"</b>");
		tableProperties.setHTML(2, 0, "<b>" + Main.i18n("mail.folder")+"</b>");
		tableProperties.setHTML(3, 0, "<b>" + Main.i18n("mail.size")+"</b>");
		tableProperties.setHTML(4, 0, "<b>" + Main.i18n("mail.created")+"</b>");
		tableProperties.setHTML(6, 0, "<b>" + Main.i18n("mail.mimetype")+"</b>");
		tableProperties.setHTML(7, 0, "<b>" + Main.i18n("mail.keywords")+"</b>");
		tableProperties.setHTML(8, 0, "<b>" + Main.i18n("mail.url")+"</b>");
		tableProperties.setHTML(9, 0, "<b>" + Main.i18n("mail.webdav")+"</b>");
		keywordManager.langRefresh();
		categoryManager.langRefresh();
	}
	
	/**
	 * Sets visibility to buttons ( true / false )
	 * 
	 * @param visible The visible value
	 */
	public void setVisibleButtons(boolean visible) {
		this.visible = visible;
		keywordManager.setVisible(visible);
		categoryManager.setVisible(visible);
	}
	
	/**
	 * Removes a key
	 * 
	 * @param keyword The key to be removed
	 */
	public void removeKey(String keyword) {
		keywordManager.removeKey(keyword);
	}
	
	/**
	 * addKeywordToPendinList
	 * 
	 * @param key
	 */
	public void addKeywordToPendinList(String key) {
		keywordManager.addKeywordToPendinList(key);
	}
	
	/**
	 * Adds keywords sequentially
	 * 
	 */
	public void addPendingKeyWordsList() {
		keywordManager.addPendingKeyWordsList();
	}
	
	/**
	 * getKeywords
	 * 
	 * @return
	 */
	public Collection<String>  getKeywords() {
		return mail.getKeywords();
	}
	
	/**
	 * @param enabled
	 */
	public void setKeywordEnabled(boolean enabled) {
		keywordManager.setKeywordEnabled(enabled);
	}
	
	/**
	 * Set the WordWarp for all the row cells
	 * 
	 * @param row The row cell
	 * @param columns Number of row columns
	 * @param warp
	 * @param table The table to change word wrap
	 */
	private void setRowWordWarp(int row, int columns, boolean warp, FlexTable table) {
		CellFormatter cellFormatter = table.getCellFormatter();
		
		for (int i=0; i<columns; i++) {
			cellFormatter.setWordWrap(row, i, warp);
		}
	}
	
	/**
	 * showAddCategory
	 */
	public void showAddCategory() {
		categoryManager.showAddCategory();
	}
	
	/**
	 * showRemoveCategory
	 */
	public void showRemoveCategory() {
		categoryManager.showRemoveCategory();
	}
	
	/**
	 * showAddKeyword
	 */
	public void showAddKeyword() {
		keywordManager.showAddKeyword();
	}
	
	/**
	 * showRemoveKeyword
	 */
	public void showRemoveKeyword() {
		keywordManager.showRemoveKeyword();
	}
}