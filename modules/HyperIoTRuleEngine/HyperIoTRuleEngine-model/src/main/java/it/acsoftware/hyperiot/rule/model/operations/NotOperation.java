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
 * 
 * @author Aristide Cittadino Not operator
 */
@Component(service = RuleOperation.class)
public class NotOperation extends AbstractOperation implements RuleNode, RuleOperator {

	@Override
	public void defineOperands(RuleNode... operands) {
		if (operands.length != 1)
			throw new RuntimeException("And operation must have exactly 1 operands");
		super.defineOperands(operands);
	}

	/**
	 * return the string rappresentaion of the rule
	 */
	@Override
	public String getDefinition() {
		StringBuilder sb = new StringBuilder();
		sb.append("NOT ").append(this.operands.get(0).getDefinition()).append("");
		return sb.toString();
	}

	/**
	 * returns the token string which identifies the operation
	 */
	@Override
	public String operator() {
		return "NOT";
	}

	/**
	 * Returns the operation name
	 */
	@Override
	public String getName() {
		return "Not";
	}

	@Override
	public boolean needsExpr() {
		return false;
	}

	/**
	 * returns the number of operands required by this operation
	 */
	@Override
	public int numOperands() {
		return 1;
	}

	@Override
	public String droolsDefinition() {
		return "eval(!(" + operands.get(0).droolsDefinition() + "))";
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
