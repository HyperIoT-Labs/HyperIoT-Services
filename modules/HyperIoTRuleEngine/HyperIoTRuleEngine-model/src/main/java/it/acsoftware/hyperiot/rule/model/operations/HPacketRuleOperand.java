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
 * Class for retrieving values from the concrete packet (nested fileds declared by the user)
 */
public class HPacketRuleOperand extends AbstractOperation implements RuleNode {
	/**
	 * Rules are multi packet, so each HPacketField needs to be related to its HPacket
	 */
	private long hPacketId;

	/**
	 *
	 * @id
	 */
	public HPacketRuleOperand(long id) {
		super();
		this.hPacketId = id;
	}

	public long getHPacketId() {
		return hPacketId;
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
		return String.valueOf(hPacketId);
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
		return "packet_" + getHPacketId();
	}
}
