/**
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

package com.openkm.servlet.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.api.OKMAuth;
import com.openkm.api.OKMDashboard;
import com.openkm.api.OKMPropertyGroup;
import com.openkm.bean.PropertyGroup;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.AuthDAO;
import com.openkm.dao.LanguageDAO;
import com.openkm.dao.MailAccountDAO;
import com.openkm.dao.ReportDAO;
import com.openkm.dao.UserConfigDAO;
import com.openkm.dao.bean.Language;
import com.openkm.dao.bean.MailAccount;
import com.openkm.dao.bean.Profile;
import com.openkm.dao.bean.Report;
import com.openkm.dao.bean.User;
import com.openkm.dao.bean.UserConfig;
import com.openkm.frontend.client.OKMException;
import com.openkm.frontend.client.bean.GWTAvailableOption;
import com.openkm.frontend.client.bean.GWTLanguage;
import com.openkm.frontend.client.bean.GWTProfileFileBrowser;
import com.openkm.frontend.client.bean.GWTProfileExplorer;
import com.openkm.frontend.client.bean.GWTProfileToolbar;
import com.openkm.frontend.client.bean.GWTPropertyGroup;
import com.openkm.frontend.client.bean.GWTUser;
import com.openkm.frontend.client.bean.GWTWorkspace;
import com.openkm.frontend.client.constants.service.ErrorCode;
import com.openkm.frontend.client.service.OKMWorkspaceService;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.principal.DatabasePrincipalAdapter;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.util.GWTUtil;
import com.openkm.util.ReportUtils;
import com.openkm.util.WarUtils;
import com.openkm.validator.ValidatorException;
import com.openkm.validator.ValidatorFactory;
import com.openkm.validator.password.PasswordValidator;

/**
 * WorkspaceServlet
 * 
 * @author jllort
 */
public class WorkspaceServlet extends OKMRemoteServiceServlet implements OKMWorkspaceService {
	private static Logger log = LoggerFactory.getLogger(WorkspaceServlet.class);
	private static final long serialVersionUID = 8673521252684830906L;
	
	@Override
	public GWTWorkspace getUserWorkspace() throws OKMException {
		log.debug("getUserWorkspace()");
		updateSessionManager();
		GWTWorkspace workspace = new GWTWorkspace();
		workspace.setApplicationURL(Config.APPLICATION_URL);
		workspace.setAppVersion(GWTUtil.copy(WarUtils.getAppVersion()));
		workspace.setWorkflowRunConfigForm(Config.WORKFLOW_RUN_CONFIG_FORM);
		workspace.setWorkflowProcessIntanceVariableUUID(Config.WORKFLOW_PROCESS_INSTANCE_VARIABLE_UUID);
		workspace.setWorkflowProcessIntanceVariablePath(Config.WORKFLOW_PROCESS_INSTANCE_VARIABLE_PATH);
		workspace.setSessionId(getThreadLocalRequest().getSession().getId());
		workspace.setMinSearchCharacters(Config.MIN_SEARCH_CHARACTERS);
		
		// Security mode
		workspace.setSecurityModeMultiple(Config.SECURITY_MODE_MULTIPLE);
		
		// Schedule time
		workspace.setKeepAliveSchedule(TimeUnit.MINUTES.toMillis(Config.SCHEDULE_SESSION_KEEPALIVE));
		workspace.setDashboardSchedule(TimeUnit.MINUTES.toMillis(Config.SCHEDULE_DASHBOARD_REFRESH));
		workspace.setUINotificationSchedule(TimeUnit.MINUTES.toMillis(Config.SCHEDULE_UI_NOTIFICATION));
		
		List<GWTPropertyGroup> wizardPropGrpLst = new ArrayList<GWTPropertyGroup>();
		List<String> wizardWorkflowLst = new ArrayList<String>();
		List<String> miscWorkflowLst = new ArrayList<String>();
		Profile up = new Profile();
		Session session = null;
		
		try {
			// User data
			GWTUser gwtUser = new GWTUser();
			gwtUser.setId(getThreadLocalRequest().getRemoteUser());
			gwtUser.setUsername(OKMAuth.getInstance().getName(null, gwtUser.getId()));
			workspace.setUser(gwtUser);
			
			UserConfig uc = UserConfigDAO.findByPk(getThreadLocalRequest().getRemoteUser());
			up = uc.getProfile();
			
			for (String pgroup : up.getPrfWizard().getPropertyGroups()) {
				for (PropertyGroup pg : OKMPropertyGroup.getInstance().getAllGroups(null)) {
					if (pg.getName().equals(pgroup) && pg.isVisible()) {
						wizardPropGrpLst.add(GWTUtil.copy(pg));
						break;
					}
				}
			}
			
			for (String workflow : up.getPrfWizard().getWorkflows()) {
				wizardWorkflowLst.add(workflow);
			}
			
			for (String workflow : up.getPrfMisc().getWorkflows()) {
				miscWorkflowLst.add(workflow);
			}
			
			// Previewer
			workspace.setPreviewer(Config.SYSTEM_PREVIEWER);
			
			// Advanced filters ( used when there a lot of users and groups )
			workspace.setAdvancedFilters(up.getPrfMisc().isAdvancedFilters());
			
			// Is a wizard to uploading documents
			workspace.setWizardPropertyGroups(!up.getPrfWizard().getPropertyGroups().isEmpty());
			workspace.setWizardPropertyGroupList(wizardPropGrpLst);
			workspace.setWizardWorkflows(!up.getPrfWizard().getWorkflows().isEmpty());
			workspace.setWizardWorkflowList(wizardWorkflowLst);
			workspace.setWizardCategories(up.getPrfWizard().isCategoriesEnabled());
			workspace.setWizardKeywords(up.getPrfWizard().isKeywordsEnabled());
			
			// Is a misc workflow list available
			workspace.setMiscWorkflowList(miscWorkflowLst);
			
			// Is chat enabled and autologin
			workspace.setChatEnabled(up.getPrfChat().isChatEnabled());
			workspace.setChatAutoLogin(up.getPrfChat().isAutoLoginEnabled());
			
			// Is admin
			workspace.setAdminRole(getThreadLocalRequest().isUserInRole(Config.DEFAULT_ADMIN_ROLE));
			
			// Setting web skin
			workspace.setWebSkin(up.getPrfMisc().getWebSkin());
			
			// Only thesaurus keywords are allowed
			workspace.setKeywordEnabled(up.getPrfMisc().isKeywordsEnabled());
			
			// User quota ( limit user repository size )
			workspace.setUserQuotaEnabled(up.getPrfMisc().getUserQuota() > 0);
			workspace.setUserQuotaLimit(up.getPrfMisc().getUserQuota() * 1024 * 1024);
			workspace.setUploadNotifyUsers(up.getPrfMisc().isUploadNotifyUsers());
			workspace.setWebdavFix(Config.SYSTEM_WEBDAV_FIX);
			
			// Stack visibility
			workspace.setStackTaxonomy(up.getPrfStack().isTaxonomyVisible());
			workspace.setStackCategoriesVisible(up.getPrfStack().isCategoriesVisible());
			workspace.setStackThesaurusVisible(up.getPrfStack().isThesaurusVisible());
			workspace.setStackTemplatesVisible(up.getPrfStack().isTemplatesVisible());
			workspace.setStackPersonalVisible(up.getPrfStack().isPersonalVisible());
			workspace.setStackMailVisible(up.getPrfStack().isMailVisible());
			workspace.setStackTrashVisible(up.getPrfStack().isTrashVisible());
			
			// Menus visibility
			workspace.setMenuFileVisible(up.getPrfMenu().isFileVisible());
			workspace.setMenuEditVisible(up.getPrfMenu().isEditVisible());
			workspace.setMenuBookmarksVisible(up.getPrfMenu().isBookmarksVisible());
			workspace.setMenuToolsVisible(up.getPrfMenu().isToolsVisible());
			workspace.setMenuTemplatesVisible(up.getPrfMenu().isTemplatesVisible());
			workspace.setMenuHelpVisible(up.getPrfMenu().isHelpVisible());
			
			// Tab visibility
			workspace.setTabDesktopVisible(up.getPrfTab().isDesktopVisible());
			workspace.setTabSearchVisible(up.getPrfTab().isSearchVisible());
			workspace.setTabDashboardVisible(up.getPrfTab().isDashboardVisible());
			workspace.setTabAdminVisible(getThreadLocalRequest().isUserInRole(Config.DEFAULT_ADMIN_ROLE)
					&& up.getPrfTab().isAdministrationVisible());
			
			// If there's no stack visible force Desktop to do not be visible
			if (!up.getPrfStack().isTaxonomyVisible() && !up.getPrfStack().isCategoriesVisible()
					&& !up.getPrfStack().isThesaurusVisible() && !up.getPrfStack().isTemplatesVisible()
					&& !up.getPrfStack().isPersonalVisible() && !up.getPrfStack().isMailVisible()
					&& !up.getPrfStack().isTrashVisible()) {
				workspace.setTabDesktopVisible(false);
			}
			
			// Tab document visibility
			workspace.setTabDocumentPropertiesVisible(up.getPrfTab().getPrfDocument().isPropertiesVisible());
			workspace.setTabDocumentSecurityVisible(up.getPrfTab().getPrfDocument().isSecurityVisible());
			workspace.setTabDocumentNotesVisible(up.getPrfTab().getPrfDocument().isNotesVisible());
			workspace.setTabDocumentVersionVisible(up.getPrfTab().getPrfDocument().isVersionsVisible());
			workspace.setTabDocumentPreviewVisible(up.getPrfTab().getPrfDocument().isPreviewVisible());
			workspace.setTabDocumentPropertyGroupsVisible(up.getPrfTab().getPrfDocument().isPropertyGroupsVisible());
			workspace.setTabDocumentVersionDownloadVisible(up.getPrfTab().getPrfDocument().isVersionDownloadVisible());
			
			// Tab folder visibility
			workspace.setTabFolderPropertiesVisible(up.getPrfTab().getPrfFolder().isPropertiesVisible());
			workspace.setTabFolderSecurityVisible(up.getPrfTab().getPrfFolder().isSecurityVisible());
			workspace.setTabFolderNotesVisible(up.getPrfTab().getPrfFolder().isNotesVisible());
			
			// Tab mail visibility
			workspace.setTabMailPropertiesVisible(up.getPrfTab().getPrfMail().isPropertiesVisible());
			workspace.setTabMailSecurityVisible(up.getPrfTab().getPrfMail().isSecurityVisible());
			workspace.setTabMailPreviewVisible(up.getPrfTab().getPrfMail().isPreviewVisible());
			workspace.setTabMailNotesVisible(up.getPrfTab().getPrfMail().isNotesVisible());
			
			// Dashboard visibility
			workspace.setDashboardUserVisible(up.getPrfDashboard().isUserVisible());
			workspace.setDashboardMailVisible(up.getPrfDashboard().isMailVisible());
			workspace.setDashboardNewsVisible(up.getPrfDashboard().isNewsVisible());
			workspace.setDashboardGeneralVisible(up.getPrfDashboard().isGeneralVisible());
			workspace.setDashboardWorkflowVisible(up.getPrfDashboard().isWorkflowVisible());
			workspace.setDashboardKeywordsVisible(up.getPrfDashboard().isKeywordsVisible());
			
			// Available options
			GWTAvailableOption availableOption = new GWTAvailableOption();
			
			// Menu File
			availableOption.setCreateFolderOption(up.getPrfMenu().getPrfFile().isCreateFolderVisible());
			availableOption.setFindFolderOption(up.getPrfMenu().getPrfFile().isFindFolderVisible());
			availableOption.setFindDocumentOption(up.getPrfMenu().getPrfFile().isFindDocumentVisible());
			availableOption.setGotoFolderOption(up.getPrfMenu().getPrfFile().isGoFolderVisible());
			availableOption.setDownloadOption(up.getPrfMenu().getPrfFile().isDownloadVisible());
			availableOption.setDownloadPdfOption(up.getPrfMenu().getPrfFile().isDownloadPdfVisible());
			availableOption.setAddDocumentOption(up.getPrfMenu().getPrfFile().isAddDocumentVisible());
			availableOption.setWorkflowOption(up.getPrfMenu().getPrfFile().isStartWorkflowVisible());
			availableOption.setRefreshOption(up.getPrfMenu().getPrfFile().isRefreshVisible());
			availableOption.setScannerOption(up.getPrfMenu().getPrfFile().isScannerVisible());
			availableOption.setUploaderOption(up.getPrfMenu().getPrfFile().isUploaderVisible());
			availableOption.setExportOption(up.getPrfMenu().getPrfFile().isExportVisible());
			availableOption.setCreateFromTemplateOption(up.getPrfMenu().getPrfFile().isCreateFromTemplateVisible());
			availableOption.setPurgeOption(up.getPrfMenu().getPrfFile().isPurgeVisible());
			availableOption.setRestoreOption(up.getPrfMenu().getPrfFile().isRestoreVisible());
			availableOption.setPurgeTrashOption(up.getPrfMenu().getPrfFile().isPurgeTrashVisible());
			availableOption.setSendDocumentLinkOption(up.getPrfMenu().getPrfFile().isSendDocumentLinkVisible());
			availableOption.setSendDocumentAttachmentOption(up.getPrfMenu().getPrfFile()
					.isSendDocumentAttachmentVisible());
			
			// Menu Edit
			availableOption.setLockOption(up.getPrfMenu().getPrfEdit().isLockVisible());
			availableOption.setUnLockOption(up.getPrfMenu().getPrfEdit().isUnlockVisible());
			availableOption.setRenameOption(up.getPrfMenu().getPrfEdit().isRenameVisible());
			availableOption.setCopyOption(up.getPrfMenu().getPrfEdit().isCopyVisible());
			availableOption.setMoveOption(up.getPrfMenu().getPrfEdit().isMoveVisible());
			availableOption.setCheckinOption(up.getPrfMenu().getPrfEdit().isCheckInVisible());
			availableOption.setCheckoutOption(up.getPrfMenu().getPrfEdit().isCheckOutVisible());
			availableOption.setCancelCheckoutOption(up.getPrfMenu().getPrfEdit().isCancelCheckOutVisible());
			availableOption.setDeleteOption(up.getPrfMenu().getPrfEdit().isDeleteVisible());
			availableOption.setAddPropertyGroupOption(up.getPrfMenu().getPrfEdit().isAddPropertyGroupVisible());
			availableOption.setRemovePropertyGroupOption(up.getPrfMenu().getPrfEdit().isRemovePropertyGroupVisible());
			availableOption.setAddSubscriptionOption(up.getPrfMenu().getPrfEdit().isAddSubscriptionVisible());
			availableOption.setRemoveSubscriptionOption(up.getPrfMenu().getPrfEdit().isRemoveSubscriptionVisible());
			availableOption.setAddNoteOption(up.getPrfMenu().getPrfEdit().isAddNoteVisible());
			availableOption.setAddCategoryOption(up.getPrfMenu().getPrfEdit().isAddCategoryVisible());
			availableOption.setAddKeywordOption(up.getPrfMenu().getPrfEdit().isAddKeywordVisible());
			availableOption.setRemoveNoteOption(up.getPrfMenu().getPrfEdit().isRemoveNoteVisible());
			availableOption.setRemoveCategoryOption(up.getPrfMenu().getPrfEdit().isRemoveCategoryVisible());
			availableOption.setRemoveKeywordOption(up.getPrfMenu().getPrfEdit().isRemoveKeywordVisible());
			availableOption.setMergePdfOption(up.getPrfMenu().getPrfEdit().isMergePdfVisible());
			
			// Menu Bookmark
			availableOption.setManageBookmarkOption(up.getPrfMenu().getPrfBookmark().isManageBookmarksVisible());
			availableOption.setAddBookmarkOption(up.getPrfMenu().getPrfBookmark().isAddBookmarkVisible());
			availableOption.setHomeOption(up.getPrfMenu().getPrfBookmark().isGoHomeVisible());
			availableOption.setSetHomeOption(up.getPrfMenu().getPrfBookmark().isSetHomeVisible());
			
			// Menu Tool
			availableOption.setLanguagesOption(up.getPrfMenu().getPrfTool().isLanguagesVisible());
			availableOption.setSkinOption(up.getPrfMenu().getPrfTool().isSkinVisible());
			availableOption.setDebugOption(up.getPrfMenu().getPrfTool().isDebugVisible());
			availableOption.setAdministrationOption(up.getPrfMenu().getPrfTool().isAdministrationVisible()
					&& getThreadLocalRequest().isUserInRole(Config.DEFAULT_ADMIN_ROLE));
			availableOption.setPreferencesOption(up.getPrfMenu().getPrfTool().isPreferencesVisible());
			
			// Menu Help
			availableOption.setHelpOption(up.getPrfMenu().getPrfHelp().isHelpVisible());
			availableOption.setDocumentationOption(up.getPrfMenu().getPrfHelp().isDocumentationVisible());
			availableOption.setBugReportOption(up.getPrfMenu().getPrfHelp().isBugTrackingVisible());
			availableOption.setSupportRequestOption(up.getPrfMenu().getPrfHelp().isSupportVisible());
			availableOption.setPublicForumOption(up.getPrfMenu().getPrfHelp().isForumVisible());
			availableOption.setVersionChangesOption(up.getPrfMenu().getPrfHelp().isChangelogVisible());
			availableOption.setProjectWebOption(up.getPrfMenu().getPrfHelp().isWebSiteVisible());
			availableOption.setAboutOption(up.getPrfMenu().getPrfHelp().isAboutVisible());
			
			availableOption.setMediaPlayerOption(true);
			availableOption.setImageViewerOption(true);
			
			workspace.setAvailableOption(availableOption);
			
			// Reports
			for (Long rpId : up.getPrfMisc().getReports()) {
				Report report = ReportDAO.findByPk(rpId);
				
				if (report != null && report.isActive()) {
					workspace.getReports().add(GWTUtil.copy(report, ReportUtils.getReportParameters(rpId)));
				}
			}
			
			// Toolbar
			// Is visible on toolbar && available option too
			GWTProfileToolbar profileToolbar = new GWTProfileToolbar();
			profileToolbar.setAddDocumentVisible(up.getPrfToolbar().isAddDocumentVisible()
					&& availableOption.isAddDocumentOption());
			profileToolbar.setAddPropertyGroupVisible(up.getPrfToolbar().isAddPropertyGroupVisible()
					&& availableOption.isAddPropertyGroupOption());
			profileToolbar.setAddSubscriptionVisible(up.getPrfToolbar().isAddSubscriptionVisible()
					&& availableOption.isAddSubscriptionOption());
			profileToolbar.setCancelCheckoutVisible(up.getPrfToolbar().isCancelCheckoutVisible()
					&& availableOption.isCancelCheckoutOption());
			profileToolbar.setCheckoutVisible(up.getPrfToolbar().isCheckoutVisible()
					&& availableOption.isCheckoutOption());
			profileToolbar
					.setCheckinVisible(up.getPrfToolbar().isCheckinVisible() && availableOption.isCheckinOption());
			profileToolbar.setCreateFolderVisible(up.getPrfToolbar().isCreateFolderVisible()
					&& availableOption.isCreateFolderOption());
			profileToolbar.setDeleteVisible(up.getPrfToolbar().isDeleteVisible() && availableOption.isDeleteOption());
			profileToolbar.setDownloadPdfVisible(up.getPrfToolbar().isDeleteVisible()
					&& availableOption.isDeleteOption());
			profileToolbar.setDownloadVisible(up.getPrfToolbar().isDownloadVisible()
					&& availableOption.isDownloadOption());
			profileToolbar.setFindDocumentVisible(up.getPrfToolbar().isFindDocumentVisible()
					&& availableOption.isFindDocumentOption());
			profileToolbar.setFindFolderVisible(up.getPrfToolbar().isFindFolderVisible()
					&& availableOption.isFindFolderOption());
			profileToolbar.setHomeVisible(up.getPrfToolbar().isHomeVisible() && availableOption.isHomeOption());
			profileToolbar.setLockVisible(up.getPrfToolbar().isLockVisible() && availableOption.isLockOption());
			profileToolbar
					.setRefreshVisible(up.getPrfToolbar().isRefreshVisible() && availableOption.isRefreshOption());
			profileToolbar.setRemovePropertyGroupVisible(up.getPrfToolbar().isRemovePropertyGroupVisible()
					&& availableOption.isRemovePropertyGroupOption());
			profileToolbar.setRemoveSubscriptionVisible(up.getPrfToolbar().isRemoveSubscriptionVisible()
					&& availableOption.isRemoveSubscriptionOption());
			profileToolbar
					.setScannerVisible(up.getPrfToolbar().isScannerVisible() && availableOption.isScannerOption());
			profileToolbar.setStartWorkflowVisible(up.getPrfToolbar().isStartWorkflowVisible()
					&& availableOption.isWorkflowOption());
			profileToolbar.setUnlockVisible(up.getPrfToolbar().isUnlockVisible() && availableOption.isUnLockOption());
			profileToolbar.setUploaderVisible(up.getPrfToolbar().isUploaderVisible()
					&& availableOption.isUploaderOption());
			profileToolbar.setOmrVisible(up.getPrfToolbar().isOmrVisible());
			workspace.setProfileToolbar(profileToolbar);
			
			// file broser
			GWTProfileFileBrowser profileFileBrowser = new GWTProfileFileBrowser();
			profileFileBrowser.setStatusVisible(up.getPrfFileBrowser().isStatusVisible());
			profileFileBrowser.setMassiveVisible(up.getPrfFileBrowser().isMassiveVisible());
			profileFileBrowser.setIconVisible(up.getPrfFileBrowser().isIconVisible());
			profileFileBrowser.setNameVisible(up.getPrfFileBrowser().isNameVisible());
			profileFileBrowser.setSizeVisible(up.getPrfFileBrowser().isSizeVisible());
			profileFileBrowser.setLastModifiedVisible(up.getPrfFileBrowser().isLastModifiedVisible());
			profileFileBrowser.setAuthorVisible(up.getPrfFileBrowser().isAuthorVisible());
			profileFileBrowser.setVersionVisible(up.getPrfFileBrowser().isVersionVisible());
			workspace.setProfileFileBrowser(profileFileBrowser);
			
			// pagination
			GWTProfileExplorer profileExplorer = new GWTProfileExplorer();
			profileExplorer.setTypeFilterEnabled(up.getPrfExplorer().isTypeFilterEnabled());
			workspace.setProfileExplorer(profileExplorer);
			
			// Setting available UI languages
			List<GWTLanguage> langs = new ArrayList<GWTLanguage>();
			
			for (Language lang : LanguageDAO.findAll()) {
				langs.add(GWTUtil.copy(lang));
			}
			
			workspace.setLangs(langs);
			User user = new User();
			
			if (Config.PRINCIPAL_ADAPTER.equals(DatabasePrincipalAdapter.class.getCanonicalName())) {
				user = AuthDAO.findUserByPk(getThreadLocalRequest().getRemoteUser());
				
				if (user != null) {
					workspace.setEmail(user.getEmail());
				}
			} else {
				user.setId(getThreadLocalRequest().getRemoteUser());
				user.setName("");
				user.setEmail("");
				user.setActive(true);
				user.setPassword("");
			}
			
			for (Iterator<MailAccount> it = MailAccountDAO.findByUser(getThreadLocalRequest().getRemoteUser(), true)
					.iterator(); it.hasNext();) {
				MailAccount mailAccount = it.next();
				workspace.setImapHost(mailAccount.getMailHost());
				workspace.setImapUser(mailAccount.getMailUser());
				workspace.setImapFolder(mailAccount.getMailFolder());
				workspace.setImapID(mailAccount.getId());
			}
			
			if (user != null) {
				workspace.setRoleList(OKMAuth.getInstance().getRolesByUser(null, user.getId()));
			} else {
				log.warn("User is null! Please, check principal.adapter={}", Config.PRINCIPAL_ADAPTER);
			}
			
			if (Config.PRINCIPAL_ADAPTER.equals(DatabasePrincipalAdapter.class.getCanonicalName())) {
				workspace.setChangePassword(true);
			} else {
				workspace.setChangePassword(false);
			}
			
			// Saving workspace to session ( will be used to get extracolumn
			// data )
			saveUserWorkspaceSession(workspace);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Database),
					e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_IO),
					e.getMessage());
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Parse),
					e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Repository),
					e.getMessage());
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(
					ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_PrincipalAdapter),
					e.getMessage());
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_PathNotFound),
					e.getMessage());
		} catch (NoSuchGroupException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_NoSuchGroup),
					e.getMessage());
		} finally {
			JCRUtils.logout(session);
		}
		
		return workspace;
	}
	
	@Override
	public Double getUserDocumentsSize() throws OKMException {
		log.debug("getUserDocumentsSize()");
		Double docSize = new Double(0);
		updateSessionManager();
		
		try {
			docSize = new Double(OKMDashboard.getInstance().getUserDocumentsSize(null));
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Repository),
					e.getMessage());
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Database),
					e.getMessage());
		}
		
		return docSize;
	}
	
	@Override
	public void updateUserWorkspace(GWTWorkspace workspace) throws OKMException {
		log.debug("updateUserWorkspace()");
		updateSessionManager();
		
		// For updating user
		User user = new User();
		user.setId(workspace.getUser().getId());
		user.setPassword(workspace.getPassword());
		user.setEmail(workspace.getEmail());
		
		// For updating imap mail
		MailAccount mailAccount = new MailAccount();
		mailAccount.setActive(true);
		mailAccount.setMailFolder(workspace.getImapFolder());
		mailAccount.setMailHost(workspace.getImapHost());
		mailAccount.setMailPassword(workspace.getImapPassword());
		mailAccount.setMailUser(workspace.getImapUser());
		mailAccount.setUser(workspace.getUser().getId());
		mailAccount.setId(workspace.getImapID());
		
		try {
			// Can change password
			if (Config.PRINCIPAL_ADAPTER.equals(DatabasePrincipalAdapter.class.getCanonicalName())) {
				AuthDAO.updateUserPassword(workspace.getUser().getId(), workspace.getPassword());
				
				if (!user.getEmail().equals("")) {
					AuthDAO.updateUserEmail(workspace.getUser().getId(), workspace.getEmail());
				}
			}
			
			if (MailAccountDAO.findByUser(workspace.getUser().getId(), false).size() > 0) {
				MailAccountDAO.update(mailAccount);
				
				if (!mailAccount.getMailPassword().equals("")) {
					MailAccountDAO.updatePassword(mailAccount.getId(), mailAccount.getMailPassword());
				}
			} else if (mailAccount.getMailHost().length() > 0 && mailAccount.getMailFolder().length() > 0
					&& mailAccount.getMailUser().length() > 0 && !mailAccount.getMailPassword().equals("")) {
				MailAccountDAO.create(mailAccount);
			}
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_SQL),
					e.getMessage());
		}
	}
	
	@Override
	public void deleteMailAccount(long id) throws OKMException {
		log.debug("deleteMailAccount({})", id);
		updateSessionManager();
		
		try {
			MailAccountDAO.delete(id);
		} catch (DatabaseException e) {
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_SQL),
					e.getMessage());
		}
	}
	
	@Override
	public String isValidPassword(String password) throws OKMException {
		log.debug("isValidPassword()");
		String msg = "";
		updateSessionManager();
		
		try {
			PasswordValidator passwordValidator = ValidatorFactory.getPasswordValidator();
			try {
				passwordValidator.Validate(password);
			} catch (ValidatorException e) {
				msg = e.getMessage();
			}
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new OKMException(ErrorCode.get(ErrorCode.ORIGIN_OKMWorkspaceService, ErrorCode.CAUSE_Repository),
					e.getMessage());
		}
		
		return msg;
	}
}
