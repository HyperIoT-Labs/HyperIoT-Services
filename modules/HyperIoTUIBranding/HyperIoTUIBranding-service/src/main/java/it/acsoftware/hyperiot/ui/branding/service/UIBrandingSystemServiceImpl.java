package it.acsoftware.hyperiot.ui.branding.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingRepository;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingSystemApi;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

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

    private PermissionSystemApi permissionSystemApi;

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

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String hProjectResourceName = UIBranding.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(hProjectResourceName);
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }
}
