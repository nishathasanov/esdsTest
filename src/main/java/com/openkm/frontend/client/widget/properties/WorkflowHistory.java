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

package com.openkm.frontend.client.widget.properties;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * Notes
 * 
 * @author jllort
 */
public class WorkflowHistory extends Composite {
	private HorizontalPanel hPanel;
	private Frame iframe;
	private String Uuid;
	private int width = 0;
	private int height = 0;
	private boolean historyAvailable = true;
	
	public void setUuid(String Uuid) {
		this.Uuid = Uuid;
		showEmbedWorkflow(Uuid);
	}
	
	/**
	 * Preview
	 */
	public WorkflowHistory() {
		hPanel = new HorizontalPanel();
		iframe = new Frame();
		iframe.setSize("100%", "100%");
		hPanel.add(iframe);
		initWidget(hPanel);
	}
	
	@Override
	public void setPixelSize(int width, int height) {
		super.setPixelSize(width, height);
		this.width = width;
		this.height = height;
	}
	
	/**
	 * showEmbedSWF
	 * 
	 * @param Uuid Unique document ID to be previewed.
	 */
	public void showEmbedWorkflow(String Uuid) {
		
		if (historyAvailable) {
			iframe.setUrl("/ESDS/document_workflow_history.jsp?uuid=" + Uuid);
		} else {
			// TODO: display empty
		}
	}
	
	/**
	 * Sets the boolean value if previewing document is available
	 * 
	 * @param previewAvailable Set preview availability status.
	 */
	public void setPreviewAvailable(boolean previewAvailable) {
		
		this.historyAvailable = previewAvailable;
	}
	
	public int getWidth(){ return width; }
	public int getHeight(){ return height; }
	
}
