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
import org.osgi.service.component.annotations.Component;

/**
 * @author Aristide Cittadino Equal to operation
 */
@Component(service = RuleOperation.class)
public class IsFalse extends UnaryOperation implements RuleNode {

    /**
     * Returns the operation name
     */
    @Override
    public String getName() {
        return "isFalse";
    }

    /**
     * returns the token string which identifies the operation
     */
    @Override
    public String operator() {
        return "isFalse";
    }

    @Override
    public String getDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.operands.get(0).getDefinition()).append("== false");
        return sb.toString();
    }

    @Override
    public String droolsDefinition() {
        StringBuilder sb = new StringBuilder();
        return sb.append("eval(").append(operands.get(0).droolsDefinition()).append(" ")
            .append("==").append(" false")
            .append(")").toString();
    }

    @Override
    public HPacketFieldType[] supportedFieldTypes() {
        return new HPacketFieldType[]{HPacketFieldType.BOOLEAN};
    }
}
