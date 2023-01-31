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

/**
 * 
 * @author Aristide Cittadino 
 * Operand for retrieving a packet field value by getter methods of HPacket class
 */
public class HPacketClassFieldRuleOperand extends AbstractOperation implements RuleNode {
	private String value;

	/**
	 * 
	 * @param value
	 */
	public HPacketClassFieldRuleOperand(String value) {
		super();
		this.value = value;
	}

	/**
	 * 
	 */
	@Override
	public String getName() {
		return null;
	}

	/**
	 * 
	 */
	@Override
	public String operator() {
		return null;
	}

	/**
	 * 
	 */
	@Override
	public String getDefinition() {
		return value.substring(0,1).toUpperCase()+value.substring(1);
	}

	/**
	 * 
	 */
	@Override
	public int numOperands() {
		return 1;
	}

	/**
	 * 
	 */
	@Override
	public String droolsDefinition() {
		return "packet.get"+this.getDefinition() + "()";
	}
}
