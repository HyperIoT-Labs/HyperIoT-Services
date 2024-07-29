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

package it.acsoftware.hyperiot.ui.branding.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.ui.branding.model.view.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

/**
 * @Author Aristide Cittadino.
 * Model class for UIBranding of HyperIoT platform.
 * This class is used to map UIBranding with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"huser_id"})})
public class UIBranding extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity, HyperIoTOwnedResource {
    @Transient
    private static final Logger log = LoggerFactory.getLogger(UIBranding.class);
    @JsonIgnore
    private HUser huser;
    @JsonIgnore
    private String faviconPath;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String faviconBase64;
    @JsonIgnore
    private String logoPath;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String logoBase64;
    private String colorScheme;
    private String name;

    @NotNullOnPersist
    @OneToOne
    public HUser getHuser() {
        return huser;
    }

    public void setHuser(HUser huser) {
        this.huser = huser;
    }

    @JsonView({Isolated.class})
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonView({Isolated.class})
    public String getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }

    @NoMalitiusCode
    @NotNullOnPersist
    public String getFaviconPath() {
        return faviconPath;
    }

    public void setFaviconPath(String faviconPath) {
        this.faviconPath = faviconPath;
    }

    @Transient
    @JsonView({Isolated.class})
    public String getFaviconBase64() {
        return faviconBase64;
    }

    public void setFaviconBase64(String faviconBase64) {
        this.faviconBase64 = faviconBase64;
    }

    @NoMalitiusCode
    @NotNullOnPersist
    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    @Transient
    @JsonView({Isolated.class})
    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTUser getUserOwner() {
        return getHuser();
    }

    @Override
    public void setUserOwner(HyperIoTUser hyperIoTUser) {
        this.setHuser((HUser) hyperIoTUser);
    }

    @PostLoad
    public void loadBase64Images() {
        this.setLogoBase64(encodeBase64StringFromPath(this.getLogoPath()));
        this.setFaviconBase64(encodeBase64StringFromPath(this.getFaviconPath()));
    }

    private String encodeBase64StringFromPath(String path) {
        if (path != null && !path.isEmpty() && !path.isBlank()) {
            try {
                File file = new File(path);
                byte[] encoded = Base64.getEncoder().encode(Files.readAllBytes(file.toPath()));
                return new String(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return "";
    }
}