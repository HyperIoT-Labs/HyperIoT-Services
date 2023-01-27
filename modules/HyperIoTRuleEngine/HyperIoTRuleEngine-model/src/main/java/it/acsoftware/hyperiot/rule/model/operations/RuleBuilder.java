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
 * @author Aristide Cittadino
 * Rule Builder in order to create rules from code
 */
public class RuleBuilder {
    private RuleNode rule;

    public static RuleBuilder newRule() {
        return new RuleBuilder();
    }

    public static RuleBuilder newRule(RuleNode init) {
        RuleBuilder rb = new RuleBuilder();
        rb.rule = init;
        return rb;
    }

    public RuleBuilder field(String name, long hPacketId) {
        rule = new HPacketFieldRuleOperand(name, hPacketId);
        return this;
    }

    public RuleBuilder classField(String name) {
        rule = new HPacketClassFieldRuleOperand(name.toLowerCase());
        return this;
    }

    public RuleBuilder equalTo(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        EqualTo op = new EqualTo();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder equalTo(String value) {
        StringRuleOperand valueOperand = new StringRuleOperand(value);
        EqualTo op = new EqualTo();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder notEqualTo(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        NotEqualTo op = new NotEqualTo();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder notEqualTo(String value) {
        StringRuleOperand valueOperand = new StringRuleOperand(value);
        NotEqualTo op = new NotEqualTo();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder greaterOrEqualsTo(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        GreaterOrEqualThan op = new GreaterOrEqualThan();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder greaterThan(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        GreaterThan op = new GreaterThan();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }


    public RuleBuilder lowerOrEqualThan(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        LowerOrEqualThan op = new LowerOrEqualThan();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }


    public RuleBuilder lowerThan(Number value) {
        NumberRuleOperand valueOperand = new NumberRuleOperand(value);
        LowerThan op = new LowerThan();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder like(String value) {
        StringRuleOperand valueOperand = new StringRuleOperand(value);
        Like op = new Like();
        op.defineOperands(rule, valueOperand);
        rule = op;
        return this;
    }

    public RuleBuilder isTrue() {
        IsTrue op = new IsTrue();
        op.defineOperands(rule);
        rule = op;
        return this;
    }

    public RuleBuilder isFalse() {
        IsFalse op = new IsFalse();
        op.defineOperands(rule);
        rule = op;
        return this;
    }

    public RuleBuilder and(RuleNode rightCondition) {
        AndOperation andOp = new AndOperation();
        andOp.defineOperands(this.rule, rightCondition);
        this.rule = andOp;
        return this;
    }

    public RuleBuilder Or(RuleNode rightCondition) {
        OrOperation orOp = new OrOperation();
        orOp.defineOperands(this.rule, rightCondition);
        this.rule = orOp;
        return this;
    }

    public RuleBuilder not() {
        NotOperation op = new NotOperation();
        op.defineOperands(this.rule);
        this.rule = op;
        return this;
    }

    public RuleBuilder parenthesis() {
        ParenthesisNode pNode = new ParenthesisNode();
        pNode.defineOperands(this.rule);
        this.rule = pNode;
        return this;
    }

    public RuleNode build() {
        return this.rule;
    }

}
