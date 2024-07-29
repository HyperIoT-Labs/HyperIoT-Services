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

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Aristide Cittadino Model class for Widget of HyperIoT platform. This
 * class is used to map Widget with the database.
 */

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class Widget extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity {

    private String name;
    private String description;
    private WidgetCategory widgetCategory;
    private Set<WidgetDomain> domains;
    private String baseConfig;
    private String type;
    private int cols;
    private int rows;
    private byte[] image;
    private byte[] preView;
    private Float avgRating;
    private boolean offline;
    private boolean realTime = true;
    private List<WidgetRating> widgetRatings;

    @Column(length = 500)
    @Size(max = 500)
    @NotNullOnPersist
    @NoMalitiusCode
    @NotEmpty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Size(max = 3000)
    @NoMalitiusCode
    @Column( length = 3000)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public WidgetCategory getWidgetCategory() {
        return widgetCategory;
    }

    public void setWidgetCategory(WidgetCategory widgetCategory) {
        this.widgetCategory = widgetCategory;
    }

    //@NotNullOnPersist
    //@ElementCollection
    //@Basic(fetch = FetchType.EAGER)
    @Transient
    public Set<WidgetDomain> getDomains() {
        return domains;
    }

    public void setDomains(Set<WidgetDomain> domains) {
        this.domains = domains;
    }

    @Column(length = 200000)
    @Size(max = 200000)
    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    @Column(length = 200000)
    @Size(max = 200000)
    public byte[] getPreView() {
        return preView;
    }

    public void setPreView(byte[] preView) {
        this.preView = preView;
    }

    @NotNullOnPersist
    @NoMalitiusCode
    public String getBaseConfig() {
        return baseConfig;
    }

    public void setBaseConfig(String baseConfig) {
        this.baseConfig = baseConfig;
    }

    @Formula("(SELECT SUM(wr.rating)/count(*) FROM widgetrating wr WHERE wr.widget_id = id)")
    @Basic(fetch = FetchType.LAZY)
    public Float getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(Float avgRating) {
        this.avgRating = avgRating;
    }

    @NotNullOnPersist
    @NoMalitiusCode
    @NotEmpty
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    @Column(columnDefinition = "boolean default 'false'")
    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    @Column(columnDefinition = "boolean default 'true'")
    public boolean isRealTime() {
        return realTime;
    }

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }

    /**
     * Get widget's rating.
     *
     * @return widget ratings
     */
    @OneToMany(mappedBy = "widget", cascade = CascadeType.REMOVE, targetEntity = WidgetRating.class)
    @JsonIgnore
    public List<WidgetRating> getWidgetRatings() {
        return widgetRatings;
    }

    /**
     * Set widget's rating.
     *
     * @param widgetRatings
     */
    public void setWidgetRatings(List<WidgetRating> widgetRatings) {
        this.widgetRatings = widgetRatings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Widget)) return false;
        Widget widget = (Widget) o;
        if (widget.getId() > 0 && this.getId() > 0)
            return widget.getId() == this.getId();

        return getName().equals(widget.getName()) &&
                getType().equals(widget.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType());
    }
}
