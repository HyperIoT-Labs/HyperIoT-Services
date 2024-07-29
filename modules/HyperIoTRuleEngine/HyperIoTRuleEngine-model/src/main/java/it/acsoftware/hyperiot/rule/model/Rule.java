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

package it.acsoftware.hyperiot.rule.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.model.operations.RootRuleNode;
import it.acsoftware.hyperiot.rule.model.operations.RuleOperation;
import it.acsoftware.hyperiot.rule.model.operations.RuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Aristide Cittadino Model class for RuleEngine of HyperIoT platform.
 * This class is used to map RuleEngine with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "project_id"})})
public class Rule extends HyperIoTAbstractEntity
        implements HyperIoTProtectedEntity, HyperIoTOwnedChildResource {
    private static Logger logger = LoggerFactory.getLogger(Rule.class.getName());

    /**
     * Rule name
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String name;
    /**
     * Rule description
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String description;
    /**
     * Body of the rule
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String ruleDefinition;

    @JsonView(HyperIoTJSONView.Public.class)
    private String rulePrettyDefinition;
    /**
     * Project the rule is referred to
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private HProject project;

    /**
     * List of the packet ids involved inside the rule
     * automatically calculated.
     */
    @JsonView(HyperIoTJSONView.Public.class)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String packetIds;

    /**
     * Action to execute
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String jsonActions;

    /**
     *
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private List<RuleAction> actions = new ArrayList<>();

    /**
     * Rule Type
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private RuleType type;

    /**
     * Rule Object rappresentation
     */
    @JsonView(HyperIoTJSONView.Internal.class)
    private RootRuleNode rule;

    /**
     * @return Rule name
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Column(columnDefinition = "TEXT")
    public String getName() {
        return name;
    }

    /**
     * @param name Rule Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Rule description
     */
    @NoMalitiusCode
    @Column(columnDefinition = "TEXT")
    public String getDescription() {
        return description;
    }

    /**
     * @param description of the rule
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return project the rule is referred to
     */
    @ManyToOne(targetEntity = HProject.class)
    @NotNullOnPersist
    public HProject getProject() {
        return project;
    }

    /**
     * @param project
     */
    public void setProject(HProject project) {
        this.project = project;
    }

    /**
     * List of the packet ids
     *
     * @return
     */
    public String getPacketIds() {
        return packetIds;
    }

    public void setPacketIds(String packetIds) {
        this.packetIds = packetIds;
    }

    /**
     * @return String representation of the rule
     */
    @NoMalitiusCode
    @Column(columnDefinition = "TEXT")
    public String getRuleDefinition() {
        return ruleDefinition;
    }

    public void setRuleDefinition(String ruleDefinition) {
        this.ruleDefinition = ruleDefinition;
    }

    /**
     * @return String pretty representation of the rule in order to be sent via mail or other channels
     */
    @NoMalitiusCode
    @Column(columnDefinition = "TEXT")
    public String getRulePrettyDefinition() {
        return rulePrettyDefinition;
    }

    public void setRulePrettyDefinition(String rulePrettyDefinition) {
        this.rulePrettyDefinition = rulePrettyDefinition;
    }


    /**
     * @return RuleType if Enrichment or event
     */
    @Enumerated
    public RuleType getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(RuleType type) {
        this.type = type;
    }

    /**
     * @return
     */
    @Transient
    @ApiModelProperty(hidden = true)
    public List<RuleAction> getActions() {
        return actions;
    }

    /**
     * @param actions
     * @Deprecated
     */
    public void setActions(List<RuleAction> actions) {
        this.actions = actions;
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<String> stringActionsList = new ArrayList<>();
        actions.forEach(r -> {
            r.setRuleId(this.getId());
            try {
                stringActionsList.add(objectMapper.writeValueAsString(r));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        try {
            jsonActions = objectMapper.writeValueAsString(stringActionsList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return JSON serialized Actions
     */
    @NotNullOnPersist
    @NotEmpty
    @Column(columnDefinition = "TEXT")
    public String getJsonActions() {
        return jsonActions;
    }

    /**
     * @param jsonActions
     */
    public void setJsonActions(String jsonActions) throws IOException {
        this.jsonActions = jsonActions;
        TypeReference<ArrayList<String>> typeRefList = new TypeReference<ArrayList<String>>() {
        };
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<String> actionList = objectMapper.readValue(this.jsonActions, typeRefList);
        actions.clear();
        for (String o : actionList) {
            RuleAction r = RuleAction.deserializeFromJson(o);
            if (r != null) {
                r.setRuleId(this.getId());
                actions.add(r);
            } else {
                logger.error("Impossible to deserialize: " + jsonActions);
            }
        }
    }

    /**
     * @return the Rule representation with objects
     */
    @Transient
    @ApiModelProperty(hidden = true)
    public RootRuleNode getRule() {
        if (this.rule == null && this.getRuleDefinition() != null && !this.getRuleDefinition().isBlank()) {
            try {
                RuleParser parser = new RuleParser(
                        new StreamTokenizer(new StringReader(this.getRuleDefinition())));
                this.rule = parser.parse();
            } catch (IOException | IllegalAccessException | InstantiationException e) {
                logger.error(e.getMessage(), e);
                this.rule = null;
            }
        } else if (this.getRuleDefinition() == null || this.getRuleDefinition().isBlank()) {
            // return empty RootRuleNode if no RuleDefinition was set
            this.rule = new RootRuleNode(Collections.emptySet(), null);
        }
        return this.rule;
    }

    /**
     * Before save or update , calculates the packet associated with the rule and save them in a specific comma separated string.
     */
    @PrePersist
    @PreUpdate
    public void updatePacketIds() {
        StringBuilder sb = new StringBuilder();
        this.packetIds = "";
        this.getRule().getPacketIds().forEach(packetId -> sb.append(packetId).append(","));
        if (sb.length() > 0)
            this.packetIds = sb.substring(0, sb.length() - 1);
    }

    /**
     * For testing purpose, passing directly available operations
     *
     * @param operations
     * @return
     */
    @Transient
    public RootRuleNode getRule(List<RuleOperation> operations) {
        if (this.rule == null) {
            try {
                RuleParser parser = new RuleParser(
                        new StreamTokenizer(new StringReader(this.getRuleDefinition())),
                        operations);
                this.rule = parser.parse();
            } catch (IOException | IllegalAccessException | InstantiationException e) {
                logger.error(e.getMessage(), e);
                this.rule = null;
            }
        }
        return this.rule;
    }

    /**
     * @param rule
     */
    public void setRule(RootRuleNode rule) {
        this.rule = rule;
        this.setRuleDefinition(rule.getRuleNode().getDefinition());
    }

    /**
     * @return the Drools definition of the rule
     */
    public String droolsDefinition() {
        return RuleDroolsBuilder.buildDroolsFromRule(this);
    }

    /**
     * Override to define the parent object which owns this resource
     */
    @Override
    @Transient
    public HyperIoTBaseEntity getParent() {
        return this.project;
    }

    /**
     * Utility method for retrieving de SystemApi of the owner
     */
    @Override
    @Transient
    public String getSystemApiClassName() {
        String className = this.getClass().getName();
        return className.replace(".model.", ".api.") + "EngineSystemApi";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ruleDefinition == null) ? 0 : ruleDefinition.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Rule other = (Rule) obj;
        if (other.getId() > 0 && this.getId() > 0 && other.getId() == this.getId())
            return other.getId() == this.getId();

        if (ruleDefinition == null) {
            if (other.ruleDefinition != null)
                return false;
        } else if (!ruleDefinition.equals(other.ruleDefinition))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (project == null) {
            if (other.project != null)
                return false;
        } else if (!project.equals(other.project))
            return false;
        return true;
    }

}
