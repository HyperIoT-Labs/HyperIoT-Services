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

@Component(service = RuleOperation.class)
public class MaxMsPeriodicity extends BinaryOperation implements RuleNode {
    @Override
    public String getName() {
        return "MaxPeriodicity";
    }

    @Override
    public String operator() {
        return "@@";
    }

    @Override
    public OperationAppliance getAppliance() {
        //default
        return OperationAppliance.PACKET;
    }

    @Override
    public String droolsDefinition() {
        //operand 0 - packet
        HPacketRuleOperand hPacketOperand = (HPacketRuleOperand) this.operands.get(0);
        //operand 1 - maximum periodicity
        long maxAwaitMillis = (long) Double.parseDouble(this.operands.get(1).droolsDefinition());
        //if this difference is greater than the treshold than actions are executed
        return "(LastReceivedPacket(packetId == " + hPacketOperand.getHPacketId() + ", $timeDiff:(System.currentTimeMillis() - lastReceivedDateMillis)) and eval($timeDiff >= " + maxAwaitMillis + "L))";
    }

    @Override
    public HPacketFieldType[] supportedFieldTypes() {
        return new HPacketFieldType[]{HPacketFieldType.INTEGER};
    }

}
