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

/**
 * 
 * @author Aristide Cittadino
 * Number operand
 */
public class NumberRuleOperand extends AbstractOperation implements RuleNode {

	private Number number;

	public NumberRuleOperand(Number number) {
		super();
		this.number = number;
	}

	/**
	 * return the string rappresentaion of the rule
	 */
	@Override
	public String getDefinition() {
		return number.toString();
	}
	
	/**
	 * returns the token string which identifies the operation
	 */
	@Override
	public String operator() {
		return null;
	}

	/**
	 * Returns the operation name
	 */
	@Override
	public String getName() {
		return null;
	}

	/**
	 * returns the number of operands required by this operation
	 */
	@Override
	public int numOperands() {
		return 0;
	}
	
	@Override
	public String droolsDefinition() {
		return number.toString();
	}

	@Override
	public HPacketFieldType[] supportedFieldTypes() {
		return new HPacketFieldType[]{HPacketFieldType.INTEGER,HPacketFieldType.FLOAT,HPacketFieldType.DOUBLE};
	}
	
}
