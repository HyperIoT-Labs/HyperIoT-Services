/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.ui.branding.service.pre;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingSystemApi;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import it.acsoftware.hyperiot.ui.branding.model.UIBrandingConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.huser.model.HUser"},immediate = true)
public class BeforeUserRemovedAction<T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {
    private static Logger log = LoggerFactory.getLogger(BeforeUserRemovedAction.class);

    private UIBrandingSystemApi uiBrandingSystemApi;

    @Override
    public void execute(T entity) {
        //before User removed, just empty the asset folder and remove the branding
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

            try {
                HyperIoTQuery q = HyperIoTQueryBuilder.newQuery().equals("huser.id", user.getId());
                UIBranding branding = uiBrandingSystemApi.find(q, null);
                if (branding != null) {
                    uiBrandingSystemApi.remove(branding.getId(), null);
                }
            } catch (Exception e){
                log.warn("No branding found for user, not removing it!");
            }
        }
    }

    @Reference
    public void setUiBrandingSystemApi(UIBrandingSystemApi uiBrandingSystemApi) {
        this.uiBrandingSystemApi = uiBrandingSystemApi;
    }
}
