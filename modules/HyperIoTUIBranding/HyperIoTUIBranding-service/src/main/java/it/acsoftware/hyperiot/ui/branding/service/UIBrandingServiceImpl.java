package it.acsoftware.hyperiot.ui.branding.service;

import java.util.logging.Level;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.ui.branding.api.UIBrandingSystemApi;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingApi;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl ;


/**
 * 
 * @author Aristide Cittadino compileOnly class of UIBrandingApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = UIBrandingApi.class, immediate = true)
public final class UIBrandingServiceImpl extends HyperIoTBaseEntityServiceImpl<UIBranding>  implements UIBrandingApi {
	/**
	 * Injecting the UIBrandingSystemApi
	 */
	private UIBrandingSystemApi systemService;
	
	/**
	 * Constructor for a UIBrandingServiceImpl
	 */
	public UIBrandingServiceImpl() {
		super(UIBranding.class);
	}
	
	/**
	 * 
	 * @return The current UIBrandingSystemApi
	 */
	protected UIBrandingSystemApi getSystemService() {
		getLog().debug("invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 * 
	 * @param uIBrandingSystemService Injecting via OSGi DS current systemService 
	 */
	@Reference
	protected void setSystemService(UIBrandingSystemApi uIBrandingSystemService) {
		getLog().debug("invoking setSystemService, setting: {}" , systemService);
		this.systemService = uIBrandingSystemService ;
	}

}
