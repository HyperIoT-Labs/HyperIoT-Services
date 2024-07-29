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

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.rule.model.operations.RuleNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FieldFunction extends RuleNode {
    Logger log = LoggerFactory.getLogger(FieldFunction.class);

    String getName();

    HPacketFieldType[] getFieldTypeAppliance();

    String droolsDefinition();

    RuleNode[] getOperands();

    int numOperands();

    /**
     * @return the OSGi registerd operations
     */
    static List<FieldFunction> getDefinedFieldFunctions() {
        List<FieldFunction> operations = new ArrayList<>();
        try {
            BundleContext ctx = HyperIoTUtil.getBundleContext(FieldFunction.class);
            Collection<ServiceReference<FieldFunction>> references = ctx
                    .getServiceReferences(FieldFunction.class, null);
            references.stream().forEach(
                    reference -> operations.add(ctx.getService(reference)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return operations;
    }

    static Optional<FieldFunction> findFieldFunction(String name) {
        return getDefinedFieldFunctions().stream().filter(fieldFunction -> fieldFunction.getName().equalsIgnoreCase(name)).findAny();
    }
}
