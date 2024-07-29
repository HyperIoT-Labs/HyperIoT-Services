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
 * @author Aristide Cittadino And logic operation
 */
@Component(service = RuleOperation.class)
public class AndOperation extends BinaryOperation implements RuleNode {


    /**
     * returns the token string which identifies the operation
     */
    @Override
    public String operator() {
        return "AND";
    }

    /**
     * Returns the operation name
     */
    @Override
    public String getName() {
        return "And";
    }

    /**
     * @return true if this operation needs an inner expression
     */
    @Override
    public boolean needsExpr() {
        return true;
    }

    /**
     * @return the drools definition of the logic condition
     */
    @Override
    public String droolsDefinition() {
        return operands.get(0).droolsDefinition() +
                " && " +
                operands.get(1).droolsDefinition();
    }

    @Override
    public OperationAppliance getAppliance() {
        return OperationAppliance.LOGIC_OPERATOR;
    }

    @Override
    public HPacketFieldType[] supportedFieldTypes() {
        return new HPacketFieldType[]{HPacketFieldType.BOOLEAN};
    }
}
