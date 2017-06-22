package com.juliar.symboltable;

import com.juliar.nodes.Node;
import com.juliar.nodes.UserDefinedTypeNode;
import com.juliar.nodes.VariableNode;
import com.juliar.vistor.Visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Created by donreamey on 1/9/17.
 *
 * root
 *   |
 *    - main
 *   |  |
 *   |  |- z1
 *   |  |
 *   |  |- X1
 *   |
 *    - test1
 *   |  |
 *   |  |- s
 *   |
 *    - foo1
 *   |
 *    - foo2
 */
public class SymbolTable {
    private HashMap< String, SymbolTableNode> scopeHash = new HashMap<>();
    private static SymbolTable symbolTable;
    private static Stack<String> currentScope = new Stack<>();
    private Visitor visitor;

    static public SymbolTable createSymbolTable(Visitor v) {
        if (symbolTable == null) {
            symbolTable = new SymbolTable( v );
        }
        return symbolTable;
    }

    static public void clearSymbolTable(){
        currentScope.empty();
        symbolTable = null;
        symbolTable = null;
    }

    private SymbolTable(){
    }

    private SymbolTable( Visitor v){
        this();
        visitor = v;
    }

    public void addLevel(String level) {
        SymbolTableNode node = new SymbolTableNode();
        node.levelNode = level;

        if (scopeHash.containsKey(level)) {
            visitor.addError( "identifier " + level + " already exist" );
        } else {
            currentScope.push(level);
            scopeHash.put(level, node);
        }
    }

    public void popScope(){
        currentScope.pop();
    }

    public void addChild(Node child) {
        SymbolTableNode node = scopeHash.get(currentScope.peek());

        if (child instanceof VariableNode) {

            if (node.children.stream()
                    .filter(f -> f instanceof VariableNode && ((VariableNode) f)
                            .variableName.equals(((VariableNode) child)
                                    .variableName))
                    .count() > 0) {
                visitor
                        .addError("identifier " + ((VariableNode) child)
                                .variableName + " already exist");
            }

            /*
            if (node.children.stream()
                    .filter(f -> ((VariableNode) f).variableName.equals(((VariableNode) child).variableName)).count() > 0) {
                visitor.addError( "identifier " + ((VariableNode) child).variableName + " already exist" );
             }*/

        } else if (child instanceof UserDefinedTypeNode){

            if (node.children.stream()
                    .filter( f -> f instanceof UserDefinedTypeNode)
                    .filter(f -> ((UserDefinedTypeNode) f)
                            .getTypeName()
                            .equals(((UserDefinedTypeNode) child)
                                    .getTypeName()))
                    .count() > 0) {
                visitor
                        .addError("identifier " + ((UserDefinedTypeNode) child)
                                .getTypeName() + " already exist");
            }

        }


        node.children.add ( child );
        //scopeHash.get(currentScope.peek()).children.add(child);
    }


    public Node getNode(Node child){
        Node returnNode = null;

        if (child instanceof VariableNode){

            Stack<String> tempScope = new Stack<>();
            tempScope.push( currentScope.pop() );

            while ( !currentScope.empty() ) {

                if (child instanceof VariableNode) {

                    SymbolTableNode node = scopeHash.get(currentScope.peek());
                    if (node.children.stream()
                            .filter(f -> f instanceof VariableNode)
                            .filter(f -> ((VariableNode) f).variableName.equals(((VariableNode) child).variableName)).count() == 1) {

                        returnNode = node.children
                                .stream()
                                .filter(f -> ((VariableNode) f).variableName.equals(((VariableNode) child).variableName))
                                .findFirst().get();

                        if (returnNode != null) {
                            break;
                        }
                    }
                }
                else if (child instanceof UserDefinedTypeNode){
                    SymbolTableNode node = scopeHash.get(currentScope.peek());

                    if (node.children.stream()
                            .filter(f -> f instanceof UserDefinedTypeNode)
                            .filter(f -> ((UserDefinedTypeNode) f).getTypeName().equals(((UserDefinedTypeNode) child).getTypeName())).count() == 1) {

                        returnNode = node.children
                                .stream()
                                .filter(f -> f instanceof UserDefinedTypeNode)
                                .filter(f -> ((UserDefinedTypeNode) f).getTypeName().equals(((UserDefinedTypeNode) child).getTypeName()))
                                .findFirst().get();

                        if (returnNode != null) {
                            break;
                        }
                    }
                }

               while (!tempScope.empty()){
                    currentScope.push (tempScope.pop());
               }
            }
        }

        if (returnNode == null){
            throw new RuntimeException( "unable to find variable -" + ((VariableNode)child).variableName + "in scope for reassignment");
        }

        return returnNode;
    }

    public boolean doesChildExistAtScope(Node child){
        Stack<String> tempScope = new Stack<>();
        tempScope.push ( currentScope.pop() );
        boolean doesExist = false;

        while ( !currentScope.empty() ) {
            if (child instanceof  VariableNode) {
                doesExist = scopeHash.get(tempScope.peek())
                        .children.stream()
                        .filter(f -> f instanceof VariableNode)
                        .filter(t -> ((VariableNode) t).variableName.equals(((VariableNode) child).variableName)).count() == 1;

                if (doesExist) {
                    break;
                }
            } else if (child instanceof UserDefinedTypeNode){
                doesExist = scopeHash.get(tempScope.peek())
                        .children.stream()
                        .filter(f -> f instanceof UserDefinedTypeNode)
                        .filter(t -> ((VariableNode) t).variableName.equals(((UserDefinedTypeNode) child).getTypeName())).count() == 1;

                if (doesExist) {
                    break;
                }
            }

            tempScope.push ( currentScope.pop() );
        }


        while(!tempScope.empty()){
            currentScope.push ( tempScope.pop());
        }

        return doesExist;
    }


    class SymbolTableNode {
        public String levelNode;
        public List<Node> children = new ArrayList<>();
    }
}
