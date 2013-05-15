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

package com.openkm.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.dao.ConfigDAO;
import com.openkm.dao.SearchDAO;
import com.openkm.extractor.RegisteredExtractors;
import com.openkm.module.db.stuff.DbSimpleAccessManager;
import com.openkm.module.db.stuff.FsDataStore;
import com.openkm.principal.DatabasePrincipalAdapter;
import com.openkm.util.EnvironmentDetector;
import com.openkm.validator.password.NoPasswordValidator;
import com.openkm.vernum.MajorMinorVersionNumerationAdapter;

public class Config {
	private static Logger log = LoggerFactory.getLogger(Config.class);
	public static TreeMap<String, String> values = new TreeMap<String, String>();
	
	// Server specific configuration
	public static final String HOME_DIR = EnvironmentDetector.getServerHomeDir();
	public static final String TMP_DIR = EnvironmentDetector.getTempDir();
	public static final String NULL_DEVICE = EnvironmentDetector.getNullDevice();
	public static final String JNDI_BASE = EnvironmentDetector.getServerJndiBase();
	public static final boolean IN_SERVER = EnvironmentDetector.inServer();
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
	
	// Scripting
	public static final String START_SCRIPT = "start.bsh";
	public static final String STOP_SCRIPT = "stop.bsh";
	public static final String START_JAR = "start.jar";
	public static final String STOP_JAR = "stop.jar";
	
	// Configuration files
	public static final String OPENKM_CONFIG = "OpenKM.cfg";
	public static final String NODE_DEFINITIONS = "CustomNodes.cnd";
	public static String CONTEXT;
	public static String INSTANCE_HOME;
	public static String INSTANCE_DIRNAME = "instances";
	public static String INSTANCE_CHROOT_PATH;
	public static String JBPM_CONFIG;
	public static String PROPERTY_GROUPS_XML;
	public static String PROPERTY_GROUPS_CND;
	public static String DTD_BASE;
	public static String LANG_PROFILES_BASE;
	
	// Default users
	public static String PROPERTY_SYSTEM_USER = "user.system";
	public static String PROPERTY_ADMIN_USER = "user.admin";
	
	// General configuration
	public static String EXPORT_METADATA_EXT = ".okm";
	public static String ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";
	public static Version LUCENE_VERSION = Version.LUCENE_31;
	public static String DEFAULT_CRONTAB_MAIL = "noreply@openkm.com";
	
	// Default script
	public static final String PROPERTY_DEFAULT_SCRIPT = "default.script";
	
	// Preview cache
	public static String REPOSITORY_CACHE_HOME;
	public static String REPOSITORY_CACHE_DIRNAME = "cache";
	public static String REPOSITORY_CACHE_DXF;
	public static String REPOSITORY_CACHE_PDF;
	public static String REPOSITORY_CACHE_SWF;
	
	// Experimental features
	public static final String PROPERTY_PLUGIN_DEBUG = "plugin.debug";
	public static final String PROPERTY_MANAGED_TEXT_EXTRACTION = "managed.text.extraction";
	public static final String PROPERTY_MANAGED_TEXT_EXTRACTION_BATCH = "managed.text.extraction.batch";
	public static final String PROPERTY_REPOSITORY_NATIVE = "repository.native";
	public static final String PROPERTY_REPOSITORY_CONTENT_CHECKSUM = "repository.content.checksum";
	
	// Security properties
	public static final String PROPERTY_SECURITY_ACCESS_MANAGER = "security.access.manager";
	public static final String PROPERTY_SECURITY_SEARCH_EVALUATION = "security.search.evaluation";
	public static final String PROPERTY_SECURITY_MODE_MULTIPLE = "security.mode.multiple";
	
	// Configuration properties
	public static final String PROPERTY_REPOSITORY_UUID = "repository.uuid";
	public static final String PROPERTY_REPOSITORY_VERSION = "repository.version";
	public static final String PROPERTY_REPOSITORY_CONFIG = "repository.config";
	public static final String PROPERTY_REPOSITORY_HOME = "repository.home";
	public static final String PROPERTY_REPOSITORY_DATASTORE_BACKEND = "repository.datastore.backend";
	public static final String PROPERTY_REPOSITORY_DATASTORE_HOME = "repository.datastore.home";
	public static final String PROPERTY_REPOSITORY_CACHE_HOME = "repository.cache.home";
	public static final String PROPERTY_VERSION_NUMERATION_ADAPTER = "version.numeration.adapter";
	public static final String PROPERTY_MAX_FILE_SIZE = "max.file.size";
	public static final String PROPERTY_MAX_SEARCH_RESULTS = "max.search.results";
	public static final String PROPERTY_MIN_SEARCH_CHARACTERS = "min.search.characters";
	
	public static final String PROPERTY_DEFAULT_USER_ROLE = "default.user.role";
	public static final String PROPERTY_DEFAULT_ADMIN_ROLE = "default.admin.role";
	
	// Text extractors
	public static final String PROPERTY_REGISTERED_TEXT_EXTRACTORS = "registered.text.extractors";
	
	// Workflow
	public static final String PROPERTY_WORKFLOW_START_TASK_AUTO_RUN = "workflow.start.task.auto.run";
	public static final String PROPERTY_WORKFLOW_RUN_CONFIG_FORM = "workflow.run.config.form";
	
	// Principal
	public static final String PROPERTY_PRINCIPAL_ADAPTER = "principal.adapter";
	public static final String PROPERTY_PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS = "principal.database.filter.inactive.users";
	public static final String PROPERTY_PRINCIPAL_HIDE_CONNECTION_ROLES = "principal.hide.connection.roles";
	
	// LDAP
	public static final String PROPERTY_PRINCIPAL_LDAP_SERVER = "principal.ldap.server";
	public static final String PROPERTY_PRINCIPAL_LDAP_SECURITY_PRINCIPAL = "principal.ldap.security.principal";
	public static final String PROPERTY_PRINCIPAL_LDAP_SECURITY_CREDENTIALS = "principal.ldap.security.credentials";
	public static final String PROPERTY_PRINCIPAL_LDAP_REFERRAL = "principal.ldap.referral";
	public static final String PROPERTY_PRINCIPAL_LDAP_USERS_FROM_ROLES = "principal.ldap.users.from.roles";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_BASE = "principal.ldap.user.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_FILTER = "principal.ldap.user.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_USER_ATTRIBUTE = "principal.ldap.user.attribute";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_BASE = "principal.ldap.role.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_FILTER = "principal.ldap.role.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLE_ATTRIBUTE = "principal.ldap.role.attribute";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_BASE = "principal.ldap.username.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER = "principal.ldap.username.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_USERNAME_ATTRIBUTE = "principal.ldap.username.attribute";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_BASE = "principal.ldap.mail.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_FILTER = "principal.ldap.mail.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_MAIL_ATTRIBUTE = "principal.ldap.mail.attribute";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE = "principal.ldap.users.by.role.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER = "principal.ldap.users.by.role.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE = "principal.ldap.users.by.role.attribute";
	
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE = "principal.ldap.roles.by.user.search.base";
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER = "principal.ldap.roles.by.user.search.filter";
	public static final String PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE = "principal.ldap.roles.by.user.attribute";
	
	public static final String PROPERTY_RESTRICT_FILE_MIME = "restrict.file.mime";
	public static final String PROPERTY_RESTRICT_FILE_NAME = "restrict.file.name";
	
	public static final String PROPERTY_NOTIFICATION_MESSAGE_SUBJECT = "notification.message.subject";
	public static final String PROPERTY_NOTIFICATION_MESSAGE_BODY = "notification.message.body";
	
	public static final String PROPERTY_SUBSCRIPTION_MESSAGE_SUBJECT = "subscription.message.subject";
	public static final String PROPERTY_SUBSCRIPTION_MESSAGE_BODY = "subscription.message.body";
	
	public static final String PROPERTY_SUBSCRIPTION_TWITTER_USER = "notify.twitter.user";
	public static final String PROPERTY_SUBSCRIPTION_TWITTER_PASSWORD = "notify.twitter.password";
	public static final String PROPERTY_SUBSCRIPTION_TWITTER_STATUS = "notify.twitter.status";
	
	public static final String PROPERTY_SYSTEM_APACHE_REQUEST_HEADER_FIX = "system.apache.request.header.fix";
	public static final String PROPERTY_SYSTEM_WEBDAV_SERVER = "system.webdav.server";
	public static final String PROPERTY_SYSTEM_WEBDAV_FIX = "system.webdav.fix";
	public static final String PROPERTY_SYSTEM_READONLY = "system.readonly";
	public static final String PROPERTY_SYSTEM_MAINTENANCE = "system.maintenance";
	public static final String PROPERTY_SYSTEM_OCR = "system.ocr";
	public static final String PROPERTY_SYSTEM_OCR_ROTATE = "system.ocr.rotate";
	public static final String PROPERTY_SYSTEM_PDF_FORCE_OCR = "system.pdf.force.ocr";
	public static final String PROPERTY_SYSTEM_OPENOFFICE_PATH = "system.openoffice.path";
	public static final String PROPERTY_SYSTEM_OPENOFFICE_TASKS = "system.openoffice.tasks";
	public static final String PROPERTY_SYSTEM_OPENOFFICE_PORT = "system.openoffice.port";
	public static final String PROPERTY_SYSTEM_OPENOFFICE_DICTIONARY = "system.openoffice.dictionary";
	public static final String PROPERTY_SYSTEM_IMAGEMAGICK_CONVERT = "system.imagemagick.convert";
	public static final String PROPERTY_SYSTEM_SWFTOOLS_PDF2SWF = "system.swftools.pdf2swf";
	public static final String PROPERTY_SYSTEM_GHOSTSCRIPT_PS2PDF = "system.ghostscript.ps2pdf";
	public static final String PROPERTY_SYSTEM_ANTIVIR = "system.antivir";
	public static final String PROPERTY_SYSTEM_LOGIN_LOWERCASE = "system.login.lowercase";
	public static final String PROPERTY_SYSTEM_PREVIEWER = "system.previewer";
	public static final String PROPERTY_SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK = "system.document.name.mismatch.check";
	public static final String PROPERTY_SYSTEM_KEYWORD_LOWERCASE = "system.keyword.lowercase";
	public static final String PROPERTY_SYSTEM_EXECUTION_TIMEOUT = "system.execution.timeout";
	
	public static final String PROPERTY_UPDATE_INFO = "update.info";
	public static final String PROPERTY_APPLICATION_URL = "application.url";
	public static final String PROPERTY_DEFAULT_LANG = "default.lang";
	public static final String PROPERTY_USER_ASSIGN_DOCUMENT_CREATION = "user.assign.document.creation";
	public static final String PROPERTY_USER_KEYWORDS_CACHE = "user.keywords.cache";
	public static final String PROPERTY_USER_ITEM_CACHE = "user.item.cache";
	public static final String PROPERTY_UPLOAD_THROTTLE_FILTER = "upload.throttle.filter";
	
	// Schedule
	public static final String PROPERTY_SCHEDULE_SESSION_KEEPALIVE = "schedule.session.keepalive";
	public static final String PROPERTY_SCHEDULE_DASHBOARD_REFRESH = "schedule.dashboard.refresh";
	public static final String PROPERTY_SCHEDULE_UI_NOTIFICATION = "schedule.ui.notification";
	
	// KEA
	// Used in generate_thesaurus.jsp
	public static final String PROPERTY_KEA_THESAURUS_SKOS_FILE = "kea.thesaurus.skos.file";
	public static final String PROPERTY_KEA_THESAURUS_OWL_FILE = "kea.thesaurus.owl.file";
	public static final String PROPERTY_KEA_THESAURUS_VOCABULARY_SERQL = "kea.thesaurus.vocabulary.serql";
	public static final String PROPERTY_KEA_THESAURUS_BASE_URL = "kea.thesaurus.base.url";
	public static final String PROPERTY_KEA_THESAURUS_TREE_ROOT = "kea.thesaurus.tree.root";
	public static final String PROPERTY_KEA_THESAURUS_TREE_CHILDS = "kea.thesaurus.tree.childs";
	
	// Validator
	public static final String PROPERTY_VALIDATOR_PASSWORD = "validator.password";
	
	public static final String PROPERTY_VALIDATOR_PASSWORD_MIN_LENGTH = "validator.password.min.length";
	public static final String PROPERTY_VALIDATOR_PASSWORD_MAX_LENGTH = "validator.password.max.length";
	public static final String PROPERTY_VALIDATOR_PASSWORD_MIN_LOWERCASE = "validator.password.min.lowercase";
	public static final String PROPERTY_VALIDATOR_PASSWORD_MIN_UPPERCASE = "validator.password.min.uppercase";
	public static final String PROPERTY_VALIDATOR_PASSWORD_MIN_DIGITS = "validator.password.min.digits";
	public static final String PROPERTY_VALIDATOR_PASSWORD_MIN_SPECIAL = "validator.password.mini.special";

	// Hibernate
	public static final String PROPERTY_HIBERNATE_DIALECT = "hibernate.dialect";
	public static final String PROPERTY_HIBERNATE_DATASOURCE = "hibernate.datasource";
	public static final String PROPERTY_HIBERNATE_HBM2DDL = "hibernate.hbm2ddl"; // Used in login.jsp
	public static final String PROPERTY_HIBERNATE_SHOW_SQL = "hibernate.show_sql";
	public static final String PROPERTY_HIBERNATE_STATISTICS = "hibernate.statistics";
	public static final String PROPERTY_HIBERNATE_SEARCH_ANALYZER = "hibernate.search.analyzer";
	public static final String PROPERTY_HIBERNATE_CREATE_AUTOFIX = "hibernate.create.autofix";
	
	// Hibernate Search indexes
	public static String PROPERTY_HIBERNATE_SEARCH_INDEX_HOME = "hibernate.search.index.home";
		
	/**
	 *  Default values
	 */
	// Experimental features
	public static boolean PLUGIN_DEBUG = false;
	public static boolean MANAGED_TEXT_EXTRACTION = true;
	public static int MANAGED_TEXT_EXTRACTION_BATCH = 10;
	public static boolean REPOSITORY_NATIVE = true;
	public static boolean REPOSITORY_CONTENT_CHECKSUM = true;
	
	// Security properties
	public static String SECURITY_ACCESS_MANAGER = "";
	public static String SECURITY_SEARCH_EVALUATION = "";
	public static boolean SECURITY_MODE_MULTIPLE = true;
	
	// Configuration properties
	public static String REPOSITORY_CONFIG;
	public static String REPOSITORY_HOME;
	public static String REPOSITORY_DIRNAME = "repository";
	public static String REPOSITORY_DATASTORE_BACKEND;
	public static String REPOSITORY_DATASTORE_HOME;
	public static String DEFAULT_SCRIPT = "print(\"UserId: \" + session.getUserID());\n" +
		"print(\"EventType: \" + eventType);\n" +
		"print(\"EventNode: \" + eventNode.getPath());\n" +
		"print(\"ScriptNode: \" + scriptNode.getPath());";
	public static String VERSION_NUMERATION_ADAPTER = MajorMinorVersionNumerationAdapter.class.getCanonicalName();
	public static long MAX_FILE_SIZE;
	public static int MAX_SEARCH_RESULTS;
	public static int MIN_SEARCH_CHARACTERS;
	
	public static String SYSTEM_USER = "system";
	public static String ADMIN_USER = "okmAdmin";
	
	public static String DEFAULT_USER_ROLE = "ROLE_USER";
	public static String DEFAULT_ADMIN_ROLE = "ROLE_ADMIN";
	
	// Text extractors
	public static List<String> REGISTERED_TEXT_EXTRACTORS = new ArrayList<String>(); 
	private static final String DEFAULT_REGISTERED_TEXT_EXTRACTORS = 
		"org.apache.jackrabbit.extractor.PlainTextExtractor\n" +
		"org.apache.jackrabbit.extractor.MsWordTextExtractor\n" +
		"org.apache.jackrabbit.extractor.MsExcelTextExtractor\n" +
		"org.apache.jackrabbit.extractor.MsPowerPointTextExtractor\n" +
		"org.apache.jackrabbit.extractor.OpenOfficeTextExtractor\n" +
		"org.apache.jackrabbit.extractor.RTFTextExtractor\n" +
		"org.apache.jackrabbit.extractor.HTMLTextExtractor\n" +
		"org.apache.jackrabbit.extractor.XMLTextExtractor\n" +
		"org.apache.jackrabbit.extractor.PngTextExtractor\n" +
		"org.apache.jackrabbit.extractor.MsOutlookTextExtractor\n" +
		"com.openkm.extractor.PdfTextExtractor\n" +
		"com.openkm.extractor.AudioTextExtractor\n" +
		"com.openkm.extractor.ExifTextExtractor\n" +
		"com.openkm.extractor.CuneiformTextExtractor\n" +
		"com.openkm.extractor.SourceCodeTextExtractor\n" +
		"com.openkm.extractor.MsOffice2007TextExtractor";
	
	// Workflow
	public static String WORKFLOW_RUN_CONFIG_FORM = "run_config";
	public static boolean WORKFLOW_START_TASK_AUTO_RUN = true;
	public static String WORKFLOW_PROCESS_INSTANCE_VARIABLE_UUID = "uuid";
	public static String WORKFLOW_PROCESS_INSTANCE_VARIABLE_PATH = "path";
	
	// Principal
	public static String PRINCIPAL_ADAPTER = DatabasePrincipalAdapter.class.getCanonicalName();
	public static boolean PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS = true;
	public static boolean PRINCIPAL_HIDE_CONNECTION_ROLES = false;
	
	// LDAP
	public static String PRINCIPAL_LDAP_SERVER; // ldap://phoenix.server:389
	public static String PRINCIPAL_LDAP_SECURITY_PRINCIPAL; //"cn=Administrator,cn=Users,dc=openkm,dc=com"
	public static String PRINCIPAL_LDAP_SECURITY_CREDENTIALS; // "xxxxxx"
	public static String PRINCIPAL_LDAP_REFERRAL;
	public static boolean PRINCIPAL_LDAP_USERS_FROM_ROLES;
	
	public static List<String> PRINCIPAL_LDAP_USER_SEARCH_BASE = new ArrayList<String>(); // ou=people,dc=openkm,dc=com
	public static String PRINCIPAL_LDAP_USER_SEARCH_FILTER; // (&(objectClass=posixAccount)(!(objectClass=gosaUserTemplate)))
	public static String PRINCIPAL_LDAP_USER_ATTRIBUTE; // uid
	
	public static List<String> PRINCIPAL_LDAP_ROLE_SEARCH_BASE = new ArrayList<String>(); // ou=groups,dc=openkm,dc=com
	public static String PRINCIPAL_LDAP_ROLE_SEARCH_FILTER; // (&(objectClass=posixGroup)(cn=*)(|(description=*OpenKM*)(cn=users)))
	public static String PRINCIPAL_LDAP_ROLE_ATTRIBUTE; // cn
	
	public static String PRINCIPAL_LDAP_USERNAME_SEARCH_BASE; // ou=people,dc=openkm,dc=com
	public static String PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER; // (&(objectClass=posixAccount)(!(objectClass=gosaUserTemplate)))
	public static String PRINCIPAL_LDAP_USERNAME_ATTRIBUTE; // displayName
	
	public static String PRINCIPAL_LDAP_MAIL_SEARCH_BASE; // uid={0},ou=people,dc=openkm,dc=com
	public static String PRINCIPAL_LDAP_MAIL_SEARCH_FILTER; // (&(objectClass=inetOrgPerson)(mail=*))
	public static String PRINCIPAL_LDAP_MAIL_ATTRIBUTE; // mail
	
	public static String PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE; 
	public static String PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER; // (&(objectClass=group)(cn={0}))
	public static String PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE;
	
	public static String PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE;
	public static String PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER; // (&(objectClass=group)(cn={0}))
	public static String PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE;
	
	public static boolean RESTRICT_FILE_MIME;
	public static String RESTRICT_FILE_NAME;

	public static String NOTIFICATION_MESSAGE_SUBJECT;
	public static String NOTIFICATION_MESSAGE_BODY;

	public static String SUBSCRIPTION_MESSAGE_SUBJECT;
	public static String SUBSCRIPTION_MESSAGE_BODY;
		
	public static String SUBSCRIPTION_TWITTER_USER;
	public static String SUBSCRIPTION_TWITTER_PASSWORD;
	public static String SUBSCRIPTION_TWITTER_STATUS;
	
	public static boolean SYSTEM_APACHE_REQUEST_HEADER_FIX;
	public static boolean SYSTEM_WEBDAV_SERVER;
	public static boolean SYSTEM_WEBDAV_FIX;
	public static boolean SYSTEM_MAINTENANCE;
	public static boolean SYSTEM_READONLY;
	public static String SYSTEM_OCR = "";
	public static String SYSTEM_OCR_ROTATE = "";
	public static boolean SYSTEM_PDF_FORCE_OCR;
	public static String SYSTEM_OPENOFFICE_PATH = "";
	public static int SYSTEM_OPENOFFICE_TASKS;
	public static int SYSTEM_OPENOFFICE_PORT;
	public static String SYSTEM_OPENOFFICE_DICTIONARY = "";
	public static String SYSTEM_IMAGEMAGICK_CONVERT = "";
	public static String SYSTEM_SWFTOOLS_PDF2SWF = "";
	public static String SYSTEM_GHOSTSCRIPT_PS2PDF = "";
	public static String SYSTEM_ANTIVIR = "";
	public static boolean SYSTEM_LOGIN_LOWERCASE = false;
	public static String SYSTEM_PREVIEWER = "";
	public static boolean SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK = true;
	public static boolean SYSTEM_KEYWORD_LOWERCASE = false;
	public static int SYSTEM_EXECUTION_TIMEOUT = 5; // 5 min
	
	public static boolean UPDATE_INFO;
	public static String APPLICATION_URL;
	public static String APPLICATION_BASE;
	public static String DEFAULT_LANG = "";
	public static boolean USER_ASSIGN_DOCUMENT_CREATION = true;
	public static boolean USER_KEYWORDS_CACHE = false;
	public static boolean USER_ITEM_CACHE;
	public static boolean UPLOAD_THROTTLE_FILTER;
	
	// Schedule
	public static int SCHEDULE_SESSION_KEEPALIVE = 15; // 15 min
	public static int SCHEDULE_DASHBOARD_REFRESH = 30; // 30 min
	public static int SCHEDULE_UI_NOTIFICATION = 1; // 1 min

	// KEA
	public static String KEA_THESAURUS_SKOS_FILE;
	public static String KEA_THESAURUS_OWL_FILE;
	public static String KEA_THESAURUS_VOCABULARY_SERQL;
	public static String KEA_THESAURUS_BASE_URL;
	public static String KEA_THESAURUS_TREE_ROOT;
	public static String KEA_THESAURUS_TREE_CHILDS;

	// Validator
	public static String VALIDATOR_PASSWORD = NoPasswordValidator.class.getCanonicalName();
	
	public static int VALIDATOR_PASSWORD_MIN_LENGTH;
	public static int VALIDATOR_PASSWORD_MAX_LENGTH;
	public static int VALIDATOR_PASSWORD_MIN_LOWERCASE;
	public static int VALIDATOR_PASSWORD_MIN_UPPERCASE;
	public static int VALIDATOR_PASSWORD_MIN_DIGITS;
	public static int VALIDATOR_PASSWORD_MIN_SPECIAL;
	
	public static String VALIDATOR_PASSWORD_ERROR_MIN_LENGTH = "Password error: too short";
	public static String VALIDATOR_PASSWORD_ERROR_MAX_LENGTH = "Password error: too long";	
	public static String VALIDATOR_PASSWORD_ERROR_MIN_LOWERCASE = "Password error: too few lowercase characters";
	public static String VALIDATOR_PASSWORD_ERROR_MIN_UPPERCASE = "Password error: too few uppercase characters";
	public static String VALIDATOR_PASSWORD_ERROR_MIN_DIGITS = "Password error: too few digits";
	public static String VALIDATOR_PASSWORD_ERROR_MIN_SPECIAL = "Password error: too few special characters";
	
	// Hibernate
	public static String HIBERNATE_DIALECT = "org.hibernate.dialect.HSQLDialect";
	public static String HIBERNATE_DATASOURCE = JNDI_BASE + "jdbc/OpenKMDS";
	public static String HIBERNATE_HBM2DDL = "create";
	public static String HIBERNATE_SHOW_SQL = "false";
	public static String HIBERNATE_STATISTICS = "false";
	public static String HIBERNATE_SEARCH_ANALYZER = "org.apache.lucene.analysis.standard.StandardAnalyzer";
	public static String HIBERNATE_CREATE_AUTOFIX = "true";
	public static boolean HIBERNATE_INDEXER_MASS_INDEXER = false;
	public static int HIBERNATE_INDEXER_BATCH_SIZE_LOAD_OBJECTS = 30;
	public static int HIBERNATE_INDEXER_THREADS_SUBSEQUENT_FETCHING = 8;
	public static int HIBERNATE_INDEXER_THREADS_LOAD_OBJECTS = 4;
	public static int HIBERNATE_INDEXER_THREADS_INDEX_WRITER = 3;
	
	// Hibernate Search indexes
	public static String HIBERNATE_SEARCH_INDEX_HOME;
	public static String HIBERNATE_SEARCH_INDEX_DIRNAME = "index";
	
	// Misc
	public static int SESSION_EXPIRATION = 1800; // 30 mins (session.getMaxInactiveInterval())
	public static String LIST_SEPARATOR = ";";
	
	/**
	 * Get url base
	 */
	private static String getBase(String url) {
		String ret = "";
		
		int idx = url.lastIndexOf('/');
		if (idx > 0) ret = url.substring(0, idx);
		
		return ret;
	}
		
	/**
	 * Load OpenKM configuration from OpenKM.cfg 
	 */
	public static Properties load(ServletContext sc) {
		Properties config = new Properties();
		String configFile = HOME_DIR + File.separator + OPENKM_CONFIG;
		CONTEXT = sc.getContextPath().substring(1);
		
		// Initialize DTD location
		DTD_BASE = sc.getRealPath("WEB-INF/classes/dtd");
		log.info("** Application {} has DTDs at {} **", sc.getServletContextName(), DTD_BASE);
        
		// Initialize language profiles location
		LANG_PROFILES_BASE = sc.getRealPath("WEB-INF/classes/lang-profiles");
		log.info("** Language profiles at {} **", LANG_PROFILES_BASE);
		
		// Read config
		try {
			log.info("** Reading config file " + configFile + " **");
			FileInputStream fis = new FileInputStream(configFile);
			config.load(fis);
			fis.close();
			
			// Hibernate
			HIBERNATE_DIALECT = config.getProperty(PROPERTY_HIBERNATE_DIALECT, HIBERNATE_DIALECT);
			values.put(PROPERTY_HIBERNATE_DIALECT, HIBERNATE_DIALECT);
			HIBERNATE_DATASOURCE = config.getProperty(PROPERTY_HIBERNATE_DATASOURCE, JNDI_BASE + "jdbc/" + CONTEXT + "DS");
			values.put(PROPERTY_HIBERNATE_DATASOURCE, HIBERNATE_DATASOURCE);
			HIBERNATE_HBM2DDL = config.getProperty(PROPERTY_HIBERNATE_HBM2DDL, HIBERNATE_HBM2DDL);
			values.put(PROPERTY_HIBERNATE_HBM2DDL, HIBERNATE_HBM2DDL);
			HIBERNATE_SHOW_SQL = config.getProperty(PROPERTY_HIBERNATE_SHOW_SQL, HIBERNATE_SHOW_SQL);
			values.put(PROPERTY_HIBERNATE_SHOW_SQL, HIBERNATE_SHOW_SQL);
			HIBERNATE_STATISTICS = config.getProperty(PROPERTY_HIBERNATE_STATISTICS, HIBERNATE_STATISTICS);
			values.put(PROPERTY_HIBERNATE_STATISTICS, HIBERNATE_STATISTICS);
			HIBERNATE_SEARCH_ANALYZER = config.getProperty(PROPERTY_HIBERNATE_SEARCH_ANALYZER, HIBERNATE_SEARCH_ANALYZER);
			values.put(PROPERTY_HIBERNATE_SEARCH_ANALYZER, HIBERNATE_SEARCH_ANALYZER);
			HIBERNATE_CREATE_AUTOFIX = config.getProperty(PROPERTY_HIBERNATE_CREATE_AUTOFIX, HIBERNATE_CREATE_AUTOFIX);
			values.put(PROPERTY_HIBERNATE_CREATE_AUTOFIX, HIBERNATE_CREATE_AUTOFIX);
			
			// Misc
			if (config.getProperty(PROPERTY_REPOSITORY_NATIVE) != null) {
				REPOSITORY_NATIVE = "on".equalsIgnoreCase(config.getProperty(PROPERTY_REPOSITORY_NATIVE, "off"));
			}
			
			values.put(PROPERTY_REPOSITORY_NATIVE, Boolean.toString(REPOSITORY_NATIVE));
			INSTANCE_HOME = HOME_DIR;
			values.put("instance.home", INSTANCE_HOME);
			INSTANCE_CHROOT_PATH = "";
			values.put("instance.chroot.path", INSTANCE_CHROOT_PATH);
			
			// Preview cache & Repository datastore backend & Hibernate Search indexes
			if (Config.REPOSITORY_NATIVE) {
				REPOSITORY_CONFIG = NULL_DEVICE;
				REPOSITORY_HOME = config.getProperty(PROPERTY_REPOSITORY_HOME, INSTANCE_HOME + File.separator + REPOSITORY_DIRNAME);
				REPOSITORY_CACHE_HOME = config.getProperty(PROPERTY_REPOSITORY_CACHE_HOME, Config.REPOSITORY_HOME + File.separator + REPOSITORY_CACHE_DIRNAME);
				REPOSITORY_DATASTORE_BACKEND = config.getProperty(PROPERTY_REPOSITORY_DATASTORE_BACKEND, FsDataStore.DATASTORE_BACKEND_FS);
				REPOSITORY_DATASTORE_HOME = config.getProperty(PROPERTY_REPOSITORY_DATASTORE_HOME, Config.REPOSITORY_HOME + File.separator + FsDataStore.DATASTORE_DIRNAME);
				HIBERNATE_SEARCH_INDEX_HOME = config.getProperty(PROPERTY_HIBERNATE_SEARCH_INDEX_HOME, Config.REPOSITORY_HOME + File.separator + HIBERNATE_SEARCH_INDEX_DIRNAME);
			} else {
				REPOSITORY_CONFIG = config.getProperty(PROPERTY_REPOSITORY_CONFIG, INSTANCE_HOME + File.separator + "repository.xml");
				REPOSITORY_HOME = config.getProperty(PROPERTY_REPOSITORY_HOME, INSTANCE_HOME + File.separator + REPOSITORY_DIRNAME);
				REPOSITORY_CACHE_HOME = config.getProperty(PROPERTY_REPOSITORY_CACHE_HOME, INSTANCE_HOME + File.separator + REPOSITORY_CACHE_DIRNAME);
				REPOSITORY_DATASTORE_BACKEND = config.getProperty(PROPERTY_REPOSITORY_DATASTORE_BACKEND, "");
				REPOSITORY_DATASTORE_HOME = Config.REPOSITORY_HOME + File.separator + FsDataStore.DATASTORE_DIRNAME;
				HIBERNATE_SEARCH_INDEX_HOME = Config.REPOSITORY_HOME + File.separator + HIBERNATE_SEARCH_INDEX_DIRNAME;
			}
			
			values.put(PROPERTY_REPOSITORY_CACHE_HOME, REPOSITORY_CACHE_HOME);
			REPOSITORY_CACHE_DXF = REPOSITORY_CACHE_HOME + File.separator + "dxf";
			values.put("repository.cache.dxf", REPOSITORY_CACHE_DXF);
			REPOSITORY_CACHE_PDF = REPOSITORY_CACHE_HOME + File.separator + "pdf";
			values.put("repository.cache.pdf", REPOSITORY_CACHE_PDF);
			REPOSITORY_CACHE_SWF = REPOSITORY_CACHE_HOME + File.separator + "swf";
			values.put("repository.cache.swf", REPOSITORY_CACHE_SWF);
			values.put(PROPERTY_HIBERNATE_SEARCH_INDEX_HOME, HIBERNATE_SEARCH_INDEX_HOME);
			values.put(PROPERTY_REPOSITORY_DATASTORE_BACKEND, REPOSITORY_DATASTORE_BACKEND);
			values.put(PROPERTY_REPOSITORY_DATASTORE_HOME, REPOSITORY_DATASTORE_HOME);
			values.put(PROPERTY_REPOSITORY_CONFIG, REPOSITORY_CONFIG);
			values.put(PROPERTY_REPOSITORY_HOME, REPOSITORY_HOME);
			
			JBPM_CONFIG = INSTANCE_HOME + File.separator + "jbpm.xml";
			values.put("jbpm.config", JBPM_CONFIG);
			
			PROPERTY_GROUPS_XML = INSTANCE_HOME + File.separator + "PropertyGroups.xml";
			values.put("property.groups.xml", PROPERTY_GROUPS_XML);
			PROPERTY_GROUPS_CND = INSTANCE_HOME + File.separator + "PropertyGroups.cnd";
			values.put("property.groups.cnd", PROPERTY_GROUPS_CND);
			
			for (Entry<String, String> entry : values.entrySet()) {
				log.info("LOAD - {}={}", entry.getKey(), entry.getValue());
			}
		} catch (FileNotFoundException e) {
			log.warn("** No {} file found, set default config **", OPENKM_CONFIG);
		} catch (IOException e) {
			log.warn("** IOError reading {}, set default config **", OPENKM_CONFIG);
		}
		
		return config;
	}
	
	/**
	 * Reload OpenKM configuration from database
	 */
	public static void reload(ServletContext sc, Properties cfg) {
		try {
			// Experimental features
			PLUGIN_DEBUG = ConfigDAO.getBoolean(PROPERTY_PLUGIN_DEBUG, PLUGIN_DEBUG);
			values.put(PROPERTY_PLUGIN_DEBUG, Boolean.toString(PLUGIN_DEBUG));
			MANAGED_TEXT_EXTRACTION = ConfigDAO.getBoolean(PROPERTY_MANAGED_TEXT_EXTRACTION, MANAGED_TEXT_EXTRACTION);
			values.put(PROPERTY_MANAGED_TEXT_EXTRACTION, Boolean.toString(MANAGED_TEXT_EXTRACTION));
			MANAGED_TEXT_EXTRACTION_BATCH = ConfigDAO.getInteger(PROPERTY_MANAGED_TEXT_EXTRACTION_BATCH, MANAGED_TEXT_EXTRACTION_BATCH);
			values.put(PROPERTY_MANAGED_TEXT_EXTRACTION_BATCH, Integer.toString(MANAGED_TEXT_EXTRACTION_BATCH));
			
			REPOSITORY_CONTENT_CHECKSUM = ConfigDAO.getBoolean(PROPERTY_REPOSITORY_CONTENT_CHECKSUM, REPOSITORY_CONTENT_CHECKSUM);
			values.put(PROPERTY_REPOSITORY_CONTENT_CHECKSUM, Boolean.toString(REPOSITORY_CONTENT_CHECKSUM));
			
			// Security properties
			SECURITY_ACCESS_MANAGER = ConfigDAO.getString(PROPERTY_SECURITY_ACCESS_MANAGER, DbSimpleAccessManager.NAME);
			values.put(PROPERTY_SECURITY_ACCESS_MANAGER, SECURITY_ACCESS_MANAGER);
			SECURITY_SEARCH_EVALUATION = ConfigDAO.getString(PROPERTY_SECURITY_SEARCH_EVALUATION, SearchDAO.SEARCH_LUCENE);
			values.put(PROPERTY_SECURITY_SEARCH_EVALUATION, SECURITY_SEARCH_EVALUATION);
			SECURITY_MODE_MULTIPLE = ConfigDAO.getBoolean(PROPERTY_SECURITY_MODE_MULTIPLE, SECURITY_MODE_MULTIPLE);
			values.put(PROPERTY_SECURITY_MODE_MULTIPLE, Boolean.toString(SECURITY_MODE_MULTIPLE));
			
			VERSION_NUMERATION_ADAPTER = ConfigDAO.getString(PROPERTY_VERSION_NUMERATION_ADAPTER, cfg.getProperty(PROPERTY_VERSION_NUMERATION_ADAPTER, VERSION_NUMERATION_ADAPTER));
			values.put(PROPERTY_VERSION_NUMERATION_ADAPTER, VERSION_NUMERATION_ADAPTER);
			
			MAX_FILE_SIZE = ConfigDAO.getLong(PROPERTY_MAX_FILE_SIZE, 0) * 1024 * 1024; // 0 * 1024 * 1024 = 0 MB;
			values.put(PROPERTY_MAX_FILE_SIZE, Long.toString(MAX_FILE_SIZE));
			MAX_SEARCH_RESULTS = ConfigDAO.getInteger(PROPERTY_MAX_SEARCH_RESULTS, 500);
			values.put(PROPERTY_MAX_SEARCH_RESULTS, Integer.toString(MAX_SEARCH_RESULTS));
			MIN_SEARCH_CHARACTERS = ConfigDAO.getInteger(PROPERTY_MIN_SEARCH_CHARACTERS, 3);
			values.put(PROPERTY_MIN_SEARCH_CHARACTERS, Integer.toString(MIN_SEARCH_CHARACTERS));
			
			DEFAULT_USER_ROLE = ConfigDAO.getString(PROPERTY_DEFAULT_USER_ROLE, DEFAULT_USER_ROLE);
			values.put(PROPERTY_DEFAULT_USER_ROLE, DEFAULT_USER_ROLE);
			DEFAULT_ADMIN_ROLE = ConfigDAO.getString(PROPERTY_DEFAULT_ADMIN_ROLE, DEFAULT_ADMIN_ROLE);
			values.put(PROPERTY_DEFAULT_ADMIN_ROLE, DEFAULT_ADMIN_ROLE);
			
			DEFAULT_SCRIPT = ConfigDAO.getText(PROPERTY_DEFAULT_SCRIPT, DEFAULT_SCRIPT);
			values.put(PROPERTY_DEFAULT_SCRIPT, DEFAULT_SCRIPT);
			
			// Text extractors
			REGISTERED_TEXT_EXTRACTORS = ConfigDAO.getList(PROPERTY_REGISTERED_TEXT_EXTRACTORS, DEFAULT_REGISTERED_TEXT_EXTRACTORS);
			values.put(PROPERTY_REGISTERED_TEXT_EXTRACTORS, String.valueOf(REGISTERED_TEXT_EXTRACTORS));
			RegisteredExtractors.init();
			
			// Workflow
			WORKFLOW_RUN_CONFIG_FORM = ConfigDAO.getString(PROPERTY_WORKFLOW_RUN_CONFIG_FORM, WORKFLOW_RUN_CONFIG_FORM);
			values.put(PROPERTY_WORKFLOW_RUN_CONFIG_FORM, WORKFLOW_RUN_CONFIG_FORM);
			WORKFLOW_START_TASK_AUTO_RUN = ConfigDAO.getBoolean(PROPERTY_WORKFLOW_START_TASK_AUTO_RUN, WORKFLOW_START_TASK_AUTO_RUN);			
			values.put(PROPERTY_WORKFLOW_START_TASK_AUTO_RUN, Boolean.toString(WORKFLOW_START_TASK_AUTO_RUN));
			
			// Principal
			PRINCIPAL_ADAPTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_ADAPTER, PRINCIPAL_ADAPTER);
			values.put(PROPERTY_PRINCIPAL_ADAPTER, PRINCIPAL_ADAPTER);
			PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS = ConfigDAO.getBoolean(PROPERTY_PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS, PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS);
			values.put(PROPERTY_PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS, Boolean.toString(PRINCIPAL_DATABASE_FILTER_INACTIVE_USERS));
			PRINCIPAL_HIDE_CONNECTION_ROLES = ConfigDAO.getBoolean(PROPERTY_PRINCIPAL_HIDE_CONNECTION_ROLES, PRINCIPAL_HIDE_CONNECTION_ROLES);
			values.put(PROPERTY_PRINCIPAL_HIDE_CONNECTION_ROLES, Boolean.toString(PRINCIPAL_HIDE_CONNECTION_ROLES));

			// LDAP
			PRINCIPAL_LDAP_SERVER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_SERVER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_SERVER, PRINCIPAL_LDAP_SERVER);
			PRINCIPAL_LDAP_SECURITY_PRINCIPAL = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_SECURITY_PRINCIPAL, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_SECURITY_PRINCIPAL, PRINCIPAL_LDAP_SECURITY_PRINCIPAL);
			PRINCIPAL_LDAP_SECURITY_CREDENTIALS = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_SECURITY_CREDENTIALS, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_SECURITY_CREDENTIALS, PRINCIPAL_LDAP_SECURITY_CREDENTIALS);
			PRINCIPAL_LDAP_REFERRAL = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_REFERRAL, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_REFERRAL, PRINCIPAL_LDAP_REFERRAL);
			PRINCIPAL_LDAP_USERS_FROM_ROLES = ConfigDAO.getBoolean(PROPERTY_PRINCIPAL_LDAP_USERS_FROM_ROLES, false);
			values.put(PROPERTY_PRINCIPAL_LDAP_USERS_FROM_ROLES, Boolean.toString(PRINCIPAL_LDAP_USERS_FROM_ROLES));
			
			PRINCIPAL_LDAP_USER_SEARCH_BASE = ConfigDAO.getList(PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_BASE, String.valueOf(PRINCIPAL_LDAP_USER_SEARCH_BASE));
			PRINCIPAL_LDAP_USER_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USER_SEARCH_FILTER, PRINCIPAL_LDAP_USER_SEARCH_FILTER);
			PRINCIPAL_LDAP_USER_ATTRIBUTE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USER_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USER_ATTRIBUTE, PRINCIPAL_LDAP_USER_ATTRIBUTE);

			PRINCIPAL_LDAP_ROLE_SEARCH_BASE = ConfigDAO.getList(PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_BASE, String.valueOf(PRINCIPAL_LDAP_ROLE_SEARCH_BASE));
			PRINCIPAL_LDAP_ROLE_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLE_SEARCH_FILTER, PRINCIPAL_LDAP_ROLE_SEARCH_FILTER);
			PRINCIPAL_LDAP_ROLE_ATTRIBUTE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_ROLE_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLE_ATTRIBUTE, PRINCIPAL_LDAP_ROLE_ATTRIBUTE);
			
			PRINCIPAL_LDAP_USERNAME_SEARCH_BASE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_BASE, PRINCIPAL_LDAP_USERNAME_SEARCH_BASE);
			PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER, PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER);
			PRINCIPAL_LDAP_USERNAME_ATTRIBUTE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERNAME_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERNAME_ATTRIBUTE, PRINCIPAL_LDAP_USERNAME_ATTRIBUTE);
			
			PRINCIPAL_LDAP_MAIL_SEARCH_BASE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_BASE, PRINCIPAL_LDAP_MAIL_SEARCH_BASE);
			PRINCIPAL_LDAP_MAIL_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_MAIL_SEARCH_FILTER, PRINCIPAL_LDAP_MAIL_SEARCH_FILTER);
			PRINCIPAL_LDAP_MAIL_ATTRIBUTE= ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_MAIL_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_MAIL_ATTRIBUTE, PRINCIPAL_LDAP_MAIL_ATTRIBUTE);
			
			PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE, PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE);
			PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER, PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER);
			PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE, PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE);
			
			PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE, PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE);
			PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER, PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER);
			PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE = ConfigDAO.getString(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE, "");
			values.put(PROPERTY_PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE, PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE);

			RESTRICT_FILE_MIME = ConfigDAO.getBoolean(PROPERTY_RESTRICT_FILE_MIME, false);
			values.put(PROPERTY_RESTRICT_FILE_MIME, Boolean.toString(RESTRICT_FILE_MIME));
			RESTRICT_FILE_NAME = ConfigDAO.getString(PROPERTY_RESTRICT_FILE_NAME, "*~;*.bak");
			values.put(PROPERTY_RESTRICT_FILE_NAME, RESTRICT_FILE_NAME);
			
			NOTIFICATION_MESSAGE_SUBJECT = ConfigDAO.getText(PROPERTY_NOTIFICATION_MESSAGE_SUBJECT, "OpenKM - NOTIFICATION - ${documentName}");
			values.put(PROPERTY_NOTIFICATION_MESSAGE_SUBJECT, NOTIFICATION_MESSAGE_SUBJECT);
			NOTIFICATION_MESSAGE_BODY = ConfigDAO.getText(PROPERTY_NOTIFICATION_MESSAGE_BODY, "<b>Document: </b><a href=\"${documentUrl}\">${documentPath}</a><br/><b>User: </b>${userId}<br/><b>Message: </b>${notificationMessage}<br/>");
			values.put(PROPERTY_NOTIFICATION_MESSAGE_BODY, NOTIFICATION_MESSAGE_BODY);
			
			SUBSCRIPTION_MESSAGE_SUBJECT = ConfigDAO.getText(PROPERTY_SUBSCRIPTION_MESSAGE_SUBJECT, "OpenKM - ${eventType} - ${documentPath}");
			values.put(PROPERTY_SUBSCRIPTION_MESSAGE_SUBJECT, SUBSCRIPTION_MESSAGE_SUBJECT);
			SUBSCRIPTION_MESSAGE_BODY = ConfigDAO.getText(PROPERTY_SUBSCRIPTION_MESSAGE_BODY, "<b>Document: </b><a href=\"${documentUrl}\">${documentPath}</a><br/><b>User: </b>${userId}<br/><b>Event: </b>${eventType}<br/><b>Comment: </b>${subscriptionComment}<br/>");
			values.put(PROPERTY_SUBSCRIPTION_MESSAGE_BODY, SUBSCRIPTION_MESSAGE_BODY);
			
			SUBSCRIPTION_TWITTER_USER = ConfigDAO.getString(PROPERTY_SUBSCRIPTION_TWITTER_USER, "");
			values.put(PROPERTY_SUBSCRIPTION_TWITTER_USER, SUBSCRIPTION_TWITTER_USER);
			SUBSCRIPTION_TWITTER_PASSWORD = ConfigDAO.getString(PROPERTY_SUBSCRIPTION_TWITTER_PASSWORD, "");
			values.put(PROPERTY_SUBSCRIPTION_TWITTER_PASSWORD, SUBSCRIPTION_TWITTER_PASSWORD);
			SUBSCRIPTION_TWITTER_STATUS = ConfigDAO.getText(PROPERTY_SUBSCRIPTION_TWITTER_STATUS, "OpenKM - ${documentUrl} - ${documentPath} - ${userId} - ${eventType}");
			values.put(PROPERTY_SUBSCRIPTION_TWITTER_STATUS, SUBSCRIPTION_TWITTER_STATUS);
			
			SYSTEM_APACHE_REQUEST_HEADER_FIX = ConfigDAO.getBoolean(PROPERTY_SYSTEM_APACHE_REQUEST_HEADER_FIX, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_SYSTEM_APACHE_REQUEST_HEADER_FIX, "off")));
			values.put(PROPERTY_SYSTEM_APACHE_REQUEST_HEADER_FIX, Boolean.toString(SYSTEM_APACHE_REQUEST_HEADER_FIX));
			SYSTEM_WEBDAV_SERVER = ConfigDAO.getBoolean(PROPERTY_SYSTEM_WEBDAV_SERVER, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_SYSTEM_WEBDAV_SERVER, "off")));
			values.put(PROPERTY_SYSTEM_WEBDAV_SERVER, Boolean.toString(SYSTEM_WEBDAV_SERVER));
			SYSTEM_WEBDAV_FIX = ConfigDAO.getBoolean(PROPERTY_SYSTEM_WEBDAV_FIX, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_SYSTEM_WEBDAV_FIX, "off")));
			values.put(PROPERTY_SYSTEM_WEBDAV_FIX, Boolean.toString(SYSTEM_WEBDAV_FIX));
			
			SYSTEM_MAINTENANCE = ConfigDAO.getBoolean(PROPERTY_SYSTEM_MAINTENANCE, false);
			values.put(PROPERTY_SYSTEM_MAINTENANCE, Boolean.toString(SYSTEM_MAINTENANCE));
			SYSTEM_READONLY = ConfigDAO.getBoolean(PROPERTY_SYSTEM_READONLY, false);
			values.put(PROPERTY_SYSTEM_READONLY, Boolean.toString(SYSTEM_READONLY));
			
			SYSTEM_OPENOFFICE_PATH = ConfigDAO.getString(PROPERTY_SYSTEM_OPENOFFICE_PATH, cfg.getProperty(PROPERTY_SYSTEM_OPENOFFICE_PATH, ""));
			values.put(PROPERTY_SYSTEM_OPENOFFICE_PATH, SYSTEM_OPENOFFICE_PATH);
			SYSTEM_OPENOFFICE_TASKS = ConfigDAO.getInteger(PROPERTY_SYSTEM_OPENOFFICE_TASKS, 200);
			values.put(PROPERTY_SYSTEM_OPENOFFICE_TASKS, Integer.toString(SYSTEM_OPENOFFICE_TASKS));
			SYSTEM_OPENOFFICE_PORT = ConfigDAO.getInteger(PROPERTY_SYSTEM_OPENOFFICE_PORT, 2002);
			values.put(PROPERTY_SYSTEM_OPENOFFICE_PORT, Integer.toString(SYSTEM_OPENOFFICE_PORT));
			SYSTEM_OPENOFFICE_DICTIONARY = ConfigDAO.getString(PROPERTY_SYSTEM_OPENOFFICE_DICTIONARY, "");
			values.put(PROPERTY_SYSTEM_OPENOFFICE_DICTIONARY, SYSTEM_OPENOFFICE_DICTIONARY);
			
			SYSTEM_OCR = ConfigDAO.getString(PROPERTY_SYSTEM_OCR, cfg.getProperty(PROPERTY_SYSTEM_OCR, ""));
			values.put(PROPERTY_SYSTEM_OCR, SYSTEM_OCR);
			SYSTEM_OCR_ROTATE = ConfigDAO.getString(PROPERTY_SYSTEM_OCR_ROTATE, cfg.getProperty(PROPERTY_SYSTEM_OCR_ROTATE, ""));
			values.put(PROPERTY_SYSTEM_OCR_ROTATE, SYSTEM_OCR_ROTATE);
			SYSTEM_PDF_FORCE_OCR = ConfigDAO.getBoolean(PROPERTY_SYSTEM_PDF_FORCE_OCR, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_SYSTEM_PDF_FORCE_OCR, "off")));
			values.put(PROPERTY_SYSTEM_PDF_FORCE_OCR, Boolean.toString(SYSTEM_PDF_FORCE_OCR));
			SYSTEM_IMAGEMAGICK_CONVERT = ConfigDAO.getString(PROPERTY_SYSTEM_IMAGEMAGICK_CONVERT, cfg.getProperty(PROPERTY_SYSTEM_IMAGEMAGICK_CONVERT, ""));
			values.put(PROPERTY_SYSTEM_IMAGEMAGICK_CONVERT, SYSTEM_IMAGEMAGICK_CONVERT);
			SYSTEM_SWFTOOLS_PDF2SWF = ConfigDAO.getString(PROPERTY_SYSTEM_SWFTOOLS_PDF2SWF, cfg.getProperty(PROPERTY_SYSTEM_SWFTOOLS_PDF2SWF, ""));
			values.put(PROPERTY_SYSTEM_SWFTOOLS_PDF2SWF, SYSTEM_SWFTOOLS_PDF2SWF);
			SYSTEM_GHOSTSCRIPT_PS2PDF = ConfigDAO.getString(PROPERTY_SYSTEM_GHOSTSCRIPT_PS2PDF, cfg.getProperty(PROPERTY_SYSTEM_GHOSTSCRIPT_PS2PDF, ""));
			values.put(PROPERTY_SYSTEM_GHOSTSCRIPT_PS2PDF, SYSTEM_GHOSTSCRIPT_PS2PDF);
			SYSTEM_ANTIVIR = ConfigDAO.getString(PROPERTY_SYSTEM_ANTIVIR, cfg.getProperty(PROPERTY_SYSTEM_ANTIVIR, ""));
			values.put(PROPERTY_SYSTEM_ANTIVIR, SYSTEM_ANTIVIR);
			SYSTEM_PREVIEWER = ConfigDAO.getSelectedOption(PROPERTY_SYSTEM_PREVIEWER, "flexpaper|zviewer");
			values.put(PROPERTY_SYSTEM_PREVIEWER, SYSTEM_PREVIEWER);
			SYSTEM_LOGIN_LOWERCASE = ConfigDAO.getBoolean(PROPERTY_SYSTEM_LOGIN_LOWERCASE, SYSTEM_LOGIN_LOWERCASE);
			values.put(PROPERTY_SYSTEM_LOGIN_LOWERCASE, Boolean.toString(SYSTEM_LOGIN_LOWERCASE));
			SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK = ConfigDAO.getBoolean(PROPERTY_SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK, SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK);
			values.put(PROPERTY_SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK, Boolean.toString(SYSTEM_DOCUMENT_NAME_MISMATCH_CHECK));
			SYSTEM_KEYWORD_LOWERCASE = ConfigDAO.getBoolean(PROPERTY_SYSTEM_KEYWORD_LOWERCASE, SYSTEM_KEYWORD_LOWERCASE);
			values.put(PROPERTY_SYSTEM_KEYWORD_LOWERCASE, Boolean.toString(SYSTEM_KEYWORD_LOWERCASE));
			
			// Modify default admin user if login lowercase is active
			if (SYSTEM_LOGIN_LOWERCASE) {
				ADMIN_USER = ADMIN_USER.toLowerCase();
			}
			
			values.put(PROPERTY_ADMIN_USER, ADMIN_USER);
			values.put(PROPERTY_SYSTEM_USER, SYSTEM_USER);
			SYSTEM_EXECUTION_TIMEOUT = ConfigDAO.getInteger(PROPERTY_SYSTEM_EXECUTION_TIMEOUT, SYSTEM_EXECUTION_TIMEOUT);
			values.put(PROPERTY_SYSTEM_EXECUTION_TIMEOUT, Integer.toString(SYSTEM_EXECUTION_TIMEOUT));
			
			// Guess default application URL
			String defaultApplicationUrl = cfg.getProperty(PROPERTY_APPLICATION_URL);
			
			if (defaultApplicationUrl == null || defaultApplicationUrl.isEmpty()) {
				defaultApplicationUrl = "http://localhost:8080/" + Config.CONTEXT + "/index.jsp";
			}
			
			APPLICATION_URL = ConfigDAO.getString(PROPERTY_APPLICATION_URL, defaultApplicationUrl);
			APPLICATION_BASE = getBase(APPLICATION_URL); 
			values.put(PROPERTY_APPLICATION_URL, APPLICATION_URL);
			DEFAULT_LANG = ConfigDAO.getString(PROPERTY_DEFAULT_LANG, DEFAULT_LANG);
			values.put(PROPERTY_DEFAULT_LANG, DEFAULT_LANG);
			UPDATE_INFO = ConfigDAO.getBoolean(PROPERTY_UPDATE_INFO, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_UPDATE_INFO, "on")));
			values.put(PROPERTY_UPDATE_INFO, Boolean.toString(UPDATE_INFO));
			
			USER_ASSIGN_DOCUMENT_CREATION = ConfigDAO.getBoolean(PROPERTY_USER_ASSIGN_DOCUMENT_CREATION, USER_ASSIGN_DOCUMENT_CREATION);
			values.put(PROPERTY_USER_ASSIGN_DOCUMENT_CREATION, Boolean.toString(USER_ASSIGN_DOCUMENT_CREATION));
			USER_KEYWORDS_CACHE = ConfigDAO.getBoolean(PROPERTY_USER_KEYWORDS_CACHE, USER_KEYWORDS_CACHE);
			values.put(PROPERTY_USER_KEYWORDS_CACHE, Boolean.toString(USER_KEYWORDS_CACHE));
			USER_ITEM_CACHE = ConfigDAO.getBoolean(PROPERTY_USER_ITEM_CACHE, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_USER_ITEM_CACHE, "on")));
			values.put(PROPERTY_USER_ITEM_CACHE, Boolean.toString(USER_ITEM_CACHE));
			UPLOAD_THROTTLE_FILTER = ConfigDAO.getBoolean(PROPERTY_UPLOAD_THROTTLE_FILTER, "on".equalsIgnoreCase(cfg.getProperty(PROPERTY_UPLOAD_THROTTLE_FILTER, "off")));
			values.put(PROPERTY_UPLOAD_THROTTLE_FILTER, Boolean.toString(UPLOAD_THROTTLE_FILTER));
			
			// Schedule
			SCHEDULE_SESSION_KEEPALIVE = ConfigDAO.getInteger(PROPERTY_SCHEDULE_SESSION_KEEPALIVE, SCHEDULE_SESSION_KEEPALIVE);
			values.put(PROPERTY_SCHEDULE_SESSION_KEEPALIVE, Integer.toString(SCHEDULE_SESSION_KEEPALIVE));
			SCHEDULE_DASHBOARD_REFRESH = ConfigDAO.getInteger(PROPERTY_SCHEDULE_DASHBOARD_REFRESH, SCHEDULE_DASHBOARD_REFRESH);
			values.put(PROPERTY_SCHEDULE_DASHBOARD_REFRESH, Integer.toString(SCHEDULE_DASHBOARD_REFRESH));
			SCHEDULE_UI_NOTIFICATION = ConfigDAO.getInteger(PROPERTY_SCHEDULE_UI_NOTIFICATION, SCHEDULE_UI_NOTIFICATION);
			values.put(PROPERTY_SCHEDULE_UI_NOTIFICATION, Integer.toString(SCHEDULE_UI_NOTIFICATION));
			
			// KEA
			KEA_THESAURUS_SKOS_FILE = ConfigDAO.getString(PROPERTY_KEA_THESAURUS_SKOS_FILE, cfg.getProperty(PROPERTY_KEA_THESAURUS_SKOS_FILE, ""));
			values.put(PROPERTY_KEA_THESAURUS_SKOS_FILE, KEA_THESAURUS_SKOS_FILE);
			KEA_THESAURUS_OWL_FILE = ConfigDAO.getString(PROPERTY_KEA_THESAURUS_OWL_FILE, cfg.getProperty(PROPERTY_KEA_THESAURUS_OWL_FILE, ""));
			values.put(PROPERTY_KEA_THESAURUS_OWL_FILE, KEA_THESAURUS_OWL_FILE);
			KEA_THESAURUS_VOCABULARY_SERQL = ConfigDAO.getText(PROPERTY_KEA_THESAURUS_VOCABULARY_SERQL, cfg.getProperty(PROPERTY_KEA_THESAURUS_VOCABULARY_SERQL, ""));
			values.put(PROPERTY_KEA_THESAURUS_VOCABULARY_SERQL, KEA_THESAURUS_VOCABULARY_SERQL);
			KEA_THESAURUS_BASE_URL = ConfigDAO.getString(PROPERTY_KEA_THESAURUS_BASE_URL, cfg.getProperty(PROPERTY_KEA_THESAURUS_BASE_URL, ""));
			values.put(PROPERTY_KEA_THESAURUS_BASE_URL, KEA_THESAURUS_BASE_URL);
			KEA_THESAURUS_TREE_ROOT = ConfigDAO.getText(PROPERTY_KEA_THESAURUS_TREE_ROOT, cfg.getProperty(PROPERTY_KEA_THESAURUS_TREE_ROOT, ""));
			values.put(PROPERTY_KEA_THESAURUS_TREE_ROOT, KEA_THESAURUS_TREE_ROOT);
			KEA_THESAURUS_TREE_CHILDS = ConfigDAO.getText(PROPERTY_KEA_THESAURUS_TREE_CHILDS, cfg.getProperty(PROPERTY_KEA_THESAURUS_TREE_CHILDS, ""));
			values.put(PROPERTY_KEA_THESAURUS_TREE_CHILDS, KEA_THESAURUS_TREE_CHILDS);
			
			// Validator
			VALIDATOR_PASSWORD = ConfigDAO.getString(PROPERTY_VALIDATOR_PASSWORD, VALIDATOR_PASSWORD);
			values.put(PROPERTY_VALIDATOR_PASSWORD, VALIDATOR_PASSWORD);
			
			VALIDATOR_PASSWORD_MIN_LENGTH = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MIN_LENGTH, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MIN_LENGTH, Integer.toString(VALIDATOR_PASSWORD_MIN_LENGTH));
			VALIDATOR_PASSWORD_MAX_LENGTH = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MAX_LENGTH, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MAX_LENGTH, Integer.toString(VALIDATOR_PASSWORD_MAX_LENGTH));
			VALIDATOR_PASSWORD_MIN_LOWERCASE = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MIN_LOWERCASE, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MIN_LOWERCASE, Integer.toString(VALIDATOR_PASSWORD_MIN_LOWERCASE));
			VALIDATOR_PASSWORD_MIN_UPPERCASE = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MIN_UPPERCASE, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MIN_UPPERCASE, Integer.toString(VALIDATOR_PASSWORD_MIN_UPPERCASE));
			VALIDATOR_PASSWORD_MIN_DIGITS = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MIN_DIGITS, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MIN_DIGITS, Integer.toString(VALIDATOR_PASSWORD_MIN_DIGITS));
			VALIDATOR_PASSWORD_MIN_SPECIAL = ConfigDAO.getInteger(PROPERTY_VALIDATOR_PASSWORD_MIN_SPECIAL, 0);
			values.put(PROPERTY_VALIDATOR_PASSWORD_MIN_SPECIAL, Integer.toString(VALIDATOR_PASSWORD_MIN_SPECIAL));
			
			for (Entry<String, String> entry : values.entrySet()) {
				log.info("RELOAD - {}={}", entry.getKey(), entry.getValue());
			}
		} catch (DatabaseException e) {
			log.error("** Error reading configuration table **");
		}
	}
}
