package it.acsoftware.hyperiot.ui.branding.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingRepository;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingSystemApi;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.File;
import java.io.IOException;

/**
 * @author Aristide Cittadino compileOnly class of the UIBrandingSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = UIBrandingSystemApi.class, immediate = true)
public final class UIBrandingSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<UIBranding> implements UIBrandingSystemApi {

    /**
     * Injecting the UIBrandingRepository to interact with persistence layer
     */
    private UIBrandingRepository repository;

    /**
     * Constructor for a UIBrandingSystemServiceImpl
     */
    public UIBrandingSystemServiceImpl() {
        super(UIBranding.class);
    }

    /**
     * Return the current repository
     */
    protected UIBrandingRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param uIBrandingRepository The current value of UIBrandingRepository to interact with persistence layer
     */
    @Reference
    protected void setRepository(UIBrandingRepository uIBrandingRepository) {
        getLog().debug("invoking setRepository, setting: {}", uIBrandingRepository);
        this.repository = uIBrandingRepository;
    }
}
