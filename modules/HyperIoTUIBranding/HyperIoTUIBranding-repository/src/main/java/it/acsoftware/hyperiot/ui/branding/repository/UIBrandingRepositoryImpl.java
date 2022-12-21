package it.acsoftware.hyperiot.ui.branding.repository;

import java.util.logging.Level;

import org.apache.aries.jpa.template.JpaTemplate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;

import it.acsoftware.hyperiot.ui.branding.api.UIBrandingRepository ;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;

/**
 * 
 * @author Aristide Cittadino compileOnly class of the UIBranding. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=UIBrandingRepository.class,immediate=true)
public class UIBrandingRepositoryImpl extends HyperIoTBaseRepositoryImpl<UIBranding> implements UIBrandingRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a UIBrandingRepositoryImpl
	 */
	public UIBrandingRepositoryImpl() {
		super(UIBranding.class);
	}

	/**
	 * 
	 * @return The current jpaTemplate
	 */
	@Override
	protected JpaTemplate getJpa() {
		getLog().debug("invoking getJpa, returning: {}" , jpa);
		return jpa;
	}

	/**
	 * @param jpa Injection of JpaTemplate
	 */
	@Override
	@Reference(target = "(osgi.unit.name=hyperiot-uIBranding-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug("invoking setJpa, setting: " + jpa);
		this.jpa = jpa;
	}
}
