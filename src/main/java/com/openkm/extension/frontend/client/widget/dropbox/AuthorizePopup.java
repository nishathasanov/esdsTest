package com.openkm.extension.frontend.client.widget.dropbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.openkm.extension.frontend.client.bean.GWTDropboxAccount;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.extension.frontend.client.service.OKMDropboxServiceAsync;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.UtilComunicator;

/**
 * AuthorizePopup
 * 
 * @author sochoa
 * 
 */
public class AuthorizePopup extends DialogBox {
	private final OKMDropboxServiceAsync dropboxService = (OKMDropboxServiceAsync) GWT.create(OKMDropboxService.class);
	
	private VerticalPanel vPanel;
	private HTML allow;
	private Button cancelButton;
	private Button nextButton;
	private Button continueButton;
	private HTML operationResult;
	private HTML message;
	
	/**
	 * AuthorizePopup
	 */
	public AuthorizePopup() {
		super(false, true);
		
		setText(GeneralComunicator.i18nExtension("dropbox.authorize.title"));
		vPanel = new VerticalPanel();
	
		operationResult = new HTML("&nbsp;");
		
		message = new HTML("&nbsp;");
		// Allow
		allow = new HTML(GeneralComunicator.i18nExtension("dropbox.authorize.allow"));
				
		cancelButton = new Button(GeneralComunicator.i18n("button.cancel"));
		cancelButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		cancelButton.setStyleName("okm-NoButton");
		
		nextButton = new Button(GeneralComunicator.i18nExtension("dropbox.authorize.next"));
		nextButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				access();
			}
		});
		nextButton.setStyleName("okm-YesButton");
		
		continueButton = new Button(GeneralComunicator.i18nExtension("button.continue"));
		continueButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
				Dropbox.get().subMenuDropbox.execute();
			}
		});
		continueButton.setStyleName("okm-YesButton");
		
		HorizontalPanel hPanel = new HorizontalPanel();
		hPanel.add(cancelButton);
		hPanel.add(UtilComunicator.hSpace("5"));
		hPanel.add(nextButton);
		hPanel.add(UtilComunicator.hSpace("5"));
		hPanel.add(continueButton);
		
		vPanel.add(operationResult);
		vPanel.add(message);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(allow);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.add(hPanel);
		vPanel.add(UtilComunicator.vSpace("5"));
		vPanel.setCellHorizontalAlignment(message, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(allow, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(operationResult, HasAlignment.ALIGN_CENTER);
		vPanel.setCellHorizontalAlignment(hPanel, HasAlignment.ALIGN_CENTER);
		vPanel.setWidth("100%");		
		setWidget(vPanel);
	}
	
	/**
	 * access
	 */
	public void access() {
		dropboxService.access(new AsyncCallback<GWTDropboxAccount>() {
			@Override
			public void onSuccess(GWTDropboxAccount result) {
				operationResult.setVisible(true);
				if (result != null) {
					message.setVisible(true);
					allow.setVisible(false);
					continueButton.setVisible(true);
					nextButton.setVisible(false);
					operationResult.setHTML(GeneralComunicator.i18nExtension("dropbox.authorize.ok"));
					operationResult.setStyleName("okm-Input-Ok");
					message.setHTML(result.getDisplayName());
				} else {
					operationResult.setHTML(GeneralComunicator.i18nExtension("dropbox.authorize.error"));
					operationResult.setStyleName("okm-Input-Error");
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GeneralComunicator.showError("access", caught);
			}
		});
	}
	
	/**
	 * reset
	 */
	public void reset() {
		allow.setVisible(true);
		nextButton.setVisible(true);
		continueButton.setVisible(false);
		operationResult.setVisible(false);
		message.setVisible(false);
	}
	
	/**
	 * langRefresh
	 */
	public void langRefresh() {
		setText(GeneralComunicator.i18nExtension("dropbox.authorize.title"));
		allow.setHTML(GeneralComunicator.i18nExtension("dropbox.authorize.allow"));
		cancelButton.setHTML(GeneralComunicator.i18n("button.cancel"));
		nextButton.setHTML(GeneralComunicator.i18nExtension("dropbox.authorize.next"));
		continueButton.setHTML(GeneralComunicator.i18nExtension("button.continue"));
	}
}
