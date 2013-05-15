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

package com.openkm.frontend.client.bean;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GWTFileUploadResponse
 * 
 * @author jllort
 * 
 */
public class GWTFileUploadResponse implements IsSerializable {
	private boolean hasAutomation = false;
	private String path = "";
	private List<String> groupsList = new ArrayList<String>();
	private List<String> workflowList = new ArrayList<String>();
	private boolean showWizardCategories = false;
	private boolean showWizardKeywords = false;
	private boolean digitalSignature = false;
	private String error = "";
	
	/**
	 * GWTFileUploadResponse
	 * 
	 * @param text json encoded parameters.
	 */
	public GWTFileUploadResponse(String text) {
		text = text.substring(text.indexOf("{"));
		text = text.substring(0, text.lastIndexOf("}") + 1);
		JSONValue responseValue = JSONParser.parseStrict(text);
		JSONObject response = responseValue.isObject();
		
		// Deserialize information
		hasAutomation = response.get("hasAutomation").isBoolean().booleanValue();
		path = URL.decodeQueryString(response.get("path").isString().stringValue());
		error = response.get("error").isString().stringValue();
		showWizardCategories = response.get("showWizardCategories").isBoolean().booleanValue();
		showWizardKeywords = response.get("showWizardKeywords").isBoolean().booleanValue();
		digitalSignature = response.get("digitalSignature").isBoolean().booleanValue();
		
		// Getting property groups
		JSONArray groupsArray = response.get("groupsList").isArray();
		
		if (groupsArray != null) {
			for (int i=0; i<=groupsArray.size()-1; i++) {
				groupsList.add(groupsArray.get(i).isString().stringValue());
			}
		}
		
		// Getting workflows
		JSONArray workflowArray = response.get("workflowList").isArray();
		
		if (workflowArray != null) {
			for (int i=0; i<=workflowArray.size()-1; i++) {
				workflowList.add(workflowArray.get(i).isString().stringValue());
			}
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public List<String> getGroupsList() {
		return groupsList;
	}
	
	public void setGroupsList(List<String> groupsList) {
		this.groupsList = groupsList;
	}
	
	public List<String> getWorkflowList() {
		return workflowList;
	}
	
	public void setWorkflowList(List<String> workflowList) {
		this.workflowList = workflowList;
	}
	
	public boolean isShowWizardCategories() {
		return showWizardCategories;
	}
	
	public void setShowWizardCategories(boolean showWizardCategories) {
		this.showWizardCategories = showWizardCategories;
	}
	
	public boolean isShowWizardKeywords() {
		return showWizardKeywords;
	}
	
	public void setShowWizardKeywords(boolean showWizardKeywords) {
		this.showWizardKeywords = showWizardKeywords;
	}
	
	public boolean isHasAutomation() {
		return hasAutomation;
	}

	public void setHasAutomation(boolean hasAutomation) {
		this.hasAutomation = hasAutomation;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	public boolean isDigitalSignature() {
		return digitalSignature;
	}

	public void setDigitalSignature(boolean digitalSignature) {
		this.digitalSignature = digitalSignature;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("path=").append(path);
		sb.append(", showWizardCategories=").append(showWizardCategories);
		sb.append(", showWizardKeywords=").append(showWizardKeywords);
		sb.append(", groupsList=").append(groupsList);
		sb.append(", workflowList=").append(workflowList);
		sb.append(", hasAutomation=").append(hasAutomation);
		sb.append(", error=").append(error);
		sb.append(", digitalSignature=").append(digitalSignature);
		sb.append("}");
		return sb.toString();
	}
}