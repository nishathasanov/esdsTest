package com.openkm.extension.frontend.client.widget.dropbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.openkm.extension.frontend.client.bean.GWTDropboxAccount;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.extension.frontend.client.service.OKMDropboxServiceAsync;
import com.openkm.frontend.client.bean.GWTFolder;
import com.openkm.frontend.client.bean.GWTPermission;
import com.openkm.frontend.client.constants.ui.UIMenuConstants;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.NavigatorComunicator;
import com.openkm.frontend.client.extension.comunicator.UtilComunicator;
import com.openkm.frontend.client.extension.widget.menu.MenuBarExtension;
import com.openkm.frontend.client.extension.widget.menu.MenuItemExtension;

/**
 * SubMenuDropbox
 * 
 * @author sochoa
 */
public class SubMenuDropbox {
	private final OKMDropboxServiceAsync dropboxService = (OKMDropboxServiceAsync) GWT.create(OKMDropboxService.class);
	
	private static final int OPTION_MENU_NONE = 0;
	private static final int OPTION_MENU_EXPORT = 1;
	private static final int OPTION_MENU_IMPORT = 2;
	
	private MenuItemExtension dropboxMenu;
	private MenuItemExtension exportMenu;
	private MenuItemExtension importMenu;
	private MenuBarExtension subMenu;
	
	private boolean exportOption = false;
	private boolean importOption = false;
	private int menu = OPTION_MENU_NONE;
	
	/**
	 * SubMenuDropbox
	 */
	public SubMenuDropbox() {
		// All menu items
		exportMenu = new MenuItemExtension("img/icon/actions/export_document.png", GeneralComunicator.i18nExtension("dropbox.export"),
				dropboxExport);
		exportMenu.addStyleName("okm-MenuItem-strike");
		importMenu = new MenuItemExtension("img/icon/actions/import_document.png", GeneralComunicator.i18nExtension("dropbox.import"),
				dropboxImport);
		importMenu.addStyleName("okm-MenuItem-strike");
		
		// Main subMenu
		subMenu = new MenuBarExtension();
		subMenu.addItem(exportMenu);
		subMenu.addItem(importMenu);
		
		// Main menu
		dropboxMenu = new MenuItemExtension("img/icon/actions/dropbox.png", GeneralComunicator.i18nExtension("dropbox.menu"), subMenu);
		dropboxMenu.setMenuLocation(UIMenuConstants.MAIN_MENU_TOOLS);
	}
	
	/**
	 * getMenu
	 */
	public MenuItemExtension getMenu() {
		return dropboxMenu;
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		exportMenu.setHTML(UtilComunicator.menuHTML("img/icon/actions/export_document.png",
				GeneralComunicator.i18nExtension("dropbox.export")));
		importMenu.setHTML(UtilComunicator.menuHTML("img/icon/actions/import_document.png",
				GeneralComunicator.i18nExtension("dropbox.import")));
		dropboxMenu.setHTML(UtilComunicator.menuHTML("img/icon/actions/dropbox.png", GeneralComunicator.i18nExtension("dropbox.menu")));
	}
	
	/**
	 * dropboxExport
	 */
	Command dropboxExport = new Command() {
		@Override
		public void execute() {
			if (exportOption) {
				menu = OPTION_MENU_EXPORT;
				executeWidthAccessValidation();
			}
		}
	};
	
	/**
	 * dropboxImport
	 */
	Command dropboxImport = new Command() {
		@Override
		public void execute() {
			if (importOption) {
				menu = OPTION_MENU_IMPORT;
				executeWidthAccessValidation();
			}
		}
	};
	
	/**
	 * export
	 */
	public void export(String path) {		
		switch (Dropbox.get().getSelectedPanel()) {
			case Dropbox.TAB_DOCUMENT:
				Dropbox.get().status.setExporting();
				dropboxService.exportDocument(path, Dropbox.get().getUuid(), new AsyncCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						Dropbox.get().status.unsetExporting();
					}
					
					@Override
					public void onFailure(Throwable caught) {
						GeneralComunicator.showError("exportDocument", caught);
						Dropbox.get().status.unsetExporting();
					}
				});
				break;
			case Dropbox.TAB_FOLDER:
				Dropbox.get().status.setExporting();
				Dropbox.get().startStatusListener(StatusListenerPopup.ACTION_EXPORT);
				dropboxService.exportFolder(path, Dropbox.get().getUuid(), new AsyncCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						Dropbox.get().stopStatusListener();
						Dropbox.get().status.unsetExporting();
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Dropbox.get().stopStatusListener();
						GeneralComunicator.showError("exportFolder", caught);
						Dropbox.get().status.unsetExporting();
					}
				});
				break;
		}
	}
	
	/**
	 * evaluateMenus
	 */
	public void evaluateMenus() {
		GWTFolder folder = NavigatorComunicator.getFolder(); // The actual folder selected in navigator view
		importOption = ((folder.getPermissions() & GWTPermission.WRITE) == GWTPermission.WRITE); // folder determines
		exportOption = ((Dropbox.get().getSelectedPanel() == Dropbox.TAB_DOCUMENT) || (Dropbox.get().getSelectedPanel() == Dropbox.TAB_FOLDER));
		// Enable disabling menus
		if (exportOption) {
			exportMenu.removeStyleName("okm-MenuItem-strike");
		} else {
			exportMenu.addStyleName("okm-MenuItem-strike");
		}
		if (importOption) {
			importMenu.removeStyleName("okm-MenuItem-strike");
		} else {
			importMenu.addStyleName("okm-MenuItem-strike");
		}
	}
	
	/**
	 * execute
	 */
	public void execute() {
		switch (menu) {
			case OPTION_MENU_IMPORT:
				Dropbox.get().searchPopup.center();
				Dropbox.get().searchPopup.reset();
				break;
			
			case OPTION_MENU_EXPORT:
				switch (Dropbox.get().getSelectedPanel()) {
					case Dropbox.TAB_DOCUMENT:
						Dropbox.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_EXPORT_DOCUMENT);
						Dropbox.get().confirmPopup.center();
						break;
					case Dropbox.TAB_FOLDER:
						Dropbox.get().confirmPopup.setConfirm(ConfirmPopup.CONFIRM_EXPORT_FOLDER);
						Dropbox.get().confirmPopup.center();
						break;
				}
				break;
		}
	}
	
	/**
	 * executeWidthAccessValidation
	 */
	public void executeWidthAccessValidation() {
		dropboxService.access(new AsyncCallback<GWTDropboxAccount>() {
			@Override
			public void onSuccess(GWTDropboxAccount result) {
				if (result == null) {
					dropboxService.authorize(new AsyncCallback<String>() {
						@Override
						public void onSuccess(String result) {
							String features = "";
							Dropbox.get().authorizePopup.reset();
							int left = (Window.getClientWidth() - 500) / 2;
							int top = 150;
							if (left < 0 || top < 0) {
								Dropbox.get().authorizePopup.center();
								features = "menubar=no,location=no,resizable=yes,scrollbars=yes,status=yes,height=500,width=950";
							} else {
								Dropbox.get().authorizePopup.setPopupPosition(left, top);
								Dropbox.get().authorizePopup.show();
								left = Dropbox.get().authorizePopup.getAbsoluteLeft() - 270;
								top += 250;
								features = "menubar=no,location=no,resizable=yes,scrollbars=yes,status=yes,height=500,width=1050, left="
										+ left + ",top=" + top;
							}
							Window.open(result, "dropbox", features);
						}
						
						@Override
						public void onFailure(Throwable caught) {
							GeneralComunicator.showError("authorize", caught);
						}
					});
				} else {
					execute();
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GeneralComunicator.showError("access", caught);
			}
		});
	}
}