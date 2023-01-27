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

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author Aristide Cittadino
 * Interface which maps the concept of operation inside an expression
 */
public interface RuleOperation {
	static Logger logger = LoggerFactory.getLogger(RuleOperation.class.getName());
	/**
	 *
	 * @return Operation Name
	 */
	String getName();
	/**
	 *
	 * @return Operand string rapresentation
	 */
	String operator();
	/**
	 *
	 * @return Number of operands
	 */
	int numOperands();
	/**
	 *
	 * @return true if it needs an expression or a value
	 */
	boolean needsExpr();

	/**
	 *
	 * @return the OSGi registerd operations
	 */
	public static List<RuleOperation> getDefinedOperations() {
		List<RuleOperation> operations = new ArrayList<>();
		try {
			BundleContext ctx = HyperIoTUtil.getBundleContext(AbstractOperation.class);
			Collection<ServiceReference<RuleOperation>> references = ctx
					.getServiceReferences(RuleOperation.class, null);
			references.stream().forEach(
					reference -> operations.add(ctx.getService(reference)));
		} catch (Exception e) {
			logger.error( e.getMessage(), e);
		}
		return operations;
	}
}
