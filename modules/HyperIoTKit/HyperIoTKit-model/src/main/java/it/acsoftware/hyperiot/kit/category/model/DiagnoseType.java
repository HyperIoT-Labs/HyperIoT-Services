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

package it.acsoftware.hyperiot.kit.category.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.huser.model.HUser;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"label","user_id"})})
public class DiagnoseType extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity, HyperIoTOwnedResource {

    @JsonView({HyperIoTJSONView.Public.class})
    private String label;

    @JsonView({HyperIoTJSONView.Public.class})
    private AssetCategory category;

    /**
     * User to whom the ale belong to
     */
    @JsonView({HyperIoTJSONView.Public.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private HUser user;


    @NotNullOnPersist
    @NotBlank
    @NoMalitiusCode
    @Length(max = 80)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @OneToOne(targetEntity = AssetCategory.class , cascade = CascadeType.ALL)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    public AssetCategory getCategory() {
        return category;
    }

    public void setCategory(AssetCategory category) {
        this.category = category;
    }

    @NotNullOnPersist
    @ManyToOne(targetEntity = HUser.class)
    public HUser getUser() {
        return user;
    }

    public void setUser(HUser user) {
        this.user = user;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTUser getUserOwner() {
        return user;
    }

    @Override
    public void setUserOwner(HyperIoTUser hyperIoTUser) {
        this.setUser((HUser) hyperIoTUser);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DiagnoseType other = (DiagnoseType) obj;
        if (other.getId() > 0 && this.getId() > 0) {
            return other.getId() == this.getId();
        }
        if (label == null) {
            if (other.label!= null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if(user == null){
            if(other.user != null)
                return false;
        }else if (! user.equals(other.user)){
            return false;
        }
        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        return result;
    }
}
