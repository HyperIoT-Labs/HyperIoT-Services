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

package it.acsoftware.hyperiot.rule.model.field;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.rule.model.operations.RuleNode;
import org.osgi.service.component.annotations.Component;


@Component(service = FieldFunction.class)
public class ExtractYearFunction extends AbstractFieldFunction implements FieldFunction {
    //just one param
    RuleNode[] params = new RuleNode[1];

    @Override
    public void defineOperands(RuleNode... operands) {
        if (operands.length <= 0 || operands.length > 1)
            throw new HyperIoTRuntimeException("year function accepts only one param!");
        this.params[0] = operands[0];
    }

    @Override
    public String getName() {
        return "year";
    }

    @Override
    public int numOperands() {
        return 1;
    }

    @Override
    public RuleNode[] getOperands() {
        return params;
    }

    @Override
    public HPacketFieldType[] getFieldTypeAppliance() {
        return new HPacketFieldType[]{HPacketFieldType.TIMESTAMP};
    }

    @Override
    public String droolsDefinition() {
        return "LocalDate.ofInstant(Instant.ofEpochMilli((Long)" + this.params[0].droolsDefinition() + "), ZoneId.systemDefault()).getYear()";
    }
}
