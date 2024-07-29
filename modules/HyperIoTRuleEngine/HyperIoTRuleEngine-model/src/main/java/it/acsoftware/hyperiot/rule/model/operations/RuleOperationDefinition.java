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

package it.acsoftware.hyperiot.rule.model.operations;

import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RuleOperationDefinition {
    private String name;
    private String operator;
    private OperationAppliance appliance;
    private HPacketFieldType[] supportedFieldTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public OperationAppliance getAppliance() {
        return appliance;
    }

    public void setAppliance(OperationAppliance appliance) {
        this.appliance = appliance;
    }

    public HPacketFieldType[] getSupportedFieldTypes() {
        return supportedFieldTypes;
    }

    public void setSupportedFieldTypes(HPacketFieldType[] supportedFieldTypes) {
        this.supportedFieldTypes = supportedFieldTypes;
    }

    public static List<RuleOperationDefinition> getDefinedOperationsDefinitions() {
        List<RuleOperationDefinition> operationDefinitions = new ArrayList<>();
        RuleOperation.getDefinedOperations().forEach(op -> {
            RuleOperationDefinition opd = new RuleOperationDefinition();
            opd.setName(op.getName());
            opd.setOperator(op.operator());
            opd.setAppliance(op.getAppliance());
            opd.setSupportedFieldTypes(op.supportedFieldTypes());
            operationDefinitions.add(opd);
        });
        //sorting by name
        Collections.sort(operationDefinitions, Comparator.comparing(RuleOperationDefinition::getName));
        return operationDefinitions;
    }
}
