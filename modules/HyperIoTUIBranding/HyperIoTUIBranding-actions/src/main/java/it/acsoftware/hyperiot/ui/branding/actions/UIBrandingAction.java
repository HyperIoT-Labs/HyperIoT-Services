package it.acsoftware.hyperiot.ui.branding.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate UIBranding Actions
 *
 */
public enum UIBrandingAction implements HyperIoTActionName {
	
	//TO DO: add enumerations here
	ACTION_ENUM("action_enum");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the UIBranding  action
	 */
	private UIBrandingAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of UIBranding action
	 */
	public String getName() {
		return name;
	}

}
