package it.acsoftware.hyperiot.ui.branding.service.post;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.ui.branding.model.UIBrandingConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.huser.model.HUser"})
public class AfterUserRemovedAction<T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {
    private static Logger log = LoggerFactory.getLogger(AfterUserRemovedAction.class);

    @Override
    public void execute(T entity) {
        //after User removed, just empty the asset folder
        HUser user = (HUser) entity;
        if (user != null) {
            File brandingFolder = new File(UIBrandingConstants.ASSET_FOLDER + File.separator + user.getId());
            if (brandingFolder.exists()) {
                try {
                    brandingFolder.delete();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
