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

package it.acsoftware.hyperiot.widget.model;


import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.huser.model.HUser;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "widget_id"}))
public class WidgetRating extends HyperIoTAbstractEntity {

    private HUser user;
    private Widget widget;
    private int rating;

    @NotNullOnPersist
    @ManyToOne(targetEntity = HUser.class)
    public HUser getUser() {
        return user;
    }

    public void setUser(HUser user) {
        this.user = user;
    }

    /**
     * @return the related widget
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = Widget.class)
    public Widget getWidget() {
        return widget;
    }

    /**
     * @param widget the related widget
     */
    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }


}
