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

package it.acsoftware.hyperiot.rule.model.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.rule.model.RuleType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Enumerated;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Aristide Cittadino Identifies the interface for Rule associated
 * actions
 */
public abstract class RuleAction {
    private static final Logger log = LoggerFactory.getLogger(RuleAction.class.getName());
    public static ObjectMapper mapper = new ObjectMapper();
    public static JsonStringEncoder jse = new JsonStringEncoder();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    private String actionName = this.getClass().getName();
    private long ruleId;
    private String ruleName;
    //list of packet ids that cause the condition to be fired
    private Set<Long> packetIds = new HashSet<>();
    private List<AssetTag> tags;
    boolean active;
    protected BundleContext bundleContext;
    private long fireTimestamp;
    private String firePayload;

    public String getActionName() {
        return actionName;
    }
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
    public abstract String droolsDefinition();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext context) {
        this.bundleContext = context;
    }

    @Enumerated
    public abstract RuleType getRuleType();

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public List<AssetTag> getTags() {
        return tags;
    }

    public void setTags(List<AssetTag> tags) {
        this.tags = tags;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean executeAsSoonAsUnmetCondition() {
        return false;
    }

    public long getFireTimestamp() {
        return fireTimestamp;
    }

    public void setFireTimestamp(long fireTimestamp) {
        this.fireTimestamp = fireTimestamp;
    }

    public String getFirePayload() {
        return firePayload;
    }

    public Set<Long> getPacketIds() {
        return packetIds;
    }

    public void setPacketIds(Set<Long> packetIds) {
        this.packetIds = packetIds;
    }

    public void setFirePayload(String firePayload) {
        this.firePayload = firePayload;
    }

    public static RuleAction deserializeFromJson(String jsonObject) {
        log.debug("In RuleAction.deserializeFromJson: {}", jsonObject);
        RuleAction r = null;
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        try {
            HashMap<String, Object> node = mapper.readValue(jsonObject, typeRef);
            String className = node.get("actionName").toString();
            Class<?> cls = Class.forName(className);
            Object ruleActionObject = mapper.readValue(jsonObject, cls);
            r = (RuleAction) ruleActionObject;
            log.debug("Rule Action successfully read: {}", r);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return r;
    }

    protected String encodeAsJsonString(String jsonData) {
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, jsonData);
            String jsonEscaped = writer.toString();
            //removing quotes symbols encapsulating the json string
            return jsonEscaped.substring(1, jsonEscaped.length() - 1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        //returning original data, in fail fast way
        return jsonData;
    }

    protected String droolsAsJson() {
        String data = "";
        try {
            data = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        //Encoding in base 64
        data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        // add sendMail action to the rule actions list
        return "actions.add(\"" + data + "\")";
    }
}
