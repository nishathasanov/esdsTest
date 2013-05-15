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

package com.openkm.frontend.client.util;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTUINotification;
import com.openkm.frontend.client.bean.GWTUser;
import com.openkm.frontend.client.bean.GWTWorkspace;
import com.openkm.frontend.client.service.OKMUINotificationService;
import com.openkm.frontend.client.service.OKMUINotificationServiceAsync;
import com.openkm.frontend.client.service.OKMWorkspaceService;
import com.openkm.frontend.client.service.OKMWorkspaceServiceAsync;
import com.openkm.frontend.client.widget.startup.StartUp;

/**
 * Workspace user properties
 * 
 * @author jllort
 * 
 */
public class WorkspaceUserProperties {
	private final OKMWorkspaceServiceAsync workspaceService = (OKMWorkspaceServiceAsync) GWT
			.create(OKMWorkspaceService.class);
	private final OKMUINotificationServiceAsync uINotificationService = (OKMUINotificationServiceAsync) GWT
			.create(OKMUINotificationService.class);
	
	private GWTWorkspace workspace;
	private GWTUser user = new GWTUser();
	private String applicationURL = "";
	
	/**
	 * Workspace user properties
	 */
	public WorkspaceUserProperties() {
	}
	
	/**
	 * First time inits workspace
	 */
	public void init() {
		getUserWorkspace();
	}
	
	/**
	 * Call back to get workspace user data
	 */
	final AsyncCallback<GWTWorkspace> callbackGetUserWorkspace = new AsyncCallback<GWTWorkspace>() {
		public void onSuccess(GWTWorkspace result) {
			workspace = result;
			user = result.getUser();
			applicationURL = result.getApplicationURL();
			
			// Changing the web skin
			Util.changeCss(workspace.getWebSkin());
			
			Main.get().mainPanel.bottomPanel.userInfo.setUser(user.getUsername(), result.isTabAdminVisible());
			
			if (result.isChatEnabled()) {
				Main.get().mainPanel.bottomPanel.userInfo.enableChat();
				
				if (result.isChatAutoLogin()) {
					Main.get().mainPanel.bottomPanel.userInfo.loginChat(true);
				}
			}
			
			if (result.isUserQuotaEnabled() && result.getUserQuotaLimit() > 0) {
				Main.get().mainPanel.bottomPanel.userInfo.enableUserQuota(workspace.getUserQuotaLimit());
			}
			
			Main.get().mainPanel.bottomPanel.userInfo.showExtensions();
			Main.get().aboutPopup.setAppVersion(result.getAppVersion().toString());
			Main.get().aboutPopup.setExtVersion(result.getAppVersion().getExtension());
			getUserDocumentsSize(); // Refreshing user document size ( here is yet set userQuota limit )
			
			// Starting schedulers
			Main.get().startUp.startKeepAlive(workspace.getKeepAliveSchedule());
			Main.get().mainPanel.dashboard.startRefreshingDashboard(workspace.getDashboardSchedule());
			Main.get().mainPanel.topPanel.mainMenu.startRefreshingMenus(workspace.getDashboardSchedule());
			
			// Enabling advanced filters
			if (workspace.isAdvancedFilters()) {
				Main.get().securityPopup.enableAdvancedFilter();
				Main.get().fileUpload.enableAdvancedFilter();
				Main.get().notifyPopup.enableAdvancedFilter();
			}
			
			// Enabling security mode multiple
			if (workspace.isSecurityModeMultiple()) {
				Main.get().securityPopup.enableSecurityModeMultiple();
			}
			
			// Show / hide menus
			Main.get().mainPanel.topPanel.mainMenu.setFileMenuVisible(workspace.isMenuFileVisible());
			Main.get().mainPanel.topPanel.mainMenu.setEditMenuVisible(workspace.isMenuEditVisible());
			Main.get().mainPanel.topPanel.mainMenu.setToolsMenuVisible(workspace.isMenuToolsVisible());
			Main.get().mainPanel.topPanel.mainMenu.setBookmarkMenuVisible(workspace.isMenuBookmarksVisible());
			Main.get().mainPanel.topPanel.mainMenu.setTemplatesMenuVisible(workspace.isMenuTemplatesVisible());
			Main.get().mainPanel.topPanel.mainMenu.setHelpMenuVisible(workspace.isMenuHelpVisible());
			
			// Init available languages
			Main.get().mainPanel.topPanel.mainMenu.initAvailableLanguage(workspace.getLangs());
			// Init available templates
			Main.get().mainPanel.topPanel.mainMenu.refreshAvailableTemplates();
			
			// Enabling / disabling some actions
			Main.get().mainPanel.topPanel.toolBar.setAvailableOption(workspace.getProfileToolbar());
			
			// Add note
			if (workspace.getAvailableOption().isAddNoteOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.notes.showAddNote();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.notes.showAddNote();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.notes.showAddNote();
			}
			// Remove note
			if (workspace.getAvailableOption().isRemoveNoteOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.notes.showRemoveNote();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.notes.showRemoveNote();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.notes.showRemoveNote();
			}
			// Add Category
			if (workspace.getAvailableOption().isAddCategoryOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.document.showAddCategory();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.folder.showAddCategory();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.mail.showAddCategory();
			}
			// Remove category
			if (workspace.getAvailableOption().isRemoveCategoryOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.document.showRemoveCategory();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.folder.showRemoveCategory();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.mail.showRemoveCategory();
			}
			// Add keyword
			if (workspace.getAvailableOption().isAddKeywordOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.document.showAddKeyword();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.folder.showAddKeyword();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.mail.showAddKeyword();
			}
			// remove keyword
			if (workspace.getAvailableOption().isRemoveKeywordOption()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.document.showRemoveKeyword();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.folder.showRemoveKeyword();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.mail.showRemoveKeyword();
			}
			
			// Showing tabs
			boolean refreshTab = false;
			if (workspace.isTabDesktopVisible()) {
				Main.get().mainPanel.topPanel.tabWorkspace.showDesktop();
				refreshTab = true;
			}
			if (workspace.isTabSearchVisible()) {
				Main.get().mainPanel.topPanel.tabWorkspace.showSearh();
				refreshTab = true;
			}
			if (workspace.isTabDashboardVisible()) {
				Main.get().mainPanel.topPanel.tabWorkspace.showDashboard();
				refreshTab = true;
			}
			if (result.isTabAdminVisible()) {
				Main.get().mainPanel.topPanel.mainMenu.administration.setVisible(true);
				Main.get().mainPanel.topPanel.tabWorkspace.showAdministration();
				refreshTab = true;
			}
			refreshTab = Main.get().mainPanel.topPanel.tabWorkspace.showExtensionTabs() || refreshTab;
			if (refreshTab) {
				Main.get().mainPanel.topPanel.tabWorkspace.init();
			}
			
			// showing stack
			boolean refreshStack = false;
			if (workspace.isStackTaxonomy()) {
				Main.get().mainPanel.desktop.navigator.showTaxonomy();
				refreshStack = true;
			}
			if (workspace.isStackCategoriesVisible()) {
				Main.get().mainPanel.desktop.navigator.showCategories();
				refreshStack = true;
			}
			if (workspace.isStackThesaurusVisible()) {
				Main.get().mainPanel.desktop.navigator.showThesaurus();
			}
			if (workspace.isStackTemplatesVisible()) {
				Main.get().mainPanel.desktop.navigator.showTemplates();
				Main.get().mainPanel.desktop.navigator.taxonomyTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.categoriesTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.thesaurusTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.personalTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.templateTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.mailTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.desktop.navigator.trashTree.folderSelectPopup.showTemplates();
				Main.get().mainPanel.dashboard.keyMapDashboard.showTemplates();
				Main.get().mainPanel.search.searchBrowser.searchIn.showTemplates();
				refreshStack = true;
			}
			if (workspace.isStackPersonalVisible()) {
				Main.get().mainPanel.desktop.navigator.showPersonal();
				Main.get().mainPanel.desktop.navigator.taxonomyTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.categoriesTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.thesaurusTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.personalTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.templateTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.mailTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.desktop.navigator.trashTree.folderSelectPopup.showPersonal();
				Main.get().mainPanel.dashboard.keyMapDashboard.showPersonal();
				Main.get().mainPanel.search.searchBrowser.searchIn.showPersonal();
				refreshStack = true;
			}
			if (workspace.isStackMailVisible()) {
				Main.get().mainPanel.desktop.navigator.showMail();
				Main.get().mainPanel.desktop.navigator.taxonomyTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.categoriesTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.thesaurusTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.personalTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.templateTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.mailTree.folderSelectPopup.showMail();
				Main.get().mainPanel.desktop.navigator.trashTree.folderSelectPopup.showMail();
				Main.get().mainPanel.dashboard.keyMapDashboard.showMail();
				Main.get().mainPanel.search.searchBrowser.searchIn.showMail();
				refreshStack = true;
			}
			if (workspace.isStackTrashVisible()) {
				Main.get().mainPanel.desktop.navigator.showTrash();
				Main.get().mainPanel.dashboard.keyMapDashboard.showTrash();
				Main.get().mainPanel.search.searchBrowser.searchIn.showTrash();
				refreshStack = true;
			}
			if (refreshStack) {
				Main.get().mainPanel.desktop.navigator.refreshView();
			}
			
			// Documents tabs
			if (workspace.isTabDocumentPropertiesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showDocument();
			}
			if (workspace.isTabDocumentNotesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showNotes();
			}
			if (workspace.isTabDocumentSecurityVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showSecurity();
			}
			if (workspace.isTabDocumentVersionVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showVersion();
			}
			if (workspace.isTabDocumentPreviewVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showPreview();
			}
			if (workspace.isTabDocumentPropertyGroupsVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showPropertyGroups();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.showPropertyGroups();
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showPropertyGroups();
			}
			Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument
					.setKeywordEnabled(workspace.isKeywordEnabled());
			Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.setKeywordEnabled(workspace.isKeywordEnabled());
			Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.setKeywordEnabled(workspace.isKeywordEnabled());
			Main.get().mainPanel.desktop.browser.tabMultiple.tabDocument.showExtensions();
			
			// Folder tabs
			if (workspace.isTabFolderPropertiesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.showProperties();
			}
			if (workspace.isTabFolderNotesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.showNotes();
			}
			if (workspace.isTabFolderSecurityVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.showSecurity();
			}
			Main.get().mainPanel.desktop.browser.tabMultiple.tabFolder.showExtensions();
			
			// Mail tabs
			if (workspace.isTabMailPropertiesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showProperties();
			}
			if (workspace.isTabMailNotesVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showNotes();
			}
			if (workspace.isTabMailPreviewVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showPreview();
			}
			if (workspace.isTabMailSecurityVisible()) {
				Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showSecurity();
			}
			Main.get().mainPanel.desktop.browser.tabMultiple.tabMail.showExtensions();
			
			// Show / hide dashboard tools
			if (workspace.isDashboardUserVisible()) {
				Main.get().mainPanel.dashboard.showUser();
				Main.get().mainPanel.bottomPanel.userInfo.showDashboardUserIcons();
			}
			if (workspace.isDashboardMailVisible()) {
				Main.get().mainPanel.dashboard.showMail();
			}
			if (workspace.isDashboardNewsVisible()) {
				Main.get().mainPanel.dashboard.showNews();
				Main.get().mainPanel.bottomPanel.userInfo.showDashboardNewsIcons();
			}
			if (workspace.isDashboardGeneralVisible()) {
				Main.get().mainPanel.dashboard.showGeneral();
			}
			if (workspace.isDashboardWorkflowVisible()) {
				Main.get().mainPanel.dashboard.showWorkflow();
				Main.get().mainPanel.bottomPanel.userInfo.showDashboardWorkflowIcons();
			}
			if (workspace.isDashboardKeywordsVisible()) {
				Main.get().mainPanel.dashboard.keyMapDashboard.setDashboardKeywordsVisible(true);
				Main.get().mainPanel.dashboard.showKeywords();
			}
			// Refreshing dashboard values
			Main.get().fileUpload.setUploadNotifyUsers(workspace.isUploadNotifyUsers());
			Main.get().mainPanel.dashboard.init();
			Main.get().mainPanel.dashboard.refreshAll(); // Refreshing dashboard values
			
			// Minimun search characters
			Main.get().mainPanel.search.searchBrowser.searchIn.searchControl.setMinSearchCharacters(workspace.getMinSearchCharacters());
			
			// Filebrowser profile
			Main.get().mainPanel.desktop.browser.fileBrowser.setProfileFileBrowser(workspace.getProfileFileBrowser(),
																				   workspace.getProfileExplorer());
			
			Main.get().startUp.nextStatus(StartUp.STARTUP_GET_TAXONOMY_ROOT);
			
			// Getting ui messages
			getUINotificationMessages(new Long(
					Main.get().workspaceUserProperties.workspace.getUINotificationSchedule() / 2).intValue());
		}
		
		public void onFailure(Throwable caught) {
			Main.get().showError("getUserWorkspace", caught);
		}
	};
	
	/**
	 * Gets the users documents size
	 */
	final AsyncCallback<Double> callbackGetUserDocumentsSize = new AsyncCallback<Double>() {
		public void onSuccess(Double result) {
			Main.get().mainPanel.bottomPanel.userInfo.setUserRepositorySize(result.longValue());
		}
		
		public void onFailure(Throwable caught) {
			Main.get().showError("getUserDocumentsSize", caught);
		}
	};
	
	/**
	 * getUINotificationMessages
	 */
	private void getUINotificationMessages(final int scheduleTime) {
		uINotificationService.get(new AsyncCallback<List<GWTUINotification>>() {
			@Override
			public void onSuccess(List<GWTUINotification> result) {
				Main.get().mainPanel.bottomPanel.userInfo.reset();
				boolean schedule = true;
				
				if (!result.isEmpty()) {
					for (GWTUINotification uin : result) {
						Main.get().mainPanel.bottomPanel.userInfo.addUINotification(uin);
						
						if (uin.getAction() == GWTUINotification.ACTION_LOGOUT) {
							schedule = false;
						}
					}
					Main.get().mainPanel.bottomPanel.userInfo.setLastUIId(result.get(result.size()-1).getId());
				}
				
				if (schedule) {
					Timer timer = new Timer() {
						@Override
						public void run() {
							getUINotificationMessages(scheduleTime);
						}
					};
					
					timer.schedule(scheduleTime);
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Main.get().showError("getUpdateMessage", caught);
			}
		});
		
	}
	
	/**
	 * Gets the workspace user data
	 */
	public void getUserWorkspace() {
		workspaceService.getUserWorkspace(callbackGetUserWorkspace);
	}
	
	/**
	 * refreshUserWorkspace
	 */
	public void refreshUserWorkspace() {
		workspaceService.getUserWorkspace(new AsyncCallback<GWTWorkspace>() {
			@Override
			public void onSuccess(GWTWorkspace result) {
				workspace = result;
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Main.get().showError("getUserWorkspace", caught);
			}
		});
	}
	
	/**
	 * Gets the user documents size
	 */
	public void getUserDocumentsSize() {
		workspaceService.getUserDocumentsSize(callbackGetUserDocumentsSize);
	}
	
	/**
	 * Gets the user
	 * 
	 * @return The user
	 */
	public GWTUser getUser() {
		return user;
	}
	
	/**
	 * Gets the application URL
	 * 
	 * @return
	 */
	public String getApplicationURL() {
		return applicationURL;
	}
	
	/**
	 * Gets the workspace data
	 * 
	 * @return The workspace data
	 */
	public GWTWorkspace getWorkspace() {
		return workspace;
	}
	
	/**
	 * setAvailableAction
	 * 
	 * Some actions must be enabled at ends because some objects are not created since end startp up
	 */
	public void setAvailableAction() {
		if (Main.get().workspaceUserProperties.getWorkspace().isStackTaxonomy()) {
			Main.get().mainPanel.desktop.navigator.taxonomyTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.taxonomyMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackCategoriesVisible()) {
			Main.get().mainPanel.desktop.navigator.categoriesTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.categoriesMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackThesaurusVisible()) {
			Main.get().mainPanel.desktop.navigator.thesaurusTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.thesaurusMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackTemplatesVisible()) {
			Main.get().mainPanel.desktop.navigator.templateTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.templatesMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackPersonalVisible()) {
			Main.get().mainPanel.desktop.navigator.personalTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.personalMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackMailVisible()) {
			Main.get().mainPanel.desktop.navigator.mailTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.mailMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		if (Main.get().workspaceUserProperties.getWorkspace().isStackTrashVisible()) {
			Main.get().mainPanel.desktop.navigator.trashTree.menuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
			Main.get().mainPanel.desktop.browser.fileBrowser.trashMenuPopup.menu.setAvailableOption(workspace
					.getAvailableOption());
		}
		Main.get().mainPanel.search.searchBrowser.searchResult.searchCompactResult.menuPopup.menu
				.setAvailableOption(workspace.getAvailableOption());
		Main.get().mainPanel.topPanel.mainMenu.setAvailableOption(workspace);
	}
}
