package com.juliar.interpreter;

import com.juliar.codegenerator.InstructionInvocation;
import com.juliar.errors.JuliarLogger;
import com.juliar.nodes.*;

import java.util.*;

/**
 * Created by Don Reamey on 1/8/17.
 */
public class Interpreter {
    private final static String mainFunctionName = "main";
    private static ActivationFrameStack activationFrameStack = new ActivationFrameStack();
    private Stack<Node> returnValueStack = new Stack<>();
    private Map<String, Node> functionNodeMap;
    private Map<NodeType, Evaluate> functionMap = new HashMap<>();
    public Stack<Node> operatorStack = new Stack<Node>();
    public Stack<Node> operandStack = new Stack<Node>();
    private operatorState operatorPrecedenceTable = new operatorState();


    private enum SrEval{
        Shift,
        Reduce,
        Error,
        End,
    }

    private class operatorState {
        public SrEval shiftReduceOperationTable[][] = {
      /*              PLUS            MINUS           MULTILY        DIVIDE         EXPONENT       FUNCTION       p             c              COMMA          LEFTPAREN      RIGHTPAREN     SEMI            EQUAL         EQEQ           BIAND         BIOR          */
      /*0 PLUS     */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Reduce, SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*1 MINUS    */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Reduce, SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*2 MULTIPLY */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Reduce, SrEval.Reduce, SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Reduce, SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*3 DIVIDE   */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Reduce, SrEval.Reduce, SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Reduce, SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*4 EXPONENT */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Reduce, SrEval.Reduce, SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Reduce, SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*5 FUNCTION */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*6 p        */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*7 c        */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*8 COMMA    */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Reduce, SrEval.Reduce, SrEval.Reduce, SrEval.Reduce, SrEval.Reduce,SrEval.Reduce, SrEval.Error,  SrEval.Reduce, SrEval.Reduce, SrEval.Error,   SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
      /*9 LPAREN   */ {SrEval.Shift,   SrEval.Shift,   SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Error,   SrEval.Error, SrEval.Shift,  SrEval.Error, SrEval.Error },
     /*10 RPAREN   */ {SrEval.Reduce,  SrEval.Reduce,  SrEval.Reduce, SrEval.Reduce, SrEval.Reduce, SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Reduce, SrEval.Reduce,  SrEval.Error, SrEval.Reduce, SrEval.Error, SrEval.Error },
     /*11 SEMI     */ {SrEval.Shift,   SrEval.Shift,   SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Error,  SrEval.Shift,  SrEval.Error,  SrEval.End,     SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
     /*12 EQUAL    */ {SrEval.Shift,   SrEval.Shift,   SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift, SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,  SrEval.Shift,   SrEval.Error, SrEval.Error,  SrEval.Error, SrEval.Error },
     /*13 EQEQ     */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Error,   SrEval.Error, SrEval.Error,  SrEval.Reduce, SrEval.Reduce},
     /*14 BIAND    */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Error,   SrEval.Error, SrEval.Error,  SrEval.Reduce, SrEval.Reduce },
     /*15 BIOR     */ {SrEval.Error,   SrEval.Error,   SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error,  SrEval.Error, SrEval.Error,  SrEval.Error,  SrEval.Shift,  SrEval.Reduce, SrEval.Error,   SrEval.Error, SrEval.Error,  SrEval.Reduce, SrEval.Reduce },
        };
    }

/*
    private class operatorBinding {
        public
    }
*/

    public Interpreter(InstructionInvocation invocation){
        try {
            EvaluateAssignments.create( this );
            List<Node> inst = invocation.getInstructionList();
            activationFrameStack.push( new ActivationFrame("compliationUnit"));

            functionNodeMap = invocation.getFunctionNodeMap();

            functionMap.put( NodeType.VariableReassignmentType   , ( EvaluateAssignments::evalReassignment  ));
            functionMap.put( NodeType.AssignmentType             , ( EvaluateAssignments::evalAssignment    ));
            functionMap.put( NodeType.PrimitiveType              , ( EvaluatePrimitives::evalPrimitives     ));
            functionMap.put( NodeType.CompliationUnitType        , ((n, activationFrame, callback )-> evalCompilationUnit()    ));
            functionMap.put( NodeType.AddType                    , ((n, activationFrame, callback  )-> evalAdd()                ));
            functionMap.put( NodeType.CommandType                , ((n, activationFrame, callback  )-> evalCommand(n)           ));
            functionMap.put( NodeType.SummationType              , ((n, activationFrame, callback  )-> evalSummation()          ));
            functionMap.put( NodeType.FunctionaCallType          , ((n, activationFrame, callback  )-> evalFunctionCall(n)      ));
            functionMap.put( NodeType.FunctionDeclType           , ((n, activationFrame, callback  )-> evalFunctionDecl(n)      ));
            functionMap.put( NodeType.VariableType               , ((n, activationFrame, callback  )-> evalActivationFrame(n)   ));
            functionMap.put( NodeType.BinaryType                 , ((n, activationFrame, callback  )-> evalBinaryNode(n)        ));
            functionMap.put( NodeType.StatementType              , ((n, activationFrame, callback  )-> evalStatement(n)         ));
            functionMap.put( NodeType.ExpressionType             , ((n, activationFrame, callback  )-> evalStatement(n)         ));
            functionMap.put( NodeType.FinalType                  , ((n, activationFrame, callback  )-> evalFinal()              ));
            functionMap.put( NodeType.EvaluatableType            ,       (( n, activationFrame,      callback  )-> evaluateEvauatable(n, activationFrame, callback) ));
            functionMap.put( NodeType.UserDefinedFunctionReferenceType , (( n, activationFrameStack, callback  )-> evalUserDefinedFunctionCall(n) ));
            functionMap.put( NodeType.AggregateType              , ( this::evaluateAggregate        ));
            functionMap.put( NodeType.ReturnValueType            , ( this::evalReturn               ));
            functionMap.put( NodeType.BooleanType                , ( this::evalBooleanNode          ));
            functionMap.put( NodeType.BooleanOperatorType        , ( this::evalBooleanOperator      ));
            functionMap.put( NodeType.IfExprType                 , ( this::evalIfStatement          ));
            functionMap.put( NodeType.WhileExpressionType        , ( this::evalWhileExpression      ));
            functionMap.put( NodeType.VariableDeclarationType    , ( this::evalVariableDeclration   ));

            //functionMap.put(NodeType.VariableDeclarationType, (n-> eval(n)));
            //functionMap.put(NodeType.ReturnValueType            , (n-> evalReassignment(n)      ));
            execute( inst );
        }
        catch( Exception ex){
            JuliarLogger.log(ex);
        }
    }

    private Boolean start = false;

    public List<Node> execute( List<Node> instructions ) {
        try {
            for (Node currentNode : instructions) {
                NodeType nodeType = currentNode.getType();
                if ( functionMap.containsKey( nodeType )) {
                    Evaluate evaluate = functionMap.get(  nodeType );
                    if (evaluate != null) {
                        List<Node> instructionsToExecute = evaluate.evaluate(currentNode, activationFrameStack.peek(), this);
                        if (instructionsToExecute != null && !instructionsToExecute.isEmpty()) {
                            execute(instructionsToExecute);
                        }
                    } else {
                        evalNull();
                    }
                }
            }
        }
        catch( Exception ex){
            JuliarLogger.log( ex );
            }

        return new ArrayList<>();
    }

    public void evaluateExpressionStack(){
        Node operator = null;
        if ( operatorStack.empty() ) {
            throw new RuntimeException("not operators on stack");
        }

        operator = operatorStack.pop();

        //TODO ensure the type is accurate.
        //assert false : "this needs to fail until i fix it";
        if ( operator.getType().equals(  NodeType.EqualEqualType) && operandStack.size() >= 2 ){
            NodeImpl r = ( NodeImpl ) operandStack.pop();
            NodeImpl l = ( NodeImpl ) operandStack.pop();

            ActivationFrame frame = activationFrameStack.peek();

            Object rReal = r.getRealValue( frame );
            Object lreal = l.getRealValue( frame );

            boolean equalEqualEval = rReal.equals( lreal );
            BooleanNode booleanNode = new BooleanNode();
            FinalNode finalNode = new FinalNode();
            finalNode.setDataString( equalEqualEval );
            booleanNode.addInst( finalNode );

            operandStack.push( booleanNode );
        }
    }

    public void pushOperandStack( Node node){
        operandStack.push ( node );
    }

    public void pushOperatorStack( Node node){
        if ( operatorStack.empty() ) {
            operatorStack.push(node);
        } else {
            FinalNode currentOperand = (FinalNode) operatorStack.peek();
            int currentNodePrecedence = getOperatorPrecedenceValue( currentOperand );
            int newNodePrecedence = getOperatorPrecedenceValue( (FinalNode) node);

            SrEval eval = operatorPrecedenceTable.shiftReduceOperationTable[currentNodePrecedence][newNodePrecedence];

            if (eval == SrEval.Shift ){
                operatorStack.push( node );
                return;
            }
            if ( eval == SrEval.Reduce ){
                if ( operandStack.size() >= 2 ){
                    Node rValue = operandStack.pop();
                    Node lValue = operandStack.pop();
                    Node operation = operatorStack.pop();

                    evalBooleanOperation( operation, lValue, rValue);
                    // Need to Evaluate the operation.
                    // Create a new node?
                }
            }
        }
    }

    private Boolean evalBooleanOperation( Node operation, Node lValue, Node rValue){
        if ( operation instanceof FinalNode){
            String opType = ((FinalNode) operation).dataString();
            String lvalueString;

            switch ( opType){
                case "==":

                    if (lValue instanceof FinalNode){
                        lvalueString = ((FinalNode) lValue).dataString();
                    } else if ( lValue instanceof LiteralNode){
                        LiteralNode literalNode = (LiteralNode)lValue.getInstructions().get (0);
                        // lvalueString = (LiteralNode)literalNode.getInstructions().get( 0 );
                    }

                    break;
                case ">=":
                    break;
                case "<=":
                    break;
                case "&&":
                    if (lValue instanceof FinalNode){
                        lvalueString = ((FinalNode) lValue).dataString();
                    } else if ( lValue instanceof LiteralNode){
                        LiteralNode literalNode = (LiteralNode)lValue.getInstructions().get (0);
                       // lvalueString = (LiteralNode)literalNode.getInstructions().get( 0 );
                    }

                    //if ( )
                    break;
                case "||":
                    break;

            }
        }

        return false;
    }

    private int getOperatorPrecedenceValue( FinalNode node){
        int precedence = -1;
        String dataString = node.dataString();
        switch (dataString){
            case "*":
            case "/":
            case "%":
                precedence = 7;
                break;
            case "+":
            case "-":
                precedence = 0;
                break;
            case "(":
                precedence = 9;
                break;
            case ")":
                precedence = 10;
                break;
            case "==":
                precedence = 13;
                break;
            case "&&":
                precedence  = 14;
                break;
            case "||":
                precedence = 15;
                break;
        }

        return precedence;
    }

    private List<Node> evalCompilationUnit() {
        for(Map.Entry<String, Node> entry : functionNodeMap.entrySet()) {
            if (entry.getKey().equals( mainFunctionName )) {

                ActivationFrame frame = new ActivationFrame();
                frame.frameName = mainFunctionName;
                activationFrameStack.push( frame );
                execute( entry.getValue().getInstructions() );
                activationFrameStack.pop();

                break;
            }
        }

        return new ArrayList<>();
    }

    private List<Node> evalStatement( Node node){
        execute( node.getInstructions() );
        return new ArrayList<>();
    }

    private List<Node> evalActivationFrame(Node node) {
        ActivationFrame frame = activationFrameStack.peek();
        frame.variableSet.put (((VariableNode)node).variableName, node);
        return new ArrayList<>();
    }

    private List<Node> evalSummation(){
        return new ArrayList<>();
    }

    private List<Node> evalCommand(Node node) {
        List<Node> slotList = new ArrayList<>();
        slotList.add( node.getInstructions().get(0) );
        return slotList;
    }

    private List<Node> evaluateBreak(Node node, ActivationFrame frame){
        frame.pushReturnNode( node );
        return new ArrayList<>();
    }

    private List<Node> evalAdd(){
        return new ArrayList<>();
    }

    private List<Node> evaluateEvauatable( Node node, ActivationFrame frame, Interpreter interpreter){
            for (Node n : node.getInstructions()) {
                    if ( n instanceof FinalNode){
                        String data = ((FinalNode) n).dataString();
                        switch (data) {
                            case "+":
                            case "-":
                            case "*":
                            case "/":
                            case ">=":
                            case "<=":
                            case "&&":
                            case "==":
                                interpreter.pushOperatorStack(n);
                                break;
                            default:
                                interpreter.pushOperandStack(n);
                        }
                    }

                    if ( n instanceof LiteralNode){
                        evaluateEvauatable( n, frame, interpreter);
                    }

                    if (n instanceof FunctionCallNode ){

                    }

                    if ( n instanceof VariableNode ){
                        String s = ((VariableNode)n).getVariableName();
                        interpreter.pushOperandStack( n );
                    }

                    if ( n instanceof BinaryNode) {
                        evaluateEvauatable(n, frame, interpreter);
                    }
                }
/*
            }

            if ( shouldEvaluate ) {
                interpreter.evaluateExpressionStack();
            }

        }
*/
        return  new ArrayList<>();
    }

    private List<Node> evalNull(){
        assert true : "called evalNull";
        return new ArrayList<>();
    }

    private List<Node> evalFinal(){
        return new ArrayList<>();
    }

    private List<Node> evalReturn(Node oldnode, ActivationFrame frame, Interpreter callback) {
        return activationFrameStack.setupReturnValueOnStackFrame( oldnode );
    }

    private List<Node> evalWhileExpression(Node node, ActivationFrame frame, Interpreter callback) {
        List<Node> instructionList = ((NodeImpl)node).getConditionalExpressions();

        Node expressionNode = instructionList.get( 0 );

        if ( expressionNode instanceof EvaluatableNode ) {
            expressionNode.EvaluateNode( frame, callback);
        }

        /*
        BooleanOperatorNode booleanNode = (BooleanOperatorNode) instructionList.get( 0 );

        evalBooleanNode(booleanNode, frame, callback);

        Node boolEvalResult = null;

        if (frame.peekReturnNode() != null) {
            boolEvalResult = frame.popNode();

            FinalNode finalNode = (FinalNode) boolEvalResult.getInstructions().get(0);
            Boolean executeTrue = Boolean.parseBoolean(finalNode.dataString());

            if (executeTrue) {
                Boolean breakStatement = false;
                while(true) {
                    for (int expressionCount = 0; expressionCount < trueExpressions.size(); expressionCount++) {
                        List<Node> currentExpressionInWhileBody = new ArrayList<>();
                        Node currentNode = trueExpressions.get( expressionCount );
                        currentExpressionInWhileBody.add( currentNode );

                        if (currentNode instanceof StatementNode && currentNode.getInstructions().get(0).getInstructions().get(0) instanceof BreakExprNode){
                            breakStatement = true;
                            break;
                        }

                        execute(currentExpressionInWhileBody);

                        if (frame.peekReturnNode() instanceof BreakExprNode){
                            breakStatement = true;
                            break;
                        }
                    }

                    if (breakStatement) {
                        frame.pushReturnNode( null );
                        break;
                    }

                    // re-evaluateExpression the loop condtion
                    evalBooleanNode(booleanNode, frame, callback);
                    boolEvalResult = frame.popNode();
                    finalNode = (FinalNode) boolEvalResult.getInstructions().get(0);

                    //assert finalNode.dataString().equalsIgnoreCase( "true ") || finalNode.dataString().equalsIgnoreCase( "false" ) : "A boolean value was not returned";

                    if (!(Boolean.parseBoolean( finalNode.dataString() ))) {
                        break;
                    }
                }
            }
        }*/

        return new ArrayList<>();
    }



    private BooleanNode getBooleanExpressionNode(List<Node> instructionList, int size,List<Node> trueExpressions) {
        BooleanNode booleanNode = null;
        for (int i = 0; i < size; i++) {
            Node current = instructionList.get(i);

            if (current instanceof BooleanNode) {
                booleanNode = (BooleanNode) current;
                continue;
            }

            if (current instanceof StatementNode) {
                trueExpressions.add(current);
            }
        }
        return booleanNode;
    }

    private List<Node> evalIfStatement(Node node, ActivationFrame frame,Interpreter callback){
        List<Node> instructionList = node.getInstructions();
        int size = instructionList.size();


        List<Node> trueExpressions = new ArrayList<>();
        BooleanNode booleanNode = getBooleanExpressionNode(instructionList, size, trueExpressions);

        if (booleanNode != null && booleanNode.getInstructions().size() == 1){
            FinalNode finalNode = (FinalNode)booleanNode.getInstructions().get(0);
            if (finalNode.getIntegralType() == IntegralType.jboolean){
                Boolean bool = Boolean.parseBoolean(finalNode.dataString());
                if (bool){
                    return trueExpressions;
                }
            }
        } else if ( booleanNode != null && !booleanNode.getInstructions().isEmpty()){
            Node currentValue = frame.popNode();
            frame.pushReturnNode( null );
            Boolean booleanResult = false;
            evalBooleanNode( booleanNode, frame, callback);

            if ( frame.peekReturnNode() != null && frame.peekReturnNode() instanceof BooleanNode ){
                Node booleanEvalReturnNode = frame.popNode();
                FinalNode result = getFinalNodeFromAnyNode( booleanEvalReturnNode );
                booleanResult =  Boolean.parseBoolean( result.dataString() );
            }

            // TODO - FINISH THIS
            if ( booleanResult && frame.peekReturnNode() != null  ) {
                Node returnNode = frame.popNode();
                if ( returnNode instanceof BreakExprNode){
                    List<Node> returnList = new ArrayList<>();
                    returnList.add( returnNode );
                    return new ArrayList<>();
                }
            }

            frame.pushReturnNode( currentValue );
        }

        return new ArrayList<>();
    }

    private List<Node> evalFunctionDecl(Node node){
        return new ArrayList<>();
    }

    private List<Node> evalBooleanNode(Node node, ActivationFrame frame, Interpreter callback){
        Node variableType  = node.getInstructions().get(0);

        if ( node instanceof BooleanOperatorNode ){
      //      ((BooleanOperatorNode)node).evaluateExpression( frame , this);
        }

        try {
            Node lvalue = null;
            if (variableType instanceof VariableNode) {
                String variableName = ((VariableNode) node.getInstructions().get(0)).variableName;
                lvalue = frame.variableSet.get(variableName);

            } else {
                lvalue = variableType;
            }

            boolean isEqualEqual;
            Node rvalue = null;
            // This is ugly code. Need to find a better way to
            // handle these cases.
            // Multiple ifs will only cause confusion.
            FinalNode updatedLvalue = null;
/*
            if (node.getInstructions().size() == 1) {
                //lvalue must be a single boolean expression
                if (lvalue instanceof FinalNode) {
                    BooleanNode booleanNode = new BooleanNode();
                    booleanNode.addInst(lvalue);
                    frame.pushReturnNode( booleanNode );
                    return new ArrayList<>();
                }

                if (lvalue instanceof PrimitiveNode) {
                    updatedLvalue = (FinalNode) lvalue.getInstructions().get(0);
                }

            } else if (node.getInstructions().size() > 1) {
                //if (booleanOperatorNode.getInstructions().get(0) instanceof EqualEqualSignNode) {
                    //isEqualEqual;
                //}
                rvalue = node.getInstructions().get(2);
                FinalNode updatedRvalue = null;
                if (rvalue != null && rvalue instanceof PrimitiveNode) {
                    updatedRvalue = (FinalNode) rvalue.getInstructions().get(0);
                }

                if (updatedLvalue != null) {
                    lvalue = updatedLvalue;
                }

                if (updatedRvalue != null) {
                    rvalue = updatedRvalue;
                }

                isEqualEqual = getFinalNodeFromAnyNode( lvalue) .dataString().equals( getFinalNodeFromAnyNode(rvalue).dataString());
                //else if (booleanOperatorNode.getInstructions().get(0) instanceof  )
                FinalNode finalNode = new FinalNode();
                finalNode.setDataString(isEqualEqual);

                BooleanNode booleanNode = new BooleanNode();
                booleanNode.addInst(finalNode);

                frame.pushReturnNode( booleanNode );
                return new ArrayList<>();
            }
            */
        }
        catch( Exception ex){
            JuliarLogger.log(ex.getMessage());
        }

        return new ArrayList<>();
    }

    private List<Node> evalVariableDeclration(Node node, ActivationFrame frame, Interpreter callback) {
        if (node.getInstructions().size() == 2) {
            VariableNode variableNode = (VariableNode) node.getInstructions().get(1);
            frame.variableSet.put(variableNode.variableName, variableNode);

        } else if (node.getInstructions().size() > 2) {

            List<Node> returnValue = EvaluateAssignments.evalVariableDeclWithAssignment(node, activationFrameStack, mainFunctionName, functionNodeMap);
            if (returnValue.size() > 0) {
                execute(returnValue);
            }
        }

        return new ArrayList<>();
    }

    private Boolean doesExpressionContainFunctionCall (Node n){
        for (Node expr : n.getInstructions()) {
            if ( expr instanceof FunctionCallNode){
                return true;
            }
        }

        return false;
    }

    public FinalNode getFinalNodeFromAnyNode(Node node){
        if ( node instanceof FinalNode){
            return (FinalNode)node;
        }

        if (node.getInstructions().size() == 1){
            return getFinalNodeFromAnyNode( node.getInstructions().get(0));
        }

        throw new RuntimeException( "not able to find a final node");
    }

    private boolean isEqual(Node left, Node right){
        return left.equals(right);
    }

    private boolean isLessThan(Node left, Node right){
        return false; //left < right;
    }

    private boolean isGreaterThan(Node left, Node right){
        return false;
    }

    private List<Node> evalBooleanOperator(Node node, ActivationFrame frame,Interpreter callback){
        return new ArrayList<>();
    }

    private List<Node> evalUserDefinedFunctionCall (Node n){
        return EvaluateFunctionsCalls.evalUserDefinedFunctionCall( n , this);
    }

    private List<Node> evalFunctionCall(Node node) {
        return EvaluateFunctionsCalls.evalFunctionCall( node, activationFrameStack , mainFunctionName , functionNodeMap, this);
    }


    private List<Node> AggregateNode(Node node){
        List<IntegralTypeNode> integralTypeNodes = ((AggregateNode)node).data();
        int addCount = integralTypeNodes.size() - 1;
        //TODO Different Primitive Types //add
        for(IntegralTypeNode integralTypeNode : integralTypeNodes) {
            integralTypeNode( integralTypeNode);
        }
        for(int i =0;i<addCount;i++){
            binaryOperation( (a,b) -> a+b); //check
        }
        return new ArrayList<>();
    }

    private List<Node> binaryNode( String variableName, Node node){
        evalBinaryNode( node );
        ActivationFrame frame = activationFrameStack.peek();
        VariableNode v = (VariableNode) frame.variableSet.get( variableName );

        v.setIntegralTypeNode( (IntegralTypeNode)frame.operandStack.pop() );
        return new ArrayList<>();
    }

    private List<Node> evaluateAggregate(Node node, ActivationFrame frame, Interpreter callback) {
        SummationType summationType = (SummationType) node.getInstructions().get(0);
        List<Node> list = node.getInstructions();

        int size = list.size();
        int sum = 0;

        List<VariableNode> listOfVariableNodes = new ArrayList<>();

        for (int i = 1; i < size; i++) {
            if (node.getInstructions().get(i) instanceof VariableNode) {
                listOfVariableNodes.add((VariableNode) node.getInstructions().get(i));
            }
            else {
                String value = ((FinalNode) list.get(i).getInstructions().get(0)).dataString();
                sum += Integer.parseInt(value);
            }
        }

        if (!listOfVariableNodes.isEmpty()) {
            sum += aggregateVariable( listOfVariableNodes , frame);
        }

        FinalNode returnNode = new FinalNode();
        returnNode.setDataString(sum);
        frame.pushReturnNode( returnNode );

        return new ArrayList<>();
    }

    private int aggregateVariable(List<VariableNode> variableNodeList, ActivationFrame frame){
        int sum = 0;
        for (int i = 0; i<variableNodeList.size(); i++){

            Node v = frame.variableSet.get( variableNodeList.get(i).variableName);
            FinalNode finalNode = null;

            if (v instanceof FinalNode && v.getInstructions().isEmpty()){
                finalNode = (FinalNode)v;
            }

            if (v instanceof PrimitiveNode) {
                PrimitiveNode primitiveNode = (PrimitiveNode) v;
                finalNode = (FinalNode)primitiveNode.getInstructions().get(0);
            }

            if (finalNode != null) {
                sum += Integer.parseInt(finalNode.dataString());
            }
        }

        return sum;
    }


    private List<Node> evalBinaryNode(Node node) {
        BinaryNode bn = (BinaryNode) node;
        /*
        String operation = bn.operation().name();

        Object ol = bn.left();
        Object or = bn.right();

        if (ol instanceof IntegralTypeNode) {
            integralTypeNode((IntegralTypeNode) ol);
        }

        if (or instanceof IntegralTypeNode) {
            integralTypeNode((IntegralTypeNode) or);
        }

        switch (operation.toLowerCase()) {
            case "+":
            case "add":
                binaryOperation( (a, b) -> a + b );
                break;
            case "-":
            case "subtract":
                binaryOperation( (a, b) -> a - b );
                break;
            case "*":
            case "multiply":
                binaryOperation( (a, b) -> a * b );
                break;
            case "/":
            case "divide":
                binaryOperation( (a, b) -> a / b );
                break;
            case "%":
            case "modulo":
                binaryOperation( (a,b) -> a % b);
                break;
            default:
                assert true;
                break;
        }
        */
        return new ArrayList<>();
    }

    private List<Node> binaryOperation( IntegerMath integerMath) {
        //ActivationFrame frame = activationFrameStack.peek();
     //   String data1 = ((IntegralTypeNode) frame.operandStack.pop()).getIntegralValue();
      //  int v1 = Integer.decode(data1).intValue();


     //   String data2 = ((IntegralTypeNode) frame.operandStack.pop()).getIntegralValue();
     //   int v2 = Integer.decode(data2).intValue();

     //   String sum = new Integer(integerMath.operation(v2, v1)).toString();

        //TODO - NEED to FIX THIS.
        //frame.operandStack.push(new IntegralTypeNode(sum, IntegralType.jinteger));
        return new ArrayList<>();
    }

    private void integralTypeNode(IntegralTypeNode itn) {
        ActivationFrame frame = activationFrameStack.peek();
        frame.operandStack.push(itn);
    }

    interface IntegerMath {
        int operation(int a, int b);
    }

    /*
    interface Evaluate {
        List<Node> evaluateExpression(Node node, ActivationFrame frame);
    }
    */
}
