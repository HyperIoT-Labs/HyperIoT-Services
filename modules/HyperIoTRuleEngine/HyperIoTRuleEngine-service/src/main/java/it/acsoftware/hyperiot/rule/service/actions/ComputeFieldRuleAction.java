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

package it.acsoftware.hyperiot.rule.service.actions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.bigmath.functions.bigdecimalmath.BigDecimalMathFunctions;
import com.ezylang.evalex.bigmath.operators.bigdecimalmath.BigDecimalMathOperators;
import com.ezylang.evalex.config.ExpressionConfiguration;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldMultiplicity;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.rule.model.actions.EnrichmentRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.DiscriminatorValue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component(service = RuleAction.class, immediate = true, property = {"it.acsoftware.hyperiot.rule.action.type=ENRICHMENT"})
@DiscriminatorValue("rule.action.name.computeField")
public class ComputeFieldRuleAction extends EnrichmentRuleAction {
    private static final Logger log = LoggerFactory.getLogger(ComputeFieldRuleAction.class.getName());

    private long outputFieldId;
    private String outputFieldName;
    private String formula;

    public ComputeFieldRuleAction() {
        super();
    }

    @Override
    public String droolsDefinition() {
        log.debug("In ComputeFieldRuleAction.droolsDefinition");
        StringBuilder sb = new StringBuilder();
        sb.append("ComputeFieldRuleAction computeField = new ComputeFieldRuleAction();\n        ");
        sb.append("ComputeFieldRuleAction.computeField").append("(")
                .append(this.getDroolsPacketNameVariable())
                .append(", ")
                .append("\"" + formula + "\"")
                .append(", ")
                .append(outputFieldId)
                .append(", \"")
                .append(outputFieldName)
                .append("\")");
        log.debug("partial Drool generated: {}", sb);
        return sb.toString();
    }

    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String fieldName) {
        this.outputFieldName = fieldName;
    }

    public long getOutputFieldId() {
        return outputFieldId;
    }

    public void setOutputFieldId(long fieldId) {
        this.outputFieldId = fieldId;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public static void computeField(HPacket packet, String formula, long outputFieldId, String outputFieldName) {
        Object value = doComputeField(packet, formula);
        HPacketField outputField = packet
                .getFields().stream()
                .filter(f -> f.getId() == outputFieldId)
                .findFirst().orElse(null);

        if (outputField == null) {
            try {
                outputField = new HPacketField();
                outputField.setId(outputFieldId);
                outputField.setName(outputFieldName);
                outputField.setType(HPacketFieldType.DOUBLE);
                outputField.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
                outputField.setCategoryIds(new long[0]);
                outputField.setTagIds(new long[0]);
                outputField.setInnerFields(new HashSet<>());
                outputField.setPacket(packet);
                packet.getFields().add(outputField);
                outputField.setValue(value);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
                throw e;
            }

        }
    }

    private static double doComputeField(HPacket packet, String formula) {
        try {
            ExpressionConfiguration configuration =
                    ExpressionConfiguration.defaultConfiguration()
                            .withAdditionalFunctions(BigDecimalMathFunctions.allFunctions())
                            .withAdditionalOperators(BigDecimalMathOperators.allOperators());
            Expression expression = new Expression(formula, configuration);
            Map<String,Double> values = new HashMap<>();
            packet.getFieldsMap().forEach((key, value) -> {
                double doubleValue = (value != null && !value.getFieldValue().toString().isEmpty()) ? Double.parseDouble(value.getFieldValue().toString()) : 0d;
                values.put(key, doubleValue);
            });
            expression.withValues(values);
            return expression.evaluate().getNumberValue().doubleValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }
}
