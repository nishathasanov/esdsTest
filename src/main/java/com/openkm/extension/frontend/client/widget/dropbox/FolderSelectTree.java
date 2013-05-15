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

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.openkm.extension.frontend.client.bean.GWTDropboxEntry;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.extension.frontend.client.service.OKMDropboxServiceAsync;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.util.Util;

/**
 * Folder tree
 * 
 * @author sochoa
 * 
 */
public class FolderSelectTree extends Composite {
	private Tree tree;
	private TreeItem actualItem;
	private final OKMDropboxServiceAsync dropboxService = (OKMDropboxServiceAsync) GWT.create(OKMDropboxService.class);
	TreeItem rootItem = new TreeItem(Util.imageItemHTML("img/menuitem_childs.gif", "Dropbox", "top"));
	
	/**
	 * Folder Tree
	 */
	public FolderSelectTree() {
		tree = new Tree();
		rootItem.setStyleName("okm-TreeItem");
		rootItem.setUserObject(new GWTDropboxEntry());
		rootItem.setSelected(true);
		rootItem.setState(true);
		tree.setStyleName("okm-Tree");
		tree.addItem(rootItem);
		tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
			@Override
			public void onSelection(SelectionEvent<TreeItem> event) {
				boolean refresh = true;
				TreeItem item = event.getSelectedItem();				
				// Case that not refreshing tree and file browser ( right click )
				if (actualItem.equals(item)) {
					refresh = false;
				} else {
					// Disables actual item because on changing active node by
					// application this it's not changed automatically
					if (!actualItem.equals(item)) {
						actualItem.setSelected(false);
						actualItem = item;
					} else {
						refresh = false;
					}
				}
				
				if (refresh) {
					refresh(true);
				}
			}
		});
		actualItem = tree.getItem(0);
		initWidget(tree);
	}
	
	/**
	 * Resets all tree values
	 */
	public void reset() {
		actualItem = rootItem;
		actualItem.setSelected(true);
		while (actualItem.getChildCount() > 0) {
			actualItem.getChild(0).remove();
		}
		getRoot();
	}
	
	/**
	 * Refresh asyncronous subtree branch
	 */
	final AsyncCallback<List<GWTDropboxEntry>> callbackGetChilds = new AsyncCallback<List<GWTDropboxEntry>>() {
		public void onSuccess(List<GWTDropboxEntry> result) {
			boolean directAdd = true;
			
			// If has no childs directly add values is permited
			if (actualItem.getChildCount() > 0) {
				directAdd = false;
				// to prevent remote folder remove it disables all tree branch
				// items and after sequentially activate
				hideAllBranch(actualItem);
			}
			
			// On refreshing not refreshed the actual item values but must
			// ensure that has childs value is consistent
			if (result.isEmpty()) {
				((GWTDropboxEntry) actualItem.getUserObject()).setChildren(false);
			} else {
				((GWTDropboxEntry) actualItem.getUserObject()).setChildren(true);
			}
			
			// Ads folders childs if exists
			for (Iterator<GWTDropboxEntry> it = result.iterator(); it.hasNext();) {
				GWTDropboxEntry entry = it.next();
				TreeItem folderItem = new TreeItem(entry.getFileName());
				folderItem.setUserObject(entry);
				folderItem.setStyleName("okm-TreeItem");
				
				// If has no childs directly add values is permited, else
				// evalues each node to refresh, remove or add
				if (directAdd) {
					evaluesFolderIcon(folderItem);
					actualItem.addItem(folderItem);
				} else {
					// sequentially activate items and refreshes values
					addFolder(actualItem, folderItem);
				}
			}
			
			actualItem.setState(true);
			evaluesFolderIcon(actualItem);
			Dropbox.get().status.unsetGetChilds();
		}
		
		public void onFailure(Throwable caught) {
			Main.get().showError("GetChilds", caught);
			Dropbox.get().status.unsetGetChilds();
		}
	};
	
	/**
	 * Gets asyncronous root node
	 */
	final AsyncCallback<GWTDropboxEntry> callbackGetRootDropbox = new AsyncCallback<GWTDropboxEntry>() {
		@Override
		public void onSuccess(GWTDropboxEntry result) {
			GWTDropboxEntry folderItem = result;
			
			actualItem.setUserObject(folderItem);
			evaluesFolderIcon(actualItem);
			actualItem.setState(true);
			actualItem.setSelected(true);
			
			Dropbox.get().status.unsetGetChilds();
			getChilds(result.getPath());
		}
		
		@Override
		public void onFailure(Throwable caught) {
			Main.get().showError("GetRootFolder", caught);
			Dropbox.get().status.unsetGetChilds();
		}
	};
	
	/**
	 * Refresh the folders on a item node
	 * 
	 * @param path The folder path selected to list items
	 */
	private void getChilds(String path) {
		Dropbox.get().status.setGetChilds(Dropbox.get().folderSelectPopup);
		dropboxService.getChildren(path, callbackGetChilds);
	}
	
	/**
	 * Gets the root
	 */
	private void getRoot() {
		Dropbox.get().status.setGetChilds(Dropbox.get().folderSelectPopup);
		dropboxService.getRootDropbox(callbackGetRootDropbox);		
	}
	
	/**
	 * Refresh the tree node
	 */
	public void refresh(boolean reset) {
		String path = ((GWTDropboxEntry) actualItem.getUserObject()).getPath();
		getChilds(path);
	}
	
	/**
	 * Hides all items on a brach
	 * 
	 * @param actualItem The actual item active
	 */
	private void hideAllBranch(TreeItem actualItem) {
		int i = 0;
		int count = actualItem.getChildCount();
		
		for (i = 0; i < count; i++) {
			actualItem.getChild(i).setVisible(false);
		}
	}
	
	/**
	 * Adds folders to actual item if not exists or refreshes it values
	 * 
	 * @param actualItem The actual item active
	 * @param newItem New item to be added, or refreshed
	 */
	private void addFolder(TreeItem actualItem, TreeItem newItem) {
		int i = 0;
		boolean found = false;
		int count = actualItem.getChildCount();
		GWTDropboxEntry folder;
		GWTDropboxEntry newFolder = (GWTDropboxEntry) newItem.getUserObject();
		String folderPath = newFolder.getPath();
		
		for (i = 0; i < count; i++) {
			folder = (GWTDropboxEntry) actualItem.getChild(i).getUserObject();
			// If item is found actualizate values
			if ((folder).getPath().equals(folderPath)) {
				found = true;
				actualItem.getChild(i).setVisible(true);
				actualItem.getChild(i).setUserObject(newFolder);
				evaluesFolderIcon(actualItem.getChild(i));
			}
		}
		
		if (!found) {
			evaluesFolderIcon(newItem);
			actualItem.addItem(newItem);
		}
	}
	
	/**
	 * Gets the actual path of the selected directory tree
	 * 
	 * @return The actual path of selected directory
	 */
	public String getActualPath() {
		return ((GWTDropboxEntry) actualItem.getUserObject()).getPath();
	}	
	
	/**
	 * Evalues actual folder icon to prevent other user interaction with the same folder
	 * this ensures icon and object hasChildsValue are consistent
	 */
	private void evaluesFolderIcon(TreeItem item) {
		GWTDropboxEntry folderItem = (GWTDropboxEntry) item.getUserObject();
		preventFolderInconsitences(item);
		
		String name = (item.equals(rootItem))?"Dropbox":folderItem.getFileName();
		if (folderItem.isChildren()) {
			item.setHTML(Util.imageItemHTML("img/menuitem_childs.gif", name, "top"));
		} else {
			item.setHTML(Util.imageItemHTML("img/menuitem_empty.gif", name, "top"));
		}
	}
	
	/**
	 * Prevents folder inconsistences between server ( multi user deletes folder ) and tree
	 * nodes drawn
	 * 
	 * @param item The tree node
	 */
	private void preventFolderInconsitences(TreeItem item) {
		GWTDropboxEntry folderItem = (GWTDropboxEntry) item.getUserObject();
		
		// Case that must remove all items node
		if (item.getChildCount() > 0 && !folderItem.isChildren()) {
			while (item.getChildCount() > 0) {
				item.getChild(0).remove();
			}
		}
		
		if (item.getChildCount() < 1 && !folderItem.isChildren()) {
			folderItem.setChildren(false);
		}
	}
}