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

import it.acsoftware.hyperiot.rule.model.field.FieldFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Aristide Cittadino Rule Parser
 */
public class RuleParser {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(RuleParser.class.getName());

    private StreamTokenizer tokenizer;
    private List<RuleOperation> availableOperations;
    private Set<Long> hPacketIds;

    public RuleParser(StreamTokenizer tokenizer, List<RuleOperation> availableOperations) {
        this.tokenizer = tokenizer;
        this.availableOperations = availableOperations;
        this.hPacketIds = new HashSet<>();
    }

    public RuleParser(StreamTokenizer tokenizer) {
        this(tokenizer, RuleOperation.getDefinedOperations());
    }

    public RootRuleNode parse() throws InstantiationException, IllegalAccessException, IOException {
        RuleNode result = null;
        tokenizer.nextToken();
        while (tokenizer.ttype != StreamTokenizer.TT_EOF) {
            result = this.parseExpression(result);
        }
        return new RootRuleNode(this.hPacketIds, result);
    }

    private RuleNode parsePrimary(boolean parsePacketField)
            throws IOException, InstantiationException, IllegalAccessException {
        RuleNode result = null;
        if (tokenizer.ttype == '(') {
            tokenizer.nextToken();
            while (tokenizer.ttype != ')' && tokenizer.ttype != StreamTokenizer.TT_EOF) {
                result = parseExpression(result);
            }
            if (tokenizer.ttype != ')') {
                throw new RuntimeException(
                        ") expected, got " + tokenizer.ttype + "/" + tokenizer.sval);
            }
            RuleNode parenthesisNode = new ParenthesisNode();
            parenthesisNode.defineOperands(result);
            result = parenthesisNode;
        } else if (tokenizer.ttype == '"' || tokenizer.ttype == '\'') {
            result = checkParseOperator(tokenizer.sval);

            if (result == null) {
                if (parsePacketField) {
                    //parsing hpacket.field
                    Long hPacketId = null;
                    if (tokenizer.sval.indexOf(".") > 0) {
                        hPacketId = Long.parseLong(tokenizer.sval.substring(0, tokenizer.sval.indexOf(".")));
                        String hPacketFieldName = tokenizer.sval.substring(tokenizer.sval.indexOf(".") + 1);
                        result = parseHPacketField(hPacketFieldName, hPacketId);
                    } else {
                        //parsing hpacketId only
                        hPacketId = Long.parseLong(tokenizer.sval);
                        result = new HPacketRuleOperand(hPacketId);
                    }
                    //calculate automatically all packet ids related to a specific rule
                    if (hPacketId != null) {
                        hPacketIds.add(hPacketId);
                    }
                } else
                    result = new StringRuleOperand(tokenizer.sval);
            } else
                // returning directly in order to continue parsing
                return result;
        } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
            result = new NumberRuleOperand(tokenizer.nval);
        } else if (tokenizer.ttype != -4 /* TT_NOTHING */) {
            throw new RuntimeException(
                    "Unrecognized token: " + tokenizer.ttype + "/" + tokenizer.sval);
        }
        tokenizer.nextToken();
        return result;
    }

    /**
     * This methods parses hpacket field or function applied to fields.
     * <packetId>.<fieldId>  or <packetId>.function(<hpacketField>,....)
     * example 123.timestamp or 123.day(timestamp)
     *
     * @param packetFieldStr
     * @param hPacketId
     * @return
     */
    private RuleNode parseHPacketField(String packetFieldStr, long hPacketId) throws IOException {
        StreamTokenizer streamTokenizer = new StreamTokenizer(new StringReader(packetFieldStr));
        StringBuilder fieldOrFunction = new StringBuilder();

        while (streamTokenizer.ttype != StreamTokenizer.TT_EOF && streamTokenizer.ttype != '(') {
            if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER)
                fieldOrFunction.append(streamTokenizer.nval);
            else if (streamTokenizer.ttype == StreamTokenizer.TT_WORD)
                fieldOrFunction.append(streamTokenizer.sval);

            streamTokenizer.nextToken();
        }
        //packetFieldStr is a field name so we return directly the HPacketFieldRuleOperand
        if (streamTokenizer.ttype != ('(')) {
            //is a field not a function
            return new HPacketFieldRuleOperand(packetFieldStr, hPacketId);
        }

        //otherwise there's a defined function, we must parse it
        String fieldFunctionName = fieldOrFunction.substring(0, fieldOrFunction.length());
        StringBuilder functionParams = new StringBuilder();
        //parsing the final part of the string
        int parenthesisCount = 0;
        while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
            if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER)
                functionParams.append(streamTokenizer.nval);
            else if (streamTokenizer.ttype == '(')
                parenthesisCount++;
            else if (streamTokenizer.ttype == ')')
                parenthesisCount--;
            else if (streamTokenizer.ttype == StreamTokenizer.TT_WORD)
                functionParams.append(streamTokenizer.sval);
            else
                functionParams.append((char) streamTokenizer.ttype);
            streamTokenizer.nextToken();
        }

        //find the closing ")"
        if (parenthesisCount != 0)
            throw new RuntimeException("Wrong field function expression!");

        //calculate parameters splitted by ","
        String paramsString = functionParams.toString().trim().substring(0, functionParams.toString().length());
        String params[] = paramsString.split(",");
        //find a defined field function for the specified name
        Optional<FieldFunction> fieldFunction = FieldFunction.findFieldFunction(fieldFunctionName);
        if (fieldFunction.isPresent()) {
            FieldFunction f = fieldFunction.get();
            if (params.length != f.numOperands())
                throw new RuntimeException("Wrong number of arguments for function " + fieldFunctionName);
            //for each specified param we must create a RuleNode parsing it
            RuleNode[] operands = new RuleNode[f.numOperands()];
            for (int i = 0; i < params.length; i++) {
                operands[i] = parseHPacketField(params[i], hPacketId);
            }
            f.defineOperands(operands);
            return f;
        }
        throw new RuntimeException("No operations found with name: " + fieldFunctionName);
    }

    private RuleNode parseExpression(RuleNode left)
            throws IOException, InstantiationException, IllegalAccessException {
        if (left == null) {
            left = parsePrimary(true);
        }

        if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
            return parseOperation(left, tokenizer.sval);
        } else if (tokenizer.ttype != StreamTokenizer.TT_NUMBER
                && tokenizer.ttype != StreamTokenizer.TT_EOF && tokenizer.ttype != ')') {
            StringBuilder sb = new StringBuilder();
            while (tokenizer.ttype != StreamTokenizer.TT_WORD
                    && tokenizer.ttype != StreamTokenizer.TT_NUMBER && tokenizer.ttype != '"' && tokenizer.ttype != '\'') {
                char ch = (char) tokenizer.ttype;
                sb.append(ch);
                tokenizer.nextToken();
            }
            return parseOperation(left, sb.toString());
        }
        return left;
    }

    private RuleNode parseOperation(RuleNode firstOperand, String val)
            throws IOException, InstantiationException, IllegalAccessException {
        for (int i = 0; i < availableOperations.size(); i++) {
            if (availableOperations.get(i).operator().equals(val)) {
                RuleOperation operation = availableOperations.get(i).getClass().newInstance();
                RuleNode nr = (RuleNode) operation;
                if (operation.needsExpr()) {
                    tokenizer.nextToken();
                    nr.defineOperands(firstOperand, parseExpression(null));
                } else {
                    RuleNode[] operands = new RuleNode[operation.numOperands()];
                    operands[0] = firstOperand;
                    //if is unary, then go ahead
                    if (operation.numOperands() == 1) {
                        tokenizer.nextToken();
                    } else {
                        for (int j = 1; j < operation.numOperands(); j++) {
                            //go ahead only if operator is a string word
                            if (tokenizer.ttype == StreamTokenizer.TT_WORD)
                                tokenizer.nextToken();
                            operands[j] = parsePrimary(false);
                        }
                    }
                    nr.defineOperands(operands);
                }
                return nr;
            }
        }
        return null;
    }

    private RuleNode checkParseOperator(String val)
            throws IOException, InstantiationException, IllegalAccessException {
        for (int i = 0; i < availableOperations.size(); i++) {
            if (availableOperations.get(i).operator().equals(val)) {
                RuleOperation operation = availableOperations.get(i).getClass().newInstance();
                if (operation instanceof RuleOperator) {
                    RuleNode nr = (RuleNode) operation;
                    tokenizer.nextToken();
                    nr.defineOperands(parseExpression(null));
                    return nr;
                }
            }
        }
        return null;
    }
}
