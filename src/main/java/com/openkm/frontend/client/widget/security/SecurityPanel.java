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

package com.openkm.frontend.client.widget.security;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.openkm.frontend.client.Main;

/**
 * SecurityPanel
 * 
 * @author jllort
 *
 */
public class SecurityPanel extends Composite {
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_USERS 	= 0;
	private static final int TAB_GROUPS = 1;
	
	public TabLayoutPanel tabPanel;
	private VerticalPanel vPanel;
	private HorizontalPanel filterPanel;
	private CheckBox checkBoxFilter;
	private TextBox filter;
	private HTML filterText;
	private String usersFilter = "";
	private String groupsFilter = "";
	public SecurityUser securityUser;
	public SecurityRole securityRole;
	private boolean filterView = false;
	private int width = 612;
	
	/**
	 * SecurityPanel
	 */
	public SecurityPanel() {
		vPanel = new VerticalPanel();
		securityUser = new SecurityUser();
		securityRole = new SecurityRole();
		tabPanel =  new TabLayoutPanel(TAB_HEIGHT, Unit.PX);
		tabPanel.add(securityUser, Main.i18n("security.users"));
		tabPanel.add(securityRole, Main.i18n("security.roles"));
		tabPanel.selectTab(TAB_USERS);
		tabPanel.setWidth(String.valueOf(width));
		tabPanel.setHeight("385"); // 365 +20
		
		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				Timer timer;
				switch (event.getSelectedItem().intValue()) {
					case TAB_USERS:
						groupsFilter = filter.getText();
						filter.setText(usersFilter);
						filterText.setHTML(Main.i18n("security.filter.by.users"));
						timer = new Timer() {
							@Override
							public void run() {
								securityUser.fillWidth();
							}
						};
						timer.schedule(50); // Fill width must be done after really it'll be visible
						break;
					case TAB_GROUPS:
						usersFilter = filter.getText();
						filter.setText(groupsFilter);
						filterText.setHTML(Main.i18n("security.filter.by.roles"));
						timer = new Timer() {
							@Override
							public void run() {
								securityRole.fillWidth();
							}
						};
						timer.schedule(50); // Fill width must be done after really it'll be visible
						break;
				}
			}
		});
		
		filterPanel = new HorizontalPanel();
		filterPanel.setVisible(false);
		checkBoxFilter = new CheckBox();
		checkBoxFilter.setValue(false);
		checkBoxFilter.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				securityUser.resetUnassigned();
				securityRole.resetUnassigned();
				Widget sender = (Widget) event.getSource();
				if (((CheckBox) sender).getValue()) {
					filter.setText("");
					filter.setEnabled(true);
				} else {
					filter.setText("");
					filter.setEnabled(false);
					usersFilter = "";
					groupsFilter = "";
					refreshUnassigned();
				}
			}
		});
		filter = new TextBox();
		filterText = new HTML(Main.i18n("security.filter.by.users"));
		filterPanel.add(checkBoxFilter);
		filterPanel.add(new HTML("&nbsp;"));
		filterPanel.add(filterText);
		filterPanel.add(new HTML("&nbsp;"));
		filterPanel.add(filter);
		filterPanel.add(new HTML("&nbsp;"));
		
		filterPanel.setCellVerticalAlignment(checkBoxFilter, HasAlignment.ALIGN_MIDDLE);
		filterPanel.setCellVerticalAlignment(filterText, HasAlignment.ALIGN_MIDDLE);
		filterPanel.setCellVerticalAlignment(filter, HasAlignment.ALIGN_MIDDLE);
		
		filter.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (filter.getText().length()>=3) {
					int selected = tabPanel.getSelectedIndex();
					switch(selected) {
						case TAB_USERS:
							securityUser.getFilteredUngrantedUsers(filter.getText());
							break;
							
						case TAB_GROUPS:
							securityRole.getFilteredUngrantedRoles(filter.getText());
							break;
					}
				} else {
					securityUser.resetUnassigned();
					securityRole.resetUnassigned();
				}
			}
		});
		
		vPanel.add(filterPanel);
		vPanel.add(tabPanel);
		
		vPanel.setCellHorizontalAlignment(filterPanel, VerticalPanel.ALIGN_RIGHT);
		
		vPanel.addStyleName("okm-DisableSelect");
		tabPanel.addStyleName("okm-Border-Bottom");
		filter.setStyleName("okm-Input");
		
		tabPanel.setWidth(String.valueOf(width));
		
		initWidget(vPanel);
	}
	
	/**
	 * reset
	 * 
	 * @param path
	 */
	public void reset(String path) {
		securityUser.setPath(path);
		securityRole.setPath(path);
		securityUser.reset();
		securityRole.reset();
		filter.setText("");
		usersFilter = "";
		groupsFilter = "";
		securityUser.getGrantedUsers();
		securityRole.getGrantedRoles();
		if (!filterView) {
			securityUser.getUngrantedUsers();
			securityRole.getUngrantedRoles();
		}
	}
	
	/**
	 * refreshUnassigned
	 */
	public void refreshUnassigned() {
		securityUser.getUngrantedUsers();
		securityRole.getUngrantedRoles();
	}
	
	/**
	 * enableAdvancedFilter
	 */
	public void enableAdvancedFilter() {
		filterView = true;
		filterPanel.setVisible(true);
		checkBoxFilter.setValue(true);
	}
	
	/**
	 * fillWidth
	 */
	public void fillWidth() {
		securityUser.fillWidth();
		securityRole.fillWidth();
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		int selected = tabPanel.getSelectedIndex();
		
		while (tabPanel.getWidgetCount() > 0) {
			tabPanel.remove(0);
		}
		
		tabPanel.add(securityUser, Main.i18n("security.users"));
		tabPanel.add(securityRole, Main.i18n("security.roles"));
		tabPanel.selectTab(selected);
		
		switch (selected) {
			case TAB_USERS:
				filterText.setHTML(Main.i18n("security.filter.by.users"));
				break;
			case TAB_GROUPS:
				filterText.setHTML(Main.i18n("security.filter.by.roles"));
				break;
		}
		
		securityUser.langRefresh();
		securityRole.langRefresh();
	}
	
	/**
	 * evaluateChangeButton
	 */
	public void evaluateChangeButton() {
		Main.get().securityPopup.enableChangeButton(securityUser.getNewGrants().size()>0 || securityRole.getNewGrants().size()>0);
	}
}