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

package com.openkm.module.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.google.gson.Gson;
import com.openkm.automation.AutomationException;
import com.openkm.automation.AutomationManager;
import com.openkm.automation.AutomationUtils;
import com.openkm.bean.PropertyGroup;
import com.openkm.bean.form.CheckBox;
import com.openkm.bean.form.FormElement;
import com.openkm.bean.form.Input;
import com.openkm.bean.form.Option;
import com.openkm.bean.form.Select;
import com.openkm.bean.form.SuggestBox;
import com.openkm.bean.form.TextArea;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.NoSuchPropertyException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.RegisteredPropertyGroupDAO;
import com.openkm.dao.bean.AutomationRule;
import com.openkm.dao.bean.RegisteredPropertyGroup;
import com.openkm.module.PropertyGroupModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.FormUtils;
import com.openkm.util.UserActivity;

public class DbPropertyGroupModule implements PropertyGroupModule {
	private static Logger log = LoggerFactory.getLogger(DbPropertyGroupModule.class);
	
	@Override
	public void addGroup(String token, String nodePath, String grpName) throws NoSuchGroupException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException, AutomationException {
		log.debug("addGroup({}, {}, {})", new Object[] { token, nodePath, grpName });
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			
			// AUTOMATION - PRE
			Map<String, Object> env = new HashMap<String, Object>();
			env.put(AutomationUtils.NODE_UUID, nodeUuid);
			env.put(AutomationUtils.NODE_PATH, nodePath);
			env.put(AutomationUtils.PROPERTY_GROUP_NAME, grpName);
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_PROPERTY_GROUP_ADD, AutomationRule.AT_PRE, env);
			
			NodeBaseDAO.getInstance().addPropertyGroup(nodeUuid, grpName);
			
			// AUTOMATION - POST
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_PROPERTY_GROUP_ADD, AutomationRule.AT_POST, env);
			
			// Activity log
			UserActivity.log(auth.getName(), "ADD_PROPERTY_GROUP", nodeUuid, nodePath, grpName);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("addGroup: void");
	}
	
	@Override
	public void removeGroup(String token, String nodePath, String grpName) throws AccessDeniedException,
			NoSuchGroupException, LockException, PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("removeGroup({}, {}, {})", new Object[] { token, nodePath, grpName });
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			NodeBaseDAO.getInstance().removePropertyGroup(nodeUuid, grpName);
			
			// Activity log
			UserActivity.log(auth.getName(), "REMOVE_PROPERTY_GROUP", nodeUuid, nodePath, grpName);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("removeGroup: void");
	}
	
	@Override
	public List<PropertyGroup> getGroups(String token, String nodePath) throws IOException, ParseException,
			PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("getGroups({})", token);
		ArrayList<PropertyGroup> ret = new ArrayList<PropertyGroup>();
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			List<String> propGroups = NodeBaseDAO.getInstance().getPropertyGroups(nodeUuid);
			Map<PropertyGroup, List<FormElement>> pgf = FormUtils.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			
			// Only return registered property definitions
			for (String pgName : propGroups) {
				for (PropertyGroup pg : pgf.keySet()) {
					if (pg.getName().equals(pgName)) {
						ret.add(pg);
					}
				}
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getGroups: {}", ret);
		return ret;
	}
	
	@Override
	public List<PropertyGroup> getAllGroups(String token) throws IOException, ParseException, RepositoryException,
			DatabaseException {
		log.debug("getAllGroups({})", token);
		ArrayList<PropertyGroup> ret = new ArrayList<PropertyGroup>();
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			Map<PropertyGroup, List<FormElement>> pgf = FormUtils.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			
			// Only return registered property definitions
			for (RegisteredPropertyGroup rpg : RegisteredPropertyGroupDAO.getInstance().findAll()) {
				for (PropertyGroup pg : pgf.keySet()) {
					if (pg.getName().equals(rpg.getName())) {
						ret.add(pg);
					}
				}
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getAllGroups: {}", ret);
		return ret;
	}
	
	@Override
	public List<FormElement> getProperties(String token, String nodePath, String grpName) throws IOException,
			ParseException, NoSuchGroupException, PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("getProperties({}, {}, {})", new Object[] { token, nodePath, grpName });
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			Map<PropertyGroup, List<FormElement>> pgfs = FormUtils.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			List<FormElement> pgf = FormUtils.getPropertyGroupForms(pgfs, grpName);
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			List<FormElement> nodeProperties = new ArrayList<FormElement>();
			
			if (pgf != null) {
				Map<String, String> properties = NodeBaseDAO.getInstance().getProperties(nodeUuid, grpName);
				Gson gson = new Gson();
				
				for (FormElement fe : pgf) {
					String value = properties.get(fe.getName());
					
					if (fe instanceof Input) {
						((Input) fe).setValue(value == null ? "" : value);
					} else if (fe instanceof SuggestBox) {
						((SuggestBox) fe).setValue(value == null ? "" : value);
					} else if (fe instanceof CheckBox) {
						((CheckBox) fe).setValue(Boolean.parseBoolean(value));
					} else if (fe instanceof TextArea) {
						((TextArea) fe).setValue(value == null ? "" : value);
					} else if (fe instanceof Select) {
						if (value != null) {
							String[] values = gson.fromJson(value, String[].class);
							Select select = ((Select) fe);
							
							if (select.getType().equals(Select.TYPE_SIMPLE) && values.length > 1) {
								throw new ParseException("Inconsistent property definition: " + fe.getName());
							} else {
								for (Option opt : select.getOptions()) {
									for (int j = 0; j < values.length; j++) {
										if (opt.getValue().equals(values[j])) {
											if (select.getType().equals(Select.TYPE_SIMPLE)) {
												select.setValue(opt.getValue());
											} else {
												select.setValue(select.getValue().concat(opt.getValue()).concat(Config.LIST_SEPARATOR));
											}
											
											opt.setSelected(true);
										} else {
											// opt.setSelected(false);
										}
									}
								}
								
								if (select.getValue().endsWith(Config.LIST_SEPARATOR)) {
									select.setValue(select.getValue().substring(0, select.getValue().length() - 1));
								}
							}
						}
					} else {
						throw new ParseException("Unknown property definition: " + fe.getName());
					}
					
					nodeProperties.add(fe);
				}
			} else {
				throw new NoSuchGroupException(grpName);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_PROPERTY_GROUP_PROPERTIES", nodeUuid, nodePath, grpName + ", "
					+ nodeProperties);
			
			log.debug("getProperties: {}", nodeProperties);
			return nodeProperties;
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	/**
	 * Convenient method for GWTUtil.getExtraColumn()
	 */
	public FormElement getProperty(String token, String nodePath, String grpName, String propName) throws IOException,
			ParseException, NoSuchGroupException, PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("getProperty({}, {}, {}, {})", new Object[] { token, nodePath, grpName, propName });
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			Map<PropertyGroup, List<FormElement>> pgfs = FormUtils.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			Map<String, FormElement> pgfMap = FormUtils.getPropertyGroupFormsMap(pgfs, grpName);
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			FormElement nodeProperty = null;
			
			if (pgfMap != null) {
				String value = NodeBaseDAO.getInstance().getProperty(nodeUuid, grpName, propName);
				FormElement fe = pgfMap.get(propName);
				Gson gson = new Gson();
				
				if (fe instanceof Input) {
					((Input) fe).setValue(value == null ? "" : value);
					nodeProperty = fe;
				} else if (fe instanceof SuggestBox) {
					((SuggestBox) fe).setValue(value == null ? "" : value);
					nodeProperty = fe;
				} else if (fe instanceof CheckBox) {
					((CheckBox) fe).setValue(Boolean.parseBoolean(value));
					nodeProperty = fe;
				} else if (fe instanceof TextArea) {
					((TextArea) fe).setValue(value == null ? "" : value);
					nodeProperty = fe;
				} else if (fe instanceof Select) {
					if (value != null) {
						String[] values = gson.fromJson(value, String[].class);
						Select select = ((Select) fe);
						
						if (select.getType().equals(Select.TYPE_SIMPLE) && values.length > 1) {
							throw new ParseException("Inconsistent property definition: " + fe.getName());
						} else {
							for (Option opt : select.getOptions()) {
								for (int j = 0; j < values.length; j++) {
									if (opt.getValue().equals(values[j])) {
										if (select.getType().equals(Select.TYPE_SIMPLE)) {
											select.setValue(opt.getValue());
										} else {
											select.setValue(select.getValue().concat(opt.getValue()).concat(Config.LIST_SEPARATOR));
										}
										
										opt.setSelected(true);
									} else {
										// opt.setSelected(false);
									}
								}
							}
							
							if (select.getValue().endsWith(Config.LIST_SEPARATOR)) {
								select.setValue(select.getValue().substring(0, select.getValue().length() - 1));
							}
						}
					}
					
					nodeProperty = fe;
				} else {
					throw new ParseException("Unknown property definition: " + fe.getName());
				}
			} else {
				throw new NoSuchGroupException(grpName);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_PROPERTY_GROUP_PROPERTY", nodeUuid, nodePath, grpName + ", "
					+ nodeProperty);
			
			log.debug("getProperty: {}", nodeProperty);
			return nodeProperty;
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	@Override
	public void setProperties(String token, String nodePath, String grpName, List<FormElement> properties)
			throws IOException, ParseException, NoSuchPropertyException, NoSuchGroupException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException, AutomationException {
		log.debug("setProperties({}, {}, {}, {})", new Object[] { token, nodePath, grpName, properties });
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			Map<String, String> nodProps = new HashMap<String, String>();
			Gson gson = new Gson();
			
			// Now we can safely set all property values.
			for (FormElement fe : properties) {
				if (fe instanceof Input) {
					nodProps.put(fe.getName(), ((Input) fe).getValue());
				} else if (fe instanceof SuggestBox) {
					nodProps.put(fe.getName(), ((SuggestBox) fe).getValue());
				} else if (fe instanceof CheckBox) {
					nodProps.put(fe.getName(), Boolean.toString(((CheckBox) fe).getValue()));
				} else if (fe instanceof TextArea) {
					nodProps.put(fe.getName(), ((TextArea) fe).getValue());
				} else if (fe instanceof Select) {
					List<String> tmp = new ArrayList<String>();
					
					for (Option opt : ((Select) fe).getOptions()) {
						if (opt.isSelected()) {
							tmp.add(opt.getValue());
						}
					}
					
					if (((Select) fe).getType().equals(Select.TYPE_SIMPLE) && tmp.size() > 1) {
						throw new ParseException("Inconsistent property definition: " + fe.getName());
					} else {
						String value = gson.toJson(tmp);
						nodProps.put(fe.getName(), value);
					}
				} else {
					log.warn("Unknown property definition: {}", fe.getName());
					throw new ParseException("Unknown property definition: " + fe.getName());
				}
			}
			
			// AUTOMATION - PRE
			Map<String, Object> env = new HashMap<String, Object>();
			env.put(AutomationUtils.NODE_UUID, nodeUuid);
			env.put(AutomationUtils.NODE_PATH, nodePath);
			env.put(AutomationUtils.PROPERTY_GROUP_NAME, grpName);
			env.put(AutomationUtils.PROPERTY_GROUP_PROPERTIES, nodProps);
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_PROPERTY_GROUP_SET, AutomationRule.AT_PRE, env);
			
			NodeBaseDAO.getInstance().setProperties(nodeUuid, grpName, nodProps);
			
			// AUTOMATION - POST
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_PROPERTY_GROUP_SET, AutomationRule.AT_POST, env);
			
			// Activity log
			UserActivity.log(auth.getName(), "SET_PROPERTY_GROUP_PROPERTIES", nodeUuid, nodePath, grpName + ", "
					+ properties);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("setProperties: void");
	}
	
	@Override
	public List<FormElement> getPropertyGroupForm(String token, String grpName) throws ParseException, IOException,
			RepositoryException, DatabaseException {
		log.debug("getPropertyGroupForm({}, {})", token, grpName);
		List<FormElement> ret = new ArrayList<FormElement>();
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			RegisteredPropertyGroup rpg = RegisteredPropertyGroupDAO.getInstance().findByPk(grpName);
			Map<PropertyGroup, List<FormElement>> pgf = FormUtils.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			List<FormElement> tmp = FormUtils.getPropertyGroupForms(pgf, grpName);
			
			// Only return registered property definitions
			for (FormElement fe : tmp) {
				for (String pgName : rpg.getProperties().keySet()) {
					if (fe.getName().equals(pgName)) {
						ret.add(fe);
					}
				}
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getPropertyGroupForm: {}", ret);
		return ret;
	}
	
	@Override
	public boolean hasGroup(String token, String nodePath, String grpName) throws IOException, ParseException,
			PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("hasGroup({}, {}, {})", new Object[] { token, nodePath, grpName });
		boolean ret = false;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			List<String> propGroups = NodeBaseDAO.getInstance().getPropertyGroups(nodeUuid);
			
			if (propGroups.contains(grpName)) {
				ret = true;
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("hasGroup: {}", ret);
		return ret;
	}
}
