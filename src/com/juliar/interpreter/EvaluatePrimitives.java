package com.juliar.interpreter;

import com.juliar.nodes.*;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by donreamey on 3/28/17.
 */
public class EvaluatePrimitives {
    static public List<Node> evalPrimitives(Node n, ActivationFrame activationFrame) {
        String functionName = ((FinalNode)n.getInstructions().get(0)).dataString();
        Node argumentNode = n.getInstructions().get(2);

        printLine(activationFrame, functionName, argumentNode);
        String data = fileOpen(functionName, argumentNode);

        FinalNode finalNode = new FinalNode();
        finalNode.setDataString( data );
        finalNode.setVariableTypeByIntegralType( IntegralType.jstring );

        activationFrame.returnNode = finalNode;
        return new ArrayList<>();
    }

    private static String fileOpen(String functionName, Node argumentNode) {
        if (functionName.equals( "fileOpen")){
            if (argumentNode instanceof FinalNode) {
                FinalNode finalNode = (FinalNode) argumentNode;
                return com.juliar.pal.Primitives.sys_file_open(finalNode.dataString());
            }
        }

        return "";
    }

    private static void printLine(ActivationFrame activationFrame, String functionName, Node argumentNode) {
        if (functionName.equals( "printLine")) {
            FinalNode finalNode = null;

            if (argumentNode instanceof VariableNode) {
                VariableNode variableName = (VariableNode) argumentNode;
                Object variable = activationFrame.variableSet.get(variableName.variableName);

                if (variable != null && variable instanceof PrimitiveNode) {
                    PrimitiveNode primitiveNode = (PrimitiveNode) variable;
                    finalNode = (FinalNode) primitiveNode.getInstructions().get(0);
                }

                if (variable instanceof IntegralTypeNode) {
                    finalNode = (FinalNode) ((IntegralTypeNode) variable).getInstructions().get(0);
                }

                if (variable instanceof BooleanNode){
                    finalNode = (FinalNode) ((BooleanNode) variable).getInstructions().get(0);
                }

                if (variable instanceof FinalNode){
                    finalNode = (FinalNode)variable;
                }
            }

            if (argumentNode instanceof IntegralTypeNode) {
                finalNode = (FinalNode) argumentNode.getInstructions().get(0);
            }

            assert finalNode != null : "the finalNode was null";
            if (finalNode.dataString() != null) {
                com.juliar.pal.Primitives.sys_print_line(finalNode.dataString());
            }
        }
    }

}
