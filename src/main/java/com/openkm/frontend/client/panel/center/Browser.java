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

package com.openkm.frontend.client.panel.center;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.constants.ui.UIDockPanelConstants;
import com.openkm.frontend.client.util.TimeHelper;
import com.openkm.frontend.client.util.Util;
import com.openkm.frontend.client.widget.filebrowser.FileBrowser;
import com.openkm.frontend.client.widget.properties.TabMultiple;

/**
 * Browser panel
 * 
 * @author jllort
 *
 */
public class Browser extends Composite {
	
	private final static int IE_SIZE_RECTIFICATION = (Util.getUserAgent().startsWith("ie")?2:0);
	private final static int SPLITTER_HEIGHT = 10;
	private final static int REFRESH_WAITING_TIME = 100;
	private final static String TIME_HELPER_KEY = "SPLIT_VERTICAL_DESKTOP";
	
	private VerticalSplitPanelExtended verticalSplitPanel;
	private VerticalPanel tabPropertiesPanel;
	
	public FileBrowser fileBrowser;
	public TabMultiple tabMultiple;
	
	private boolean isResizeInProgress = false;
	private boolean finalResizeInProgess = false;
	public int width = 0;
	public int height = 0;
	public int topHeight = 0;
	public int bottomHeight = 0;
	private boolean loadFinish = false;
	
	/**
	 * Browser
	 */
	@SuppressWarnings("deprecation")
	public Browser() {
		verticalSplitPanel = new VerticalSplitPanelExtended();
		fileBrowser = new FileBrowser();
		tabPropertiesPanel = new VerticalPanel();
		tabMultiple = new TabMultiple();
		
		tabPropertiesPanel.add(tabMultiple);
		tabPropertiesPanel.setStyleName("okm-Properties-Tab");

		verticalSplitPanel.getSplitPanel().setTopWidget(fileBrowser);
		verticalSplitPanel.getSplitPanel().setBottomWidget(tabPropertiesPanel);
		
		verticalSplitPanel.addMouseMoveHandler(new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (verticalSplitPanel.getSplitPanel().isResizing()) {
					if (!isResizeInProgress) {
						isResizeInProgress = true;
						onSplitResize();
					}
				}
			}
		});
		
		verticalSplitPanel.addMouseUpHandler(new MouseUpHandler() {
			@Override
			public void onMouseUp(MouseUpEvent event) {
				if (isResizeInProgress) {
					isResizeInProgress = false;
				}
			}
		});
		
		initWidget(verticalSplitPanel);
	}
	
	/**
	 * onSplitResize
	 */
	public void onSplitResize() {
		final int resizeUpdatePeriod = 20; // ms ( Internally splitter is refreshing each 20 ms )
		if (isResizeInProgress) {
			new Timer() {
				@Override
				public void run() {
					resizePanels(); // Always making resize
					if (isResizeInProgress) {
						onSplitResize();
					} else if (Util.getUserAgent().equals("chrome")) {
						resizePanels();
					}
				}
			}.schedule(resizeUpdatePeriod);
		}
	}
	
	/**
	 * Refresh language values
	 */
	public void langRefresh() {
		fileBrowser.langRefresh();	
		tabMultiple.langRefresh();
	}
	
	/**
	 * Sets the size on initialization
	 * 
	 * @param width The max width of the widget
	 * @param height The max height of the widget
	 */
	@SuppressWarnings("deprecation")
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		topHeight = (height-SPLITTER_HEIGHT)/2;
		bottomHeight = height - (topHeight + SPLITTER_HEIGHT);
		verticalSplitPanel.setPixelSize(width, height);
		verticalSplitPanel.getSplitPanel().setSplitPosition(""+topHeight);		
		resize();
		// Solve some problems with chrome
		if (loadFinish && Util.getUserAgent().equals("chrome") && 
			Main.get().mainPanel.topPanel.tabWorkspace.getSelectedWorkspace() == UIDockPanelConstants.DESKTOP) {
			resizePanels();
		}
	}
	
	/**
	 * resize
	 */
	private void resize() {
		verticalSplitPanel.setWidth(""+width);
		fileBrowser.setPixelSize(width, topHeight);
		int fileBrowserWidth = width-2+IE_SIZE_RECTIFICATION;
		int fileBrowserHeight = topHeight-2-FileBrowser.STATUS_SIZE+IE_SIZE_RECTIFICATION;
		if (fileBrowserWidth<0) {
			fileBrowserWidth = 0;
		}
		if (fileBrowserHeight<0) {
			fileBrowserHeight = 0;
		}
		// Resize the scroll panel on filebrowser
		// We substrat 2 pixels for fileBrowser pixels on width and the status fixed sixe on height 
		fileBrowser.table.setPixelSize(fileBrowserWidth, fileBrowserHeight);
		fileBrowser.table.fillWidth();
		// Resize the scroll panel on tab properties 
		// We substract 2 pixels for width and heigh generated by border line
		int tabMultipleWidth = width-2;
		int tabMultipleHeight = bottomHeight-2;
		if (tabMultipleWidth<0) {
			tabMultipleWidth =0;
		}
		if (tabMultipleHeight<0) {
			tabMultipleHeight = 0;
		}
		tabMultiple.setPixelSize(tabMultipleWidth,tabMultipleHeight);
		tabPropertiesPanel.setPixelSize(width, bottomHeight);
	}
	
	/**
	 * setWidth
	 * 
	 * @param width
	 */
	public void setWidth(int width) {
		this.width = width;
	
		resize();
		// Solve some problems with chrome
		if (loadFinish && Util.getUserAgent().equals("chrome") && 
			Main.get().mainPanel.topPanel.tabWorkspace.getSelectedWorkspace() == UIDockPanelConstants.DESKTOP) {
			resizePanels();
		}
	}
	
	/**
	 * Sets the panel width on resizing
	 */
	private void resizePanels() {
		int total = verticalSplitPanel.getOffsetHeight();
		
		String valHeight = DOM.getStyleAttribute(DOM.getChild(DOM.getChild(verticalSplitPanel.getSplitPanel().getElement(),0), 0), "height");
		if (valHeight.contains("px")) { valHeight = valHeight.substring(0, valHeight.indexOf("px")); }
		topHeight = Integer.parseInt(valHeight);
		
		String valTop = DOM.getStyleAttribute(DOM.getChild(DOM.getChild(verticalSplitPanel.getSplitPanel().getElement(),0), 2), "top");
		if (valTop.contains("px")) { valTop = valTop.substring(0, valTop.indexOf("px")); }
		bottomHeight = total - Integer.parseInt(valTop);		
		
		resize();
		
		if (Util.getUserAgent().equals("chrome")) {
			if (!TimeHelper.hasControlTime(TIME_HELPER_KEY)) {
				TimeHelper.hasElapsedEnoughtTime(TIME_HELPER_KEY, REFRESH_WAITING_TIME);
				timeControl();
			} else {
				TimeHelper.changeControlTime(TIME_HELPER_KEY);
			}
		}
	}
	
	/**
	 * timeControl
	 */
	private void timeControl() {
		if (TimeHelper.hasElapsedEnoughtTime(TIME_HELPER_KEY, REFRESH_WAITING_TIME)) {	
			if (!finalResizeInProgess) {
				finalResizeInProgess = true;
				final int tmpHeight = topHeight;
				final int tmpbBottomHeight = bottomHeight;
				final int tmpWidth = width;
				
				// Solve some problems with chrome
				if (Util.getUserAgent().equals("chrome")) {
					if (topHeight - 20 > 0) {
						topHeight -= 20;
					} else {
						topHeight = 0;
					}
					if (bottomHeight - 20 > 0) {
						bottomHeight -= 20;
					} else {
						bottomHeight = 0;
					}
					if (width - 20 > 0) {
						width -= 20;
					} else {
						width = 0;
					}
					resize();
				}
				
				new Timer() {
					@Override
					public void run() {
						topHeight = tmpHeight;
						bottomHeight = tmpbBottomHeight;
						width = tmpWidth;
						resize();
						TimeHelper.removeControlTime(TIME_HELPER_KEY);
						finalResizeInProgess = false;
					}
				}.schedule(50);
			}
		} else {
			new Timer() {
				@Override
				public void run() {
					timeControl();
				}
			}.schedule(50);
		}
	}
	
	/**
	 * refreshSpliterAfterAdded
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void refreshSpliterAfterAdded() {
		verticalSplitPanel.getSplitPanel().setSplitPosition(""+topHeight);
		
		if (Util.getUserAgent().equals("chrome")) {
			resizePanels();
		}
	}
	
	/**
	 * setLoadFinish
	 */
	public void setLoadFinish() {
		loadFinish = true;
	}
}