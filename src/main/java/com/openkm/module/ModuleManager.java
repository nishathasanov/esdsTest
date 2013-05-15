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

package com.openkm.module;

import com.openkm.core.Config;

/**
 * Choose between Native Repository or Jackrabbit implementations.
 * 
 * @author pavila
 */
public class ModuleManager {
	private static AuthModule authModule = null;
	private static RepositoryModule repositoryModule = null;
	private static FolderModule folderModule = null;
	private static DocumentModule documentModule = null;
	private static NoteModule noteModule = null;
	private static SearchModule searchModule = null;
	private static PropertyGroupModule propertyGroupModule= null;
	private static NotificationModule notificationModule = null;
	private static BookmarkModule bookmarkModule = null;
	private static DashboardModule dashboardModule = null;
	private static WorkflowModule workflowModule = null;
	private static ScriptingModule scriptingModule = null;
	private static StatsModule statsModule = null;
	private static MailModule mailModule = null;
	private static PropertyModule propertyModule = null;
	private static UserConfigModule userConfigModule = null;
	
	/**
	 * 
	 */
	public static synchronized AuthModule getAuthModule() {
		if (authModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				authModule = new com.openkm.module.db.DbAuthModule();
			} else {
				authModule = new com.openkm.module.jcr.JcrAuthModule();
			}
		}
		
		return authModule;
	}

	/**
	 * 
	 */
	public static synchronized RepositoryModule getRepositoryModule() {
		if (repositoryModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				repositoryModule = new com.openkm.module.db.DbRepositoryModule();
			} else {
				repositoryModule = new com.openkm.module.jcr.JcrRepositoryModule();
			}
		}
		
		return repositoryModule;
	}

	/**
	 * 
	 */
	public static synchronized FolderModule getFolderModule() {
		if (folderModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				folderModule = new com.openkm.module.db.DbFolderModule();
			} else {
				folderModule = new com.openkm.module.jcr.JcrFolderModule();
			}
		}
		
		return folderModule;
	}

	/**
	 * 
	 */
	public static synchronized DocumentModule getDocumentModule() {
		if (documentModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				documentModule = new com.openkm.module.db.DbDocumentModule();
			} else {
				documentModule = new com.openkm.module.jcr.JcrDocumentModule();
			}
		}
		
		return documentModule;
	}
	
	/**
	 * 
	 */
	public static synchronized NoteModule getNoteModule() {
		if (noteModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				noteModule = new com.openkm.module.db.DbNoteModule();
			} else {
				noteModule = new com.openkm.module.jcr.JcrNoteModule();
			}
		}
		
		return noteModule;
	}

	/**
	 * 
	 */
	public static synchronized SearchModule getSearchModule() {
		if (searchModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				searchModule = new com.openkm.module.db.DbSearchModule();
			} else {
				searchModule = new com.openkm.module.jcr.JcrSearchModule();
			}
		}
		
		return searchModule;
	}
	
	/**
	 * 
	 */
	public static synchronized PropertyGroupModule getPropertyGroupModule() {
		if (propertyGroupModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				propertyGroupModule = new com.openkm.module.db.DbPropertyGroupModule();
			} else {
				propertyGroupModule = new com.openkm.module.jcr.JcrPropertyGroupModule();
			}
		}
		
		return propertyGroupModule;
	}	

	/**
	 * 
	 */
	public static synchronized NotificationModule getNotificationModule() {
		if (notificationModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				notificationModule = new com.openkm.module.db.DbNotificationModule();
			} else {
				notificationModule = new com.openkm.module.jcr.JcrNotificationModule();
			}
		}
		
		return notificationModule;
	}
	
	/**
	 * 
	 */
	public static synchronized BookmarkModule getBookmarkModule() {
		if (bookmarkModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				bookmarkModule = new com.openkm.module.db.DbBookmarkModule();
			} else {
				bookmarkModule = new com.openkm.module.jcr.JcrBookmarkModule();
			}
		}
		
		return bookmarkModule;
	}
	
	/**
	 * 
	 */
	public static synchronized DashboardModule getDashboardModule() {
		if (dashboardModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				dashboardModule = new com.openkm.module.db.DbDashboardModule();
			} else {
				dashboardModule = new com.openkm.module.jcr.JcrDashboardModule();
			}
		}
		
		return dashboardModule;
	}
	
	/**
	 * 
	 */
	public static synchronized WorkflowModule getWorkflowModule() {
		if (workflowModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				workflowModule = new com.openkm.module.db.DbWorkflowModule();
			} else {
				workflowModule = new com.openkm.module.jcr.JcrWorkflowModule();
			}
		}
		
		return workflowModule;
	}
	
	/**
	 * 
	 */
	public static synchronized ScriptingModule getScriptingModule() {
		if (scriptingModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				scriptingModule = new com.openkm.module.db.DbScriptingModule();
			} else {
				scriptingModule = new com.openkm.module.jcr.JcrScriptingModule();
			}
		}
		
		return scriptingModule;
	}

	/**
	 * 
	 */
	public static synchronized StatsModule getStatsModule() {
		if (statsModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				statsModule = new com.openkm.module.db.DbStatsModule();
			} else {
				statsModule = new com.openkm.module.jcr.JcrStatsModule();
			}
		}
		
		return statsModule;
	}

	/**
	 * 
	 */
	public static synchronized MailModule getMailModule() {
		if (mailModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				mailModule = new com.openkm.module.db.DbMailModule();
			} else {
				mailModule = new com.openkm.module.jcr.JcrMailModule();
			}
		}
		
		return mailModule;
	}
	
	/**
	 * 
	 */
	public static synchronized PropertyModule getPropertyModule() {
		if (propertyModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				propertyModule = new com.openkm.module.db.DbPropertyModule();
			} else {
				propertyModule = new com.openkm.module.jcr.JcrPropertyModule();
			}
		}
		
		return propertyModule;
	}
	
	/**
	 * 
	 */
	public static synchronized UserConfigModule getUserConfigModule() {
		if (userConfigModule == null) {
			if (Config.REPOSITORY_NATIVE) {
				userConfigModule = new com.openkm.module.db.DbUserConfigModule();
			} else {
				userConfigModule = new com.openkm.module.jcr.JcrUserConfigModule();
			}
		}
		
		return userConfigModule;
	}
}
