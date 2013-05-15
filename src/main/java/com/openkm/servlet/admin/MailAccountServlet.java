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

package com.openkm.servlet.admin;

import static com.openkm.dao.MailAccountDAO.findRuleByPk;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Repository;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.UserMailImporter;
import com.openkm.dao.MailAccountDAO;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.bean.MailAccount;
import com.openkm.dao.bean.MailFilter;
import com.openkm.dao.bean.MailFilterRule;
import com.openkm.util.MailUtils;
import com.openkm.util.UserActivity;
import com.openkm.util.WebUtils;

/**
 * User mail accounts servlet
 */
public class MailAccountServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger(MailAccountServlet.class);
	String fields[] = { MailFilterRule.FIELD_FROM, MailFilterRule.FIELD_TO, MailFilterRule.FIELD_SUBJECT,
			MailFilterRule.FIELD_CONTENT };
	String operations[] = { MailFilterRule.OPERATION_CONTAINS, MailFilterRule.OPERATION_EQUALS };
	String protocols[] = { MailAccount.PROTOCOL_POP3, MailAccount.PROTOCOL_POP3S, MailAccount.PROTOCOL_IMAP,
			MailAccount.PROTOCOL_IMAPS };
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		log.debug("doGet({}, {})", request, response);
		request.setCharacterEncoding("UTF-8");
		String action = WebUtils.getString(request, "action");
		String userId = request.getRemoteUser();
		updateSessionManager(request);
		
		try {
			if (action.equals("create")) {
				create(userId, request, response);
			} else if (action.equals("edit")) {
				edit(userId, request, response);
			} else if (action.equals("delete")) {
				delete(userId, request, response);
			} else if (action.equals("filterList")) {
				filterList(userId, request, response);
			} else if (action.equals("filterCreate")) {
				filterCreate(userId, request, response);
			} else if (action.equals("filterEdit")) {
				filterEdit(userId, request, response);
			} else if (action.equals("filterDelete")) {
				filterDelete(userId, request, response);
			} else if (action.equals("ruleList")) {
				ruleList(userId, request, response);
			} else if (action.equals("ruleCreate")) {
				ruleCreate(userId, request, response);
			} else if (action.equals("ruleEdit")) {
				ruleEdit(userId, request, response);
			} else if (action.equals("ruleDelete")) {
				ruleDelete(userId, request, response);
			}
			
			if (action.equals("") || WebUtils.getBoolean(request, "persist")) {
				if (action.startsWith("filter")) {
					filterList(userId, request, response);
				} else if (action.startsWith("rule")) {
					ruleList(userId, request, response);
				} else {
					list(userId, request, response);
				}
			}
		} catch (PathNotFoundException e) {
			log.error(e.getMessage(), e);
			sendErrorRedirect(request, response, e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			sendErrorRedirect(request, response, e);
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			sendErrorRedirect(request, response, e);
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		log.debug("doGet({}, {})", request, response);
		request.setCharacterEncoding("UTF-8");
		String action = WebUtils.getString(request, "action");
		String userId = request.getRemoteUser();
		updateSessionManager(request);
		PrintWriter pw = response.getWriter();
		
		try {
			if (action.equals("check")) {
				check(userId, request, response);
				pw.print("Success!");
			} else if (action.equals("checkAll")) {
				UserMailImporter umi = new UserMailImporter();
				
				if (umi.isRunning()) {
					pw.print("User mail import already running");
				} else {
					umi.runAs(null);
					
					if (umi.getExceptionMessages().isEmpty()) {
						pw.print("Success!");
					} else {
						for (String em : umi.getExceptionMessages()) {
							pw.print(em + "<br/>");
						}
					}
				}
				
				// Activity log
				UserActivity.log(userId, "ADMIN_MAIL_ACCOUNT_CHECK_ALL", null, null, null);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			pw.print(e.getMessage());
		} finally {
			pw.flush();
			pw.close();
		}
	}
	
	/**
	 * New mail account
	 */
	private void create(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException {
		log.debug("create({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			MailAccount ma = new MailAccount();
			ma.setUser(WebUtils.getString(request, "ma_user"));
			ma.setMailProtocol(WebUtils.getString(request, "ma_mprotocol"));
			ma.setMailUser(WebUtils.getString(request, "ma_muser"));
			ma.setMailPassword(WebUtils.getString(request, "ma_mpassword"));
			ma.setMailHost(WebUtils.getString(request, "ma_mhost"));
			ma.setMailFolder(WebUtils.getString(request, "ma_mfolder"));
			ma.setMailMarkSeen(WebUtils.getBoolean(request, "ma_mmark_seen"));
			ma.setMailMarkDeleted(WebUtils.getBoolean(request, "ma_mmark_deleted"));
			ma.setActive(WebUtils.getBoolean(request, "ma_active"));
			MailAccountDAO.create(ma);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_ACCOUNT_CREATE", ma.getUser(), null, ma.toString());
		} else {
			ServletContext sc = getServletContext();
			MailAccount ma = new MailAccount();
			ma.setUser(WebUtils.getString(request, "ma_user"));
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma", ma);
			sc.setAttribute("protocols", protocols);
			sc.getRequestDispatcher("/admin/mail_account_edit.jsp").forward(request, response);
		}
		
		log.debug("create: void");
	}
	
	/**
	 * Edit mail account
	 */
	private void edit(String userId, HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, DatabaseException, NoSuchAlgorithmException {
		log.debug("edit({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			String password = WebUtils.getString(request, "ma_mpassword");
			MailAccount ma = new MailAccount();
			ma.setId(WebUtils.getInt(request, "ma_id"));
			ma.setUser(WebUtils.getString(request, "ma_user"));
			ma.setMailProtocol(WebUtils.getString(request, "ma_mprotocol"));
			ma.setMailUser(WebUtils.getString(request, "ma_muser"));
			ma.setMailHost(WebUtils.getString(request, "ma_mhost"));
			ma.setMailFolder(WebUtils.getString(request, "ma_mfolder"));
			ma.setMailMarkSeen(WebUtils.getBoolean(request, "ma_mmark_seen"));
			ma.setMailMarkDeleted(WebUtils.getBoolean(request, "ma_mmark_deleted"));
			ma.setActive(WebUtils.getBoolean(request, "ma_active"));
			MailAccountDAO.update(ma);
			
			if (!password.equals("")) {
				MailAccountDAO.updatePassword(ma.getId(), password);
			}
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_ACCOUNT_EDIT", Long.toString(ma.getId()), null, ma.toString());
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma", MailAccountDAO.findByPk(maId));
			sc.setAttribute("protocols", protocols);
			sc.getRequestDispatcher("/admin/mail_account_edit.jsp").forward(request, response);
		}
		
		log.debug("edit: void");
	}
	
	/**
	 * Check connectivity
	 */
	private void check(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log.debug("check({}, {}, {})", new Object[] { userId, request, response });
		MailAccount ma = new MailAccount();
		ma.setId(WebUtils.getInt(request, "ma_id"));
		ma.setUser(WebUtils.getString(request, "ma_user"));
		ma.setMailUser(WebUtils.getString(request, "ma_muser"));
		ma.setMailProtocol(WebUtils.getString(request, "ma_mprotocol"));
		ma.setMailPassword(WebUtils.getString(request, "ma_mpassword"));
		ma.setMailHost(WebUtils.getString(request, "ma_mhost"));
		ma.setMailFolder(WebUtils.getString(request, "ma_mfolder"));
		ma.setMailMarkSeen(WebUtils.getBoolean(request, "ma_mmark_seen"));
		ma.setMailMarkDeleted(WebUtils.getBoolean(request, "ma_mmark_deleted"));
		ma.setActive(WebUtils.getBoolean(request, "ma_active"));
		
		// Check
		MailUtils.testConnection(ma);
		
		// Activity log
		UserActivity.log(userId, "ADMIN_MAIL_ACCOUNT_CHECK", Long.toString(ma.getId()), null, ma.toString());
		log.debug("check: void");
	}
	
	/**
	 * Update mail account
	 */
	private void delete(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException, NoSuchAlgorithmException {
		log.debug("delete({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int maId = WebUtils.getInt(request, "ma_id");
			MailAccountDAO.delete(maId);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_ACCOUNT_DELETE", Integer.toString(maId), null, null);
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma", MailAccountDAO.findByPk(maId));
			sc.setAttribute("protocols", protocols);
			sc.getRequestDispatcher("/admin/mail_account_edit.jsp").forward(request, response);
		}
		
		log.debug("delete: void");
	}
	
	/**
	 * List mail accounts
	 */
	private void list(String userId, HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, DatabaseException {
		log.debug("list({}, {}, {})", new Object[] { userId, request, response });
		ServletContext sc = getServletContext();
		String usrId = WebUtils.getString(request, "ma_user");
		sc.setAttribute("ma_user", usrId);
		sc.setAttribute("mailAccounts", MailAccountDAO.findByUser(usrId, false));
		sc.getRequestDispatcher("/admin/mail_account_list.jsp").forward(request, response);
		log.debug("list: void");
	}
	
	/**
	 * List mail filters
	 */
	private void filterList(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException {
		log.debug("filterList({}, {}, {})", new Object[] { userId, request, response });
		ServletContext sc = getServletContext();
		int maId = WebUtils.getInt(request, "ma_id");
		String ma_user = WebUtils.getString(request, "ma_user");
		sc.setAttribute("ma_id", maId);
		sc.setAttribute("ma_user", ma_user);
		MailAccount ma = MailAccountDAO.findByPk(maId);
		sc.setAttribute("mailFilters", ma.getMailFilters());
		sc.getRequestDispatcher("/admin/mail_filter_list.jsp").forward(request, response);
		log.debug("filterList: void");
	}
	
	/**
	 * Create mail filter
	 */
	private void filterCreate(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, PathNotFoundException, DatabaseException {
		log.debug("filterCreate({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int maId = WebUtils.getInt(request, "ma_id");
			String path = WebUtils.getString(request, "mf_path");
			String uuid = NodeBaseDAO.getInstance().getUuidFromPath(path);
			// String uuid = JCRUtils.getUUID(userId, mf.getPath());
			
			MailFilter mf = new MailFilter();
			mf.setPath(path);
			mf.setNode(uuid);
			mf.setGrouping(WebUtils.getBoolean(request, "mf_grouping"));
			mf.setActive(WebUtils.getBoolean(request, "mf_active"));
			MailAccount ma = MailAccountDAO.findByPk(maId);
			ma.getMailFilters().add(mf);
			MailAccountDAO.update(ma);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_CREATE", Long.toString(ma.getId()), null, mf.toString());
		} else {
			ServletContext sc = getServletContext();
			MailFilter mf = new MailFilter();
			mf.setPath("/"+Repository.ROOT);
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("mf", mf);
			sc.getRequestDispatcher("/admin/mail_filter_edit.jsp").forward(request, response);
		}
		
		log.debug("filterCreate: void");
	}
	
	/**
	 * Edit mail filter
	 */
	private void filterEdit(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, PathNotFoundException, DatabaseException {
		log.debug("filterEdit({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int mfId = WebUtils.getInt(request, "mf_id");
			String path = WebUtils.getString(request, "mf_path");
			String uuid = NodeBaseDAO.getInstance().getUuidFromPath(path);
			// String uuid = JCRUtils.getUUID(userId, mf.getPath());
			MailFilter mf = MailAccountDAO.findFilterByPk(mfId);
			
			if (mf != null) {
				mf.setPath(path);
				mf.setNode(uuid);
				mf.setGrouping(WebUtils.getBoolean(request, "mf_grouping"));
				mf.setActive(WebUtils.getBoolean(request, "mf_active"));
				MailAccountDAO.updateFilter(mf);
			}
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_EDIT", Long.toString(mf.getId()), null, mf.toString());
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			int mfId = WebUtils.getInt(request, "mf_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma_id", maId);
			sc.setAttribute("mf", MailAccountDAO.findFilterByPk(mfId));
			sc.getRequestDispatcher("/admin/mail_filter_edit.jsp").forward(request, response);
		}
		
		log.debug("filterEdit: void");
	}
	
	/**
	 * Delete filter rule
	 */
	private void filterDelete(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, PathNotFoundException, DatabaseException {
		log.debug("filterDelete({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int maId = WebUtils.getInt(request, "ma_id");
			int mfId = WebUtils.getInt(request, "mf_id");
			MailAccountDAO.deleteFilter(mfId);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_DELETE", Integer.toString(maId), null, null);
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			int mfId = WebUtils.getInt(request, "mf_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma_id", maId);
			sc.setAttribute("mf", MailAccountDAO.findFilterByPk(mfId));
			sc.getRequestDispatcher("/admin/mail_filter_edit.jsp").forward(request, response);
		}
		
		log.debug("filterDelete: void");
	}
	
	/**
	 * List filter rules
	 */
	private void ruleList(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException {
		log.debug("ruleList({}, {}, {})", new Object[] { userId, request, response });
		ServletContext sc = getServletContext();
		int maId = WebUtils.getInt(request, "ma_id");
		int mfId = WebUtils.getInt(request, "mf_id");
		sc.setAttribute("ma_id", maId);
		sc.setAttribute("mf_id", mfId);
		MailAccount ma = MailAccountDAO.findByPk(maId);
		
		for (MailFilter mf : ma.getMailFilters()) {
			if (mf.getId() == mfId) {
				sc.setAttribute("filterRules", mf.getFilterRules());
			}
		}
		
		sc.getRequestDispatcher("/admin/mail_filter_rule_list.jsp").forward(request, response);
		log.debug("ruleList: void");
	}
	
	/**
	 * Create filter rule
	 */
	private void ruleCreate(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, PathNotFoundException, DatabaseException {
		log.debug("ruleCreate({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int mf_id = WebUtils.getInt(request, "mf_id");
			MailFilterRule mfr = new MailFilterRule();
			mfr.setField(WebUtils.getString(request, "mfr_field"));
			mfr.setOperation(WebUtils.getString(request, "mfr_operation"));
			mfr.setValue(WebUtils.getString(request, "mfr_value"));
			mfr.setActive(WebUtils.getBoolean(request, "mfr_active"));
			MailFilter mf = MailAccountDAO.findFilterByPk(mf_id);
			mf.getFilterRules().add(mfr);
			MailAccountDAO.updateFilter(mf);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_RULE_CREATE", Long.toString(mf.getId()), null, mf.toString());
		} else {
			ServletContext sc = getServletContext();
			MailFilterRule mfr = new MailFilterRule();
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("mfr", mfr);
			sc.setAttribute("fields", fields);
			sc.setAttribute("operations", operations);
			sc.getRequestDispatcher("/admin/mail_filter_rule_edit.jsp").forward(request, response);
		}
		
		log.debug("ruleCreate: void");
	}
	
	/**
	 * Edit filter rule
	 */
	private void ruleEdit(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException {
		log.debug("ruleEdit({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int mfrId = WebUtils.getInt(request, "mfr_id");
			MailFilterRule mfr = MailAccountDAO.findRuleByPk(mfrId);
			
			if (mfr != null) {
				mfr.setField(WebUtils.getString(request, "mfr_field"));
				mfr.setOperation(WebUtils.getString(request, "mfr_operation"));
				mfr.setValue(WebUtils.getString(request, "mfr_value"));
				mfr.setActive(WebUtils.getBoolean(request, "mfr_active"));
				MailAccountDAO.updateRule(mfr);
			}
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_RULE_EDIT", Long.toString(mfr.getId()), null, mfr.toString());
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			int mfId = WebUtils.getInt(request, "mf_id");
			int mfrId = WebUtils.getInt(request, "mfr_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma_id", maId);
			sc.setAttribute("mf_id", mfId);
			sc.setAttribute("mfr", findRuleByPk(mfrId));
			sc.setAttribute("fields", fields);
			sc.setAttribute("operations", operations);
			sc.getRequestDispatcher("/admin/mail_filter_rule_edit.jsp").forward(request, response);
		}
		
		log.debug("ruleEdit: void");
	}
	
	/**
	 * Delete filter rule
	 */
	private void ruleDelete(String userId, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DatabaseException {
		log.debug("ruleDelete({}, {}, {})", new Object[] { userId, request, response });
		
		if (WebUtils.getBoolean(request, "persist")) {
			int mfrId = WebUtils.getInt(request, "mfr_id");
			MailAccountDAO.deleteRule(mfrId);
			
			// Activity log
			UserActivity.log(userId, "ADMIN_MAIL_FILTER_RULE_DELETE", Integer.toString(mfrId), null, null);
		} else {
			ServletContext sc = getServletContext();
			int maId = WebUtils.getInt(request, "ma_id");
			int mfId = WebUtils.getInt(request, "mf_id");
			int mfrId = WebUtils.getInt(request, "mfr_id");
			sc.setAttribute("action", WebUtils.getString(request, "action"));
			sc.setAttribute("persist", true);
			sc.setAttribute("ma_id", maId);
			sc.setAttribute("mf_id", mfId);
			sc.setAttribute("mfr", MailAccountDAO.findRuleByPk(mfrId));
			sc.setAttribute("fields", fields);
			sc.setAttribute("operations", operations);
			sc.getRequestDispatcher("/admin/mail_filter_rule_edit.jsp").forward(request, response);
		}
		
		log.debug("ruleDelete: void");
	}
}
