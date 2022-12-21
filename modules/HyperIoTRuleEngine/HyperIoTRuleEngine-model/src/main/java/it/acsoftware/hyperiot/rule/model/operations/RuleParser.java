package it.acsoftware.hyperiot.rule.model.operations;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }

    public RuleParser(StreamTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        //invoking on one it's the same because
        this.availableOperations = RuleOperation.getDefinedOperations();
    }

    public RuleNode parse(Set<Long> hPacketIds) throws InstantiationException, IllegalAccessException, IOException {
        RuleNode result = null;
        this.hPacketIds = hPacketIds;
        tokenizer.nextToken();
        while (tokenizer.ttype != StreamTokenizer.TT_EOF) {
            result = this.parseExpression(result);
        }
        return result;
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
                    String hPacketFieldName = tokenizer.sval.substring(tokenizer.sval.indexOf(".") + 1);
                    long hPacketId = Long.parseLong(tokenizer.sval.substring(0, tokenizer.sval.indexOf(".")));
                    hPacketIds.add(hPacketId);
                    result = new HPacketFieldRuleOperand(hPacketFieldName, hPacketId);
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
                    if(operation.numOperands() == 1){
                        tokenizer.nextToken();
                    } else {
                        for (int j = 1; j < operation.numOperands(); j++) {
                            //go ahead only if operator is a string word
                            if(tokenizer.ttype == StreamTokenizer.TT_WORD)
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
