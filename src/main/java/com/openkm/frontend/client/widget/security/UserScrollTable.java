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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ResizePolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollPolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollTableImages;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTPermission;
import com.openkm.frontend.client.bean.GWTUser;
/**
 * UserScrollTable
 * 
 * @author jllort
 */
public class UserScrollTable extends Composite {
	public static final int PROPERTY_READ = 0;
	public static final int PROPERTY_WRITE = 1;
	public static final int PROPERTY_DELETE = 2;
	public static final int PROPERTY_SECURITY = 3;
	
	private ScrollTable table;
	private FixedWidthFlexTable headerTable;
	private FixedWidthGrid dataTable;
	private boolean isAssigned = false; // Determines if is assigned users table
										// or not
	private String path;
	private int flag_property;
	private int numberOfColumns = 0;
	private int width = 405;
	
	/**
	 * UserScrollTable
	 * 
	 * @param isAssigned
	 */
	public UserScrollTable(boolean isAssigned) {
		this.isAssigned = isAssigned;
		
		ScrollTableImages scrollTableImages = new ScrollTableImages() {
			public AbstractImagePrototype scrollTableAscending() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/sort_asc.gif");
					}
					
					public Image createImage() {
						return new Image("img/sort_asc.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/sort_asc.gif\"/>";
					}
				};
			}
			
			public AbstractImagePrototype scrollTableDescending() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/sort_desc.gif");
					}
					
					public Image createImage() {
						return new Image("img/sort_desc.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/sort_desc.gif\"/>";
					}
				};
			}
			
			public AbstractImagePrototype scrollTableFillWidth() {
				return new AbstractImagePrototype() {
					public void applyTo(Image image) {
						image.setUrl("img/fill_width.gif");
					}
					
					public Image createImage() {
						return new Image("img/fill_width.gif");
					}
					
					public String getHTML() {
						return "<img border=\"0\" src=\"img/fill_width.gif\"/>";
					}
				};
			}
		};
		
		headerTable = new FixedWidthFlexTable();
		dataTable = new FixedWidthGrid();
		
		table = new ScrollTable(dataTable, headerTable, scrollTableImages);
		table.setCellSpacing(0);
		table.setCellPadding(2);
		
		// Table data
		dataTable.setSelectionPolicy(SelectionGrid.SelectionPolicy.ONE_ROW);
		table.setResizePolicy(ResizePolicy.UNCONSTRAINED);
		table.setScrollPolicy(ScrollPolicy.BOTH);
		
		initSecurity();
		
		initWidget(table);
	}
	
	/**
	 * initSecurity
	 * 
	 */
	public void initSecurity() {
		// Level 1 headers
		int col = 0;
		if (isAssigned) {
			headerTable.setHTML(0, col, Main.i18n("security.user.name"));
			table.setColumnWidth(col++, 175);
			headerTable.setHTML(0, col, Main.i18n("security.user.permission.read"));
			table.setColumnWidth(col++, 55);
			headerTable.setHTML(0, col, Main.i18n("security.user.permission.write"));
			table.setColumnWidth(col++, 55);
			headerTable.setHTML(0, col, Main.i18n("security.user.permission.delete"));
			table.setColumnWidth(col++, 55);
			headerTable.setHTML(0, col, Main.i18n("security.user.permission.security"));
			table.setColumnWidth(col++, 55);
			headerTable.setHTML(0, col, ""); // Hidden user id
			table.setColumnWidth(col++, 0);
			numberOfColumns = col; // Number of columns
			table.setSize(String.valueOf(width), "365"); // Setting table size
		} else {
			table.setSize("185", "365");
			headerTable.setHTML(0, col, Main.i18n("security.user.name"));
			table.setColumnWidth(col++, 167);
			headerTable.setHTML(0, col, ""); // Hidden user id
			table.setColumnWidth(col++, 0);
			numberOfColumns = col;
		}
	}
	
	/**
	 * Lang refresh
	 */
	public void langRefresh() {
		int col = 0;
		if (isAssigned) {
			headerTable.setHTML(0, col++, Main.i18n("security.user.name"));
			headerTable.setHTML(0, col++, Main.i18n("security.user.permission.read"));
			headerTable.setHTML(0, col++, Main.i18n("security.user.permission.write"));
			headerTable.setHTML(0, col++, Main.i18n("security.user.permission.delete"));
			headerTable.setHTML(0, col++, Main.i18n("security.user.permission.security"));
		} else {
			headerTable.setHTML(0, col++, Main.i18n("security.user.name"));
		}
	}
	
	/**
	 * Adds new username permission row
	 * 
	 * @param userName The user name value
	 * @param permission The permission value
	 * @param modified If the permission has been modified
	 */
	public void addRow(GWTUser user, Integer permission, boolean modified) {
		final int rows = dataTable.getRowCount();
		dataTable.insertRow(rows);
		dataTable.setHTML(rows, 0, user.getUsername());
		
		if (modified) {
			dataTable.getCellFormatter().addStyleName(rows, 0, "bold");
		}
		
		CheckBox checkReadPermission = new CheckBox();
		CheckBox checkWritePermission = new CheckBox();
		CheckBox checkDeletePermission = new CheckBox();
		CheckBox checkSecurityPermission = new CheckBox();
		
		ClickHandler checkBoxReadListener = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				flag_property = PROPERTY_READ;
				Widget sender = (Widget) event.getSource();
				
				// Actions are inverse to check value because before user
				// perform check on checkbox
				// it has inverse value
				if (((CheckBox) sender).getValue()) {
					grant(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.READ,
							Main.get().securityPopup.recursive.getValue());
				} else {
					revoke(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.READ,
							Main.get().securityPopup.recursive.getValue());
				}
			}
		};
		
		ClickHandler checkBoxWriteListener = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				flag_property = PROPERTY_WRITE;
				Widget sender = (Widget) event.getSource();
				
				// Actions are inverse to check value because before user
				// perform check on checkbox
				// it has inverse value
				if (((CheckBox) sender).getValue()) {
					grant(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.WRITE,
							Main.get().securityPopup.recursive.getValue());
				} else {
					revoke(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.WRITE,
							Main.get().securityPopup.recursive.getValue());
				}
			}
		};
		
		ClickHandler checkBoxDeleteListener = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				flag_property = PROPERTY_DELETE;
				Widget sender = (Widget) event.getSource();
				
				// Actions are inverse to check value because before user
				// perform check on checkbox
				// it has inverse value
				if (((CheckBox) sender).getValue()) {
					grant(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.DELETE,
							Main.get().securityPopup.recursive.getValue());
				} else {
					revoke(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.DELETE,
							Main.get().securityPopup.recursive.getValue());
				}
			}
		};
		
		ClickHandler checkBoxSecurityListener = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				flag_property = PROPERTY_SECURITY;
				Widget sender = (Widget) event.getSource();
				
				// Actions are inverse to check value because before user
				// perform check on checkbox
				// it has inverse value
				if (((CheckBox) sender).getValue()) {
					grant(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.SECURITY,
							Main.get().securityPopup.recursive.getValue());
				} else {
					revoke(dataTable.getText(rows, numberOfColumns - 1), GWTPermission.SECURITY,
							Main.get().securityPopup.recursive.getValue());
				}
			}
		};
		
		checkReadPermission.addClickHandler(checkBoxReadListener);
		
		int col = 0;
		col++; // Name
		
		if ((permission & GWTPermission.READ) == GWTPermission.READ) {
			checkReadPermission.setValue(true);
			dataTable.setWidget(rows, col, checkReadPermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		} else {
			checkReadPermission.setValue(false);
			dataTable.setWidget(rows, col, checkReadPermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		}
		
		checkWritePermission.addClickHandler(checkBoxWriteListener);
		
		if ((permission & GWTPermission.WRITE) == GWTPermission.WRITE) {
			checkWritePermission.setValue(true);
			dataTable.setWidget(rows, col, checkWritePermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		} else {
			checkWritePermission.setValue(false);
			dataTable.setWidget(rows, col, checkWritePermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		}
		
		checkDeletePermission.addClickHandler(checkBoxDeleteListener);
		
		if ((permission & GWTPermission.DELETE) == GWTPermission.DELETE) {
			checkDeletePermission.setValue(true);
			dataTable.setWidget(rows, col, checkDeletePermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		} else {
			checkDeletePermission.setValue(false);
			dataTable.setWidget(rows, col, checkDeletePermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		}
		
		checkSecurityPermission.addClickHandler(checkBoxSecurityListener);
		
		if ((permission & GWTPermission.SECURITY) == GWTPermission.SECURITY) {
			checkSecurityPermission.setValue(true);
			dataTable.setWidget(rows, col, checkSecurityPermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		} else {
			checkSecurityPermission.setValue(false);
			dataTable.setWidget(rows, col, checkSecurityPermission);
			dataTable.getCellFormatter().setHorizontalAlignment(rows, col++, HasAlignment.ALIGN_CENTER);
		}
		
		dataTable.setHTML(rows, col, user.getId());
		dataTable.getCellFormatter().setVisible(rows, col++, false);
	}
	
	/**
	 * Adds new user name row
	 * 
	 * @param userName The user name value
	 * @param modified If the permission has been modified
	 */
	public void addRow(GWTUser user, boolean modified) {
		int rows = dataTable.getRowCount();
		int col = 0;
		dataTable.insertRow(rows);
		dataTable.setHTML(rows, col++, user.getUsername());
		
		if (modified) {
			dataTable.getCellFormatter().addStyleName(rows, 0, "bold");
		}
		
		dataTable.setHTML(rows, col, user.getId());
		dataTable.getCellFormatter().setVisible(rows, col++, false);
	}
	
	/**
	 * Selects the last row
	 */
	public void selectLastRow() {
		if (dataTable.getRowCount() > 0) {
			dataTable.selectRow(dataTable.getRowCount() - 1, true);
		}
	}
	
	/**
	 * Removes all rows except the first
	 */
	public void removeAllRows() {
		// Purge all rows
		while (dataTable.getRowCount() > 0) {
			dataTable.removeRow(0);
		}
		dataTable.resize(0, numberOfColumns);
	}
	
	/**
	 * Reset table values
	 */
	public void reset() {
		removeAllRows();
	}
	
	/**
	 * Gets the user
	 * 
	 * @return The user
	 */
	public GWTUser getUser() {
		if (!dataTable.getSelectedRows().isEmpty()) {
			int selectedRow = ((Integer) dataTable.getSelectedRows().iterator().next()).intValue();
			if (dataTable.isRowSelected(selectedRow)) {
				GWTUser user = new GWTUser();
				user.setId(dataTable.getHTML(((Integer) dataTable.getSelectedRows().iterator().next()).intValue(), numberOfColumns-1));
				user.setUsername(dataTable.getHTML(((Integer) dataTable.getSelectedRows().iterator().next()).intValue(), 0));
				return user;
			}
		}
		
		return null;
	}
	
	public int getSelectedRow() {
		if (!dataTable.getSelectedRows().isEmpty()) {
			int selectedRow = ((Integer) dataTable.getSelectedRows().iterator().next()).intValue();
			if (dataTable.isRowSelected(selectedRow)) {
				return selectedRow;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}
	
	/**
	 * Removes the selected row
	 */
	public void removeSelectedRow() {
		if (!dataTable.getSelectedRows().isEmpty()) {
			int selectedRow = ((Integer) dataTable.getSelectedRows().iterator().next()).intValue();
			dataTable.removeRow(selectedRow);
			
			if (dataTable.getRowCount() > 0) {
				if (dataTable.getRowCount() > selectedRow) {
					dataTable.selectRow(selectedRow, true);
				} else {
					dataTable.selectRow(selectedRow - 1, true);
				}
			}
		}
	}
	
	/**
	 * markModifiedSelectedRow
	 */
	public void markModifiedSelectedRow(boolean modified) {
		if(!dataTable.getSelectedRows().isEmpty()) {
			int selectedRow = ((Integer) dataTable.getSelectedRows().iterator().next()).intValue();
			if (modified) {
				dataTable.getCellFormatter().addStyleName(selectedRow, 0, "bold");
			} else {
				dataTable.getCellFormatter().removeStyleName(selectedRow, 0, "bold");
			}
		}
	}
	
	
	/**
	 * Grant the user
	 * 
	 * @param user The granted user
	 * @param permissions The permissions value
	 */
	public void grant(String user, int permissions, boolean recursive) {
		if (path != null) {
			Log.debug("UserScrollTable.grant(" + user + ", " + permissions + ", " + recursive + ")");
			Main.get().securityPopup.securityPanel.securityUser.grant(user, permissions, recursive, flag_property);
		}
	}
	
	/**
	 * Revoke the user grant
	 * 
	 * @param user The user
	 * @param permissions The permissions value
	 */
	public void revoke(String user, int permissions, boolean recursive) {
		if (path != null) {
			Log.debug("UserScrollTable.revoke(" + user + ", " + permissions + ", " + recursive + ")");
			Main.get().securityPopup.securityPanel.securityUser.revoke(user, permissions, recursive, flag_property);
		}
	}
	
	/**
	 * Sets the path
	 * 
	 * @param path The path
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * fillWidth
	 */
	public void fillWidth() {
		table.fillWidth();
	}
	
	/**
	 * getDataTable
	 */
	public FixedWidthGrid getDataTable() {
		return table.getDataTable();
	}
	
	/**
	 * getNumberOfColumns
	 */
	public int getNumberOfColumns() {
		return numberOfColumns;
	}
}