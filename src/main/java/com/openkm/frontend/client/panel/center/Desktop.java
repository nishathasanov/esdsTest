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

package com.openkm.frontend.client.panel.center;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.constants.ui.UIDockPanelConstants;
import com.openkm.frontend.client.panel.left.Navigator;
import com.openkm.frontend.client.util.TimeHelper;
import com.openkm.frontend.client.util.Util;

/**
 * Administration
 * 
 * @author jllort
 *
 */
public class Desktop extends Composite {
	
	private final static int PANEL_LEFT_WIDTH = 225;
	public final static int SPLITTER_WIDTH = 10;
	private final static int REFRESH_WAITING_TIME = 100;
	private final static String TIME_HELPER_KEY = "SPLIT_HORIZONTAL_DESKTOP";
	
	private HorizontalSplitPanelExtended horizontalSplitPanel;
	public Navigator navigator;
	public Browser browser;
	private boolean isResizeInProgress = false;
	private boolean finalResizeInProgess = false;
	private int width = 0;
	private int height = 0; 
	private int left = PANEL_LEFT_WIDTH;
	private int right = 0;
	private boolean loadFinish = false;
	
	/**
	 * Desktop
	 */
	@SuppressWarnings("deprecation")
	public Desktop() {
		horizontalSplitPanel = new HorizontalSplitPanelExtended();
		navigator = new Navigator();
		browser = new Browser();
		
		horizontalSplitPanel.getSplitPanel().setLeftWidget(navigator);
		horizontalSplitPanel.getSplitPanel().setRightWidget(browser);
		horizontalSplitPanel.getSplitPanel().setSplitPosition(""+PANEL_LEFT_WIDTH);
		
		horizontalSplitPanel.addMouseMoveHandler(new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (horizontalSplitPanel.getSplitPanel().isResizing()) {
					if (!isResizeInProgress) {
						isResizeInProgress = true;
						onSplitResize();
					}
				}
			}
		});
		
		horizontalSplitPanel.addMouseUpHandler(new MouseUpHandler() {
			@Override
			public void onMouseUp(MouseUpEvent event) {
				if (isResizeInProgress) {
					isResizeInProgress = false;
				}
			}
		});
		
		initWidget(horizontalSplitPanel);
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
		left = (int)(width * 0.2);
		left = left < PANEL_LEFT_WIDTH ? PANEL_LEFT_WIDTH : left;
		right = width - (left + SPLITTER_WIDTH);
		
		if (right < 0) {
			right = 0;
		}
		
		horizontalSplitPanel.setPixelSize(width, height);
		navigator.setSize(left, height);
		browser.setSize(right, height);
		horizontalSplitPanel.getSplitPanel().setSplitPosition(""+left);
		
		// Solve some problems with chrome
		if (loadFinish && Util.getUserAgent().equals("chrome") && 
			Main.get().mainPanel.topPanel.tabWorkspace.getSelectedWorkspace() == UIDockPanelConstants.DESKTOP) {
			resizePanels();
		}
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
					} else {
						// On finishing in good idea to fill width column tables
						browser.fileBrowser.table.fillWidth();
						
						// Solve some problems with chrome
						if (Util.getUserAgent().equals("chrome")) {
							resizePanels();
						}
					}
				}
			}.schedule(resizeUpdatePeriod);
		}
	}
	
	/**
	 * Sets the panel width on resizing
	 */
	private void resizePanels() {
		int total = horizontalSplitPanel.getOffsetWidth();
		
		String valWidth = DOM.getStyleAttribute(DOM.getChild(DOM.getChild(horizontalSplitPanel.getSplitPanel().getElement(), 0), 0), "width");
		if (valWidth.contains("px")) { valWidth = valWidth.substring(0, valWidth.indexOf("px")); }
		left = Integer.parseInt(valWidth);
		
		String valLeft = DOM.getStyleAttribute(DOM.getChild(DOM.getChild(horizontalSplitPanel.getSplitPanel().getElement(), 0), 2), "left");
		if (valLeft.contains("px")) { valLeft = valLeft.substring(0, valLeft.indexOf("px")); }
		right = total - Integer.parseInt(valLeft);
		
		navigator.setSize(left, height);
		if (right > 0) { browser.setWidth(right); }
		
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
				int total = horizontalSplitPanel.getOffsetWidth();
				String value = DOM.getStyleAttribute (DOM.getChild(DOM.getChild(horizontalSplitPanel.getSplitPanel().getElement(),0), 0), "width");
				if (value.contains("px")) { value = value.substring(0,value.indexOf("px")); }
				left = Integer.parseInt(value);
				value = DOM.getStyleAttribute (DOM.getChild(DOM.getChild(horizontalSplitPanel.getSplitPanel().getElement(),0), 2), "left");
				if (value.contains("px")) { value = value.substring(0,value.indexOf("px")); }
				right = total - Integer.parseInt(value);
				
				// Solve some problems with chrome
				if (Util.getUserAgent().equals("chrome")) {
					int tmpLeft = left;
					int tmpHeight = height;
					int tmpRight = right;
					if (tmpLeft - 20 > 0) {
						tmpLeft -= 20;
					} else {
						tmpLeft = 0;
					}
					if (tmpHeight - 20 > 0) {
						tmpHeight -= 20;
					} else {
						tmpHeight = 0;
					}
					if (tmpRight - 20 > 0) {
						tmpRight -= 20;
					} else {
						tmpRight = 0;
					}
					navigator.setSize(tmpLeft, tmpHeight);
					browser.setWidth(tmpRight);
				}
				
				new Timer() {
					@Override
					public void run() {
						navigator.setSize(left, height);
						browser.setWidth(right);
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
	 */
	@SuppressWarnings("deprecation")
	public void refreshSpliterAfterAdded() {
		horizontalSplitPanel.getSplitPanel().setSplitPosition(""+left);
		browser.refreshSpliterAfterAdded();
		
		// Solve some problems with chrome
		if (Util.getUserAgent().equals("chrome")) {
			resizePanels();
		}
	}
	
	/**
	 * setLoadFinish
	 */
	public void setLoadFinish() {
		loadFinish = true;
		browser.setLoadFinish();
	}
	
	/**
	 * getWidth
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * getHeight
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * getLeft
	 */
	public int getLeft() {
		return left;
	}

	/**
	 * getRight
	 */
	public int getRight() {
		return right;
	}
}