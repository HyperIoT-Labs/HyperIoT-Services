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

package it.acsoftware.hyperiot.hproject.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTSharedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.huser.model.HUser;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Formula;


import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Aristide Cittadino Model class for HProject of HyperIoT platform.
 * This class is used to map HProject with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "user_id"})})
@NamedEntityGraph(name = "completeProject",
        attributeNodes = {
            @NamedAttributeNode(value = "devices", subgraph = "devices"),
            @NamedAttributeNode(value = "user")
        },
        subgraphs = {
                @NamedSubgraph(name = "devices", attributeNodes = @NamedAttributeNode(value = "packets",subgraph = "packets")),
                @NamedSubgraph(name = "packets", attributeNodes = @NamedAttributeNode(value = "fields",subgraph = "fields")),
                @NamedSubgraph(name = "fields", attributeNodes = @NamedAttributeNode(value = "innerFields",subgraph = "innerFields")),
                @NamedSubgraph(name = "innerFields", attributeNodes = @NamedAttributeNode(value = "innerFields"))
        })
public class HProject extends HyperIoTAbstractEntity
    implements HyperIoTProtectedEntity, HyperIoTSharedEntity {
    /**
     * Project name
     */
    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class, HProjectJSONView.Cards.class,HProjectJSONView.Export.class})
    private String name;

    /**
     * Project description
     */
    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class, HProjectJSONView.Cards.class,HProjectJSONView.Export.class})
    private String description;

    /**
     * User to whom the project belongs to
     */
    @JsonView({HyperIoTJSONView.Public.class,HyperIoTJSONView.Extended.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private HUser user;

    @JsonView({HProjectJSONView.Cards.class,HyperIoTJSONView.Extended.class})
    private int deviceCount;

    @JsonView({HProjectJSONView.Cards.class,HyperIoTJSONView.Extended.class})
    private int statisticsCount;

    @JsonView({HProjectJSONView.Cards.class,HyperIoTJSONView.Extended.class})
    private int rulesCount;
    @JsonIgnore
    private int sharesCount;
    @JsonView({HProjectJSONView.Cards.class,HyperIoTJSONView.Extended.class})
    private HProjectSharingInfo hProjectSharingInfo;

    /**
     * Public key for autocreation projects made by gateway devices
     */
    @JsonView(HyperIoTJSONView.Internal.class)
    private byte[] pubKey;

    /**
     * Generated challenge from server for autocreation of devices
     */
    private String generatedChallenge;


    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Internal.class,HProjectJSONView.Export.class})
    private Set<HDevice> devices;

    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Extended.class})
    private Set<Area> areas;

    /**
     * @return Project Name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size( max = 255)
    public String getName() {
        return name;
    }

    /**
     * @param name The project Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Project Description
     */
    @NoMalitiusCode
    @Column(length = 3000)
    @Size( max = 3000)
    public String getDescription() {
        return description;
    }

    /**
     * @param description Project description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Project's user
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HUser.class)
    public HUser getUser() {
        return user;
    }

    @Formula(value = "(SELECT COUNT(*) FROM hdevice h WHERE h.project_id = id)")
    @Basic(fetch = FetchType.LAZY)
    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    //TO DO: Change to statistics table, not implemented yet
    @Formula(value = "(SELECT COUNT(*) FROM hdevice h WHERE h.project_id = id)")
    @Basic(fetch = FetchType.LAZY)
    public int getStatisticsCount() {
        return statisticsCount;
    }

    public void setStatisticsCount(int statisticsCount) {
        this.statisticsCount = statisticsCount;
    }

    @Formula(value = "(SELECT COUNT(*) FROM rule r WHERE r.project_id = id)")
    @Basic(fetch = FetchType.LAZY)
    public int getRulesCount() {
        return rulesCount;
    }

    public void setRulesCount(int rulesCount) {
        this.rulesCount = rulesCount;
    }

    @Formula(value = "(SELECT COUNT(*) FROM SharedEntity s WHERE s.entityResourceName = 'it.acsoftware.hyperiot.hproject.model.HProject' AND s.entityId = id)")
    @Basic(fetch = FetchType.LAZY)
    public int getSharesCount() {
        return sharesCount;
    }

    public void setSharesCount(int sharesCount) {
        this.sharesCount = sharesCount;
    }

    @Transient
    public HProjectSharingInfo gethProjectSharingInfo() {
        this.hProjectSharingInfo = new HProjectSharingInfo(this.getUserOwner().getId(),this.getUserOwner().getName(),this.getUserOwner().getLastname(),this.getSharesCount());
        return hProjectSharingInfo;
    }

    /**
     * @return
     */
    @Override
    @Transient
    @JsonIgnore
    public HyperIoTUser getUserOwner() {
        return this.getUser();
    }

    @Override
    public void setUserOwner(HyperIoTUser hyperIoTUser) {
        this.setUser((HUser) hyperIoTUser);
    }

    /**
     * @param user The user to whom the project belongs to
     */
    public void setUser(HUser user) {
        this.user = user;
    }

    @JsonIgnore
    @Basic
    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    @JsonIgnore
    public String getGeneratedChallenge() {
        return generatedChallenge;
    }

    public void setGeneratedChallenge(String generatedChallenge) {
        this.generatedChallenge = generatedChallenge;
    }

    @OneToMany(mappedBy = "project", cascade = {CascadeType.REMOVE, CascadeType.PERSIST }, targetEntity = HDevice.class)
    @JsonIgnore
    @Fetch(FetchMode.SUBSELECT)
    public Set<HDevice> getDevices() {
        return devices;
    }

    public void setDevices(Set<HDevice> devices) {
        this.devices = devices;
    }

    @OneToMany(mappedBy = "project", cascade = {CascadeType.REMOVE , CascadeType.PERSIST}, targetEntity = Area.class)
    @JsonIgnore
    public Set<Area> getAreas() {
        return areas;
    }

    public void setAreas(Set<Area> areas) {
        this.areas = areas;
    }

    @Transient
    public List<String> getProjectTopics(HyperIoTTopicType type) {
        ArrayList<String> topics = new ArrayList<>();
        if (type == HyperIoTTopicType.MQTT) {
            topics.add("/project/" + this.getId());
        } else if (type == HyperIoTTopicType.KAFKA) {
            //raw data realtime topic
            topics.add("streaming." + this.getId());
        }
        return topics;
    }

    @Transient
    public List<String> getDeviceRealtimeTopics(HyperIoTTopicType type, Set<HDevice> devices) {
        ArrayList<String> realtimeTopics = new ArrayList<>();
        if (type == HyperIoTTopicType.MQTT) {
            //ON MQTT each topic is dedicated to a single project, device and packet
            devices.forEach((d) -> {
                realtimeTopics.addAll(this.getDeviceRealtimeTopics(type, d.getPackets()));
            });
        } else if (type == HyperIoTTopicType.KAFKA) {
            //ON Kafka we receive on a single realtime topic every data for every device
            // The Kafka Key embed deviceId.packetId, this is the enreached topic with Avro data
            realtimeTopics.add("streaming.realtime." + this.getId());
        }
        return realtimeTopics;
    }

    @Transient
    public List<String> getDeviceRealtimeTopics(HyperIoTTopicType type, Collection<HPacket> packets) {
        ArrayList<String> realtimeTopics = new ArrayList<>();
        if (type == HyperIoTTopicType.MQTT) {
            //ON MQTT each topic is dedicated to a single project, device and packet
            packets.forEach((p) -> {
                realtimeTopics.add("/" + p.getDevice().getId() + "/receive");
                realtimeTopics.add("streaming/" + p.getDevice().getProject().getId() + "/" + p.getDevice().getId() + "/" + p.getId());
            });
        } else if (type == HyperIoTTopicType.KAFKA) {
            //ON Kafka we receive on a single realtime topic every data for every device
            // The Kafka Key embed deviceId.packetId, this is the enreached topic with Avro data
            realtimeTopics.add("streaming.realtime." + this.getId());
        }
        return realtimeTopics;
    }

    @Transient
    public List<String> getWriteOnlyDeviceTopics(HyperIoTTopicType type, Collection<HDevice> devices, HDevice current) {
        List<String> topics = new ArrayList<>();
        //Retrieving other devices of same project, allowing to interact via "<id>/receive topic
        devices.forEach((d) -> {
            if (d.getId() != current.getId()) {
                if (type == HyperIoTTopicType.MQTT) {
                    topics.add("/" + d.getId() + "/receive");
                } else {
                    topics.add(+d.getId() + ".receive");
                }
            }
        });
        return topics;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HProject other = (HProject) obj;

        if (this.getId() > 0 && other.getId() > 0)
            return this.getId() == other.getId();

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

}
