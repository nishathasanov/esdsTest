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

package com.openkm.module.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.form.FormElement;
import com.openkm.bean.workflow.ProcessDefinition;
import com.openkm.bean.workflow.ProcessInstance;
import com.openkm.bean.workflow.TaskInstance;
import com.openkm.bean.workflow.Token;
import com.openkm.core.DatabaseException;
import com.openkm.core.ParseException;
import com.openkm.core.RepositoryException;
import com.openkm.core.WorkflowException;
import com.openkm.module.WorkflowModule;
import com.openkm.module.common.CommonWorkflowModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;
import com.openkm.util.UserActivity;

public class JcrWorkflowModule implements WorkflowModule {
	private static Logger log = LoggerFactory.getLogger(JcrWorkflowModule.class);

	@Override
	public void registerProcessDefinition(String token, InputStream is) throws ParseException, 
			RepositoryException, WorkflowException, DatabaseException, IOException {
		log.debug("registerProcessDefinition({}, {})", token, is);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.registerProcessDefinition(is);
			
			// Activity log
			UserActivity.log(session.getUserID(), "REGISTER_PROCESS_DEFINITION", null, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("registerProcessDefinition: void");
	}

	@Override
	public void deleteProcessDefinition(String token, long processDefinitionId) throws RepositoryException, 
			DatabaseException, WorkflowException {
		log.debug("deleteProcessDefinition({}, {})", token, processDefinitionId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.deleteProcessDefinition(processDefinitionId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "DELETE_PROCESS_DEFINITION", ""+processDefinitionId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
			
		}
		
		log.debug("deleteProcessDefinition: void");
	}

	@Override
	public ProcessDefinition getProcessDefinition(String token, long processDefinitionId) throws 
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("getProcessDefinition({}, {})", token, processDefinitionId);
		ProcessDefinition vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.getProcessDefinition(processDefinitionId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "GET_PROCESS_DEFINITION", ""+processDefinitionId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getProcessDefinition: {}", vo);
		return vo;
	}

	@Override
	public byte[] getProcessDefinitionImage(String token, long processDefinitionId, String node) throws 
			RepositoryException, DatabaseException, WorkflowException  {
		log.debug("getProcessDefinitionImage({}, {}, {})", new Object[] { token, processDefinitionId, node });
		byte[] image = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			image = CommonWorkflowModule.getProcessDefinitionImage(processDefinitionId, node);

			// Activity log
			UserActivity.log(session.getUserID(), "GET_PROCESS_DEFINITION_IMAGE", ""+processDefinitionId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getProcessDefinitionImage: {}", image);
		return image;
	}

	@Override
	public Map<String, List<FormElement>> getProcessDefinitionForms(String token, long processDefinitionId)
			throws ParseException, RepositoryException, DatabaseException, WorkflowException {
		log.debug("getProcessDefinitionForms({}, {})", token, processDefinitionId);
		Map<String, List<FormElement>> forms = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			forms = CommonWorkflowModule.getProcessDefinitionForms(processDefinitionId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "GET_PROCESS_DEFINITION_FORMS", processDefinitionId+"", null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getProcessDefinitionForms: {}", forms);
		return forms;
	}

	@Override
	public ProcessInstance runProcessDefinition(String token, long processDefinitionId, String uuid, 
			List<FormElement> variables) throws RepositoryException, DatabaseException, WorkflowException {
		log.debug("runProcessDefinition({}, {}, {})", new Object[] { token, processDefinitionId, variables });
		ProcessInstance vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.runProcessDefinition(session.getUserID(), processDefinitionId, uuid, variables);
			
			// Activity log
			UserActivity.log(session.getUserID(), "RUN_PROCESS_DEFINITION", ""+processDefinitionId, null, variables.toString());
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("runProcessDefinition: {}", vo);
		return vo;
	}

	@Override
	public ProcessInstance sendProcessInstanceSignal(String token, long processInstanceId, 
			String transitionName) throws RepositoryException, DatabaseException, WorkflowException {
		log.debug("sendProcessInstanceSignal({}, {}, {})", new Object[] { token, processInstanceId, transitionName });
		ProcessInstance vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.sendProcessInstanceSignal(processInstanceId, transitionName);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SEND_PROCESS_INSTANCE_SIGNAL", ""+processInstanceId, null, transitionName);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("sendProcessInstanceSignal: {}", vo);
		return vo;
	}

	@Override
	public void endProcessInstance(String token, long processInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("endProcessInstance({}, {})", token, processInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.endProcessInstance(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "END_PROCESS_INSTANCE", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("endProcessInstance: void");
	}
	
	@Override
	public void deleteProcessInstance(String token, long processInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("deleteProcessInstance({}, {})", token, processInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.deleteProcessInstance(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "DELETE_PROCESS_INSTANCE", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("deleteProcessInstance: void");
	}

	@Override
	public List<ProcessInstance> findProcessInstances(String token, long processDefinitionId) throws 
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("findProcessInstances({}, {})", token, processDefinitionId);
		List<ProcessInstance> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findProcessInstances(processDefinitionId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_PROCESS_INSTANCES", ""+processDefinitionId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findProcessInstances: {}", al);
		return al;
	}

	@Override
	public List<ProcessDefinition> findAllProcessDefinitions(String token) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("findAllProcessDefinitions({})", token);
		List<ProcessDefinition> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findAllProcessDefinitions();
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_ALL_PROCESS_DEFINITIONS", null, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findAllProcessDefinitions: {}", al);
		return al;
	}

	@Override
	public List<ProcessDefinition> findLatestProcessDefinitions(String token) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("findLatestProcessDefinitions({})", token);
		List<ProcessDefinition> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findLatestProcessDefinitions();
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_LATEST_PROCESS_DEFINITIONS", null, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findLatestProcessDefinitions: {}", al);
		return al;
	}
	
	@Override
	public ProcessDefinition findLastProcessDefinition(String token, String name) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("findLastProcessDefinition({}, {})", token, name);
		ProcessDefinition pd = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			pd = CommonWorkflowModule.findLastProcessDefinition(name);
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_LAST_PROCESS_DEFINITION", name, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findLatestProcessDefinitions: {}", pd);
		return pd;
	}

	@Override
	public List<ProcessDefinition> findAllProcessDefinitionVersions(String token, String name) throws 
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("findAllProcessDefinitionVersions({}, {})", token, name);
		List<ProcessDefinition> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findAllProcessDefinitionVersions(name);
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_ALL_PROCESS_DEFINITION_VERSIONS", name, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findAllProcessDefinitionVersions: {}", al);
		return al;
	}

	@Override
	public ProcessInstance getProcessInstance(String token, long processInstanceId) throws 
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("getProcessInstance({}, {})", token, processInstanceId);
		ProcessInstance vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.getProcessInstance(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "GET_PROCESS_INSTANCE", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getProcessInstance: {}", vo);
		return vo;
	}

	@Override
	public void suspendProcessInstance(String token, long processInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("suspendProcessInstance({}, {})", token, processInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.suspendProcessInstance(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SUSPEND_PROCESS_INSTANCE", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("suspendProcessInstance: void");
	}

	@Override
	public void resumeProcessInstance(String token, long processInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("resumeProcessInstance({}, {})", token, processInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.resumeProcessInstance(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "RESUME_PROCESS_INSTANCE", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("resumeProcessInstance: void");
	}

	@Override
	public void addProcessInstanceVariable(String token, long processInstanceId, String name, Object value)
			throws RepositoryException, DatabaseException, WorkflowException{
		log.debug("addProcessInstanceVariable({}, {}, {}, {})", new Object[] { token, processInstanceId, name, value });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.addProcessInstanceVariable(processInstanceId, name, value);
			
			// Activity log
			UserActivity.log(session.getUserID(), "ADD_PROCESS_INSTANCE_VARIABLE", ""+processInstanceId, null, name+", "+value);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("addProcessInstanceVariable: void");
	}

	@Override
	public void deleteProcessInstanceVariable(String token, long processInstanceId, String name) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("deleteProcessInstanceVariable({}, {}, {})", new Object[] { token, processInstanceId, name });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.deleteProcessInstanceVariable(processInstanceId, name);
			
			// Activity log
			UserActivity.log(session.getUserID(), "DELETE_PROCESS_INSTANCE_VARIABLE", ""+processInstanceId, null, name);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("deleteProcessInstanceVariable: void");
	}

	@Override
	public List<TaskInstance> findUserTaskInstances(String token) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("findUserTaskInstances({})", token);
		List<TaskInstance> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findUserTaskInstances(session.getUserID());
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_USER_TASK_INSTANCES", null, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findUserTaskInstances: {}", al);
		return al;
	}

	@Override
	public List<TaskInstance> findPooledTaskInstances(String token) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("findPooledTaskInstances({})", token);
		List<TaskInstance> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findPooledTaskInstances(session.getUserID());
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_POOLED_TASK_INSTANCES", null, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findPooledTaskInstances: {}", al);
		return al;
	}

	@Override
	public List<TaskInstance> findTaskInstances(String token, long processInstanceId) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("findTaskInstances({}, {})", token, processInstanceId);
		List<TaskInstance> al = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			al = CommonWorkflowModule.findTaskInstances(processInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "FIND_TASK_INSTANCES", ""+processInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("findTaskInstances: {}", al);
		return al;
	}

	@Override
	public void setTaskInstanceValues(String token, long taskInstanceId, String transitionName,
			List<FormElement> values) throws RepositoryException, DatabaseException, WorkflowException {
		log.debug("setTaskInstanceValues({}, {}, {}, {})", new Object[] { token, taskInstanceId, transitionName, values });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.setTaskInstanceValues(taskInstanceId, transitionName, values);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SET_TASK_INSTANCE_VALUES", ""+taskInstanceId, null, transitionName);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("setTaskInstanceValues: void");
	}

	@Override
	public void addTaskInstanceComment(String token, long taskInstanceId, String message) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("addTaskInstanceComment({}, {}, {})", new Object[] { token, taskInstanceId, message });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.addTaskInstanceComment(session.getUserID(), taskInstanceId, message);
			
			// Activity log
			UserActivity.log(session.getUserID(), "ADD_TASK_INSTANCE_COMMENT", ""+taskInstanceId, null, message);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("addTaskInstanceComment: void");
	}

	@Override
	public TaskInstance getTaskInstance(String token, long taskInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("getTaskInstance({}, {})", token, taskInstanceId);
		TaskInstance vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.getTaskInstance(taskInstanceId);
						
			// Activity log
			UserActivity.log(session.getUserID(), "GET_TASK_INSTANCE", ""+taskInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getTaskInstance: {}", vo);
		return vo;
	}

	@Override
	public void setTaskInstanceActorId(String token, long taskInstanceId, String actorId) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("setTaskInstanceActorId({}, {}, {})", new Object[] { token, taskInstanceId, actorId });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.setTaskInstanceActorId(taskInstanceId, actorId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SET_TASK_INSTANCE_ACTOR_ID", ""+taskInstanceId, null, actorId);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("setTaskInstanceActorId: void");
	}

	@Override
	public void addTaskInstanceVariable(String token, long taskInstanceId, String name, Object value) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("addTaskInstanceVariable({}, {}, {}, {})", new Object[] { token, taskInstanceId, name, value });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.addTaskInstanceVariable(taskInstanceId, name, value);
			
			// Activity log
			UserActivity.log(session.getUserID(), "ADD_TASK_INSTANCE_VARIABLE", ""+taskInstanceId, null, name+", "+value);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("addTaskInstanceVariable: void");
	}

	@Override
	public void deleteTaskInstanceVariable(String token, long taskInstanceId, String name) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("deleteTaskInstanceVariable({}, {}, {})", new Object[] { token, taskInstanceId, name });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.deleteTaskInstanceVariable(taskInstanceId, name);
			
			// Activity log
			UserActivity.log(session.getUserID(), "DELETE_TASK_INSTANCE_VARIABLE", ""+taskInstanceId, null, name);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("deleteTaskInstanceVariable: void");
	}

	@Override
	public void startTaskInstance(String token, long taskInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("startTaskInstance({}, {})", token, taskInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.startTaskInstance(taskInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "START_TASK_INSTANCE", ""+taskInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("startTaskInstance: void");
	}

	@Override
	public void endTaskInstance(String token, long taskInstanceId, String transitionName) throws
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("endTaskInstance({}, {}, {})", new Object[] { token, taskInstanceId, transitionName });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.endTaskInstance(taskInstanceId, transitionName);
			
			// Activity log
			UserActivity.log(session.getUserID(), "END_TASK_INSTANCE", ""+taskInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("endTaskInstance: void");
	}

	@Override
	public void suspendTaskInstance(String token, long taskInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("suspendTaskInstance({}, {})", token, taskInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.suspendTaskInstance(taskInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SUSPEND_TASK_INSTANCE", ""+taskInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("suspendTaskInstance: void");
	}

	@Override
	public void resumeTaskInstance(String token, long taskInstanceId) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("resumeTaskInstance({}, {})", token, taskInstanceId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.resumeTaskInstance(taskInstanceId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "RESUME_TASK_INSTANCE", ""+taskInstanceId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("resumeTaskInstance: void");
	}

	@Override
	public Token getToken(String token, long tokenId) throws RepositoryException, DatabaseException,
			WorkflowException {
		log.debug("getToken({}, {})", token, tokenId);
		Token vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			vo = CommonWorkflowModule.getToken(tokenId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "GET_TOKEN", ""+tokenId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getToken: "+vo);
		return vo;
	}

	@Override
	public void addTokenComment(String token, long tokenId, String message) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("addTokenComment({}, {}, {})", new Object[] { token, tokenId, message });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.addTokenComment(session.getUserID(), tokenId, message);
			
			// Activity log
			UserActivity.log(session.getUserID(), "ADD_TOKEN_COMMENT", ""+tokenId, null, message);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("addTokenComment: void");
	}

	@Override
	public void suspendToken(String token, long tokenId) throws RepositoryException, DatabaseException,
			WorkflowException {
		log.debug("suspendToken({}, {})", token, tokenId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.suspendToken(tokenId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SUSPEND_TOKEN", ""+tokenId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("suspendToken: void");
	}

	@Override
	public void resumeToken(String token, long tokenId) throws RepositoryException, DatabaseException,
			WorkflowException {
		log.debug("resumeToken({}, {})", token, tokenId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.resumeToken(tokenId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "RESUME_TOKEN", ""+tokenId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("resumeToken: void");
	}

	@Override
	public Token sendTokenSignal(String token, long tokenId, String transitionName) throws 
			RepositoryException, DatabaseException, WorkflowException {
		log.debug("sendTokenSignal({}, {}, {})", new Object[] { token, tokenId, transitionName });
		Token vo = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.sendTokenSignal(tokenId, transitionName);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SEND_TOKEN_SIGNAL", ""+tokenId, null, transitionName);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("sendTokenSignal: {}", vo);
		return vo;
	}

	@Override
	public void setTokenNode(String token, long tokenId, String nodeName) throws RepositoryException,
			DatabaseException, WorkflowException {
		log.debug("setTokenNode({}, {}, {})", new Object[] { token, tokenId, nodeName });
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.setTokenNode(tokenId, nodeName);
			
			// Activity log
			UserActivity.log(session.getUserID(), "SEND_TOKEN_NODE", ""+tokenId, null, nodeName);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("setTokenNode: void");
	}
	
	@Override
	public void endToken(String token, long tokenId) throws RepositoryException, DatabaseException,
			WorkflowException {
		log.debug("endToken({}, {})", token, tokenId);
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			CommonWorkflowModule.endToken(tokenId);
			
			// Activity log
			UserActivity.log(session.getUserID(), "END_TOKEN", ""+tokenId, null, null);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("endToken: void");
	}
}
