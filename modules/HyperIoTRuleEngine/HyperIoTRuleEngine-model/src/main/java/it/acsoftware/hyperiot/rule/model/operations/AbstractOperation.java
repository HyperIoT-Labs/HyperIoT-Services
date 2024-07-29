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

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Aristide Cittadino Abstract rule operation which maps every possibile
 * operation expression
 */
public abstract class AbstractOperation implements RuleOperation, RuleNode {

    public static final HPacketFieldType[] ALL_FIELD_TYPES = new HPacketFieldType[]{
            HPacketFieldType.BYTE,
            HPacketFieldType.BOOLEAN,
            HPacketFieldType.DATE,
            HPacketFieldType.FILE,
            HPacketFieldType.DOUBLE,
            HPacketFieldType.FLOAT,
            HPacketFieldType.INTEGER,
            HPacketFieldType.OBJECT,
            HPacketFieldType.TEXT,
            HPacketFieldType.TIMESTAMP
    };

    protected List<RuleNode> operands;

    public AbstractOperation() {
        super();
        this.operands = new ArrayList<>();
    }


    /**
     * Define operation operands
     */
    @Override
    public void defineOperands(RuleNode... operands) {
        if (operands.length > this.numOperands())
            throw new HyperIoTRuntimeException("Too much operands for operation!");
        this.operands.addAll(Arrays.asList(operands));
    }

    /**
     * Boolean which says if the current operation expects a single value or an
     * expression
     */
    @Override
    public boolean needsExpr() {
        return false;
    }

    /**
     * return the string rappresentaion of the rule
     */
    @Override
    public abstract String getDefinition();

    @Override
    public OperationAppliance getAppliance() {
        return OperationAppliance.FIELD;
    }

    @Override
    public HPacketFieldType[] supportedFieldTypes() {
        return ALL_FIELD_TYPES;
    }
}
