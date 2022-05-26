package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.TernaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.SetValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.NoType;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.array.ArrayType;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.ClassType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.ast.types.set.SetType;
import main.compileError.typeError.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;
import main.symbolTable.utils.graph.Graph;
import main.util.ArgPair;
import main.visitor.Visitor;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;

import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    private Graph<String> classHierarchy;
    private ClassDeclaration currentClass;
    private MethodDeclaration currentMethod;
    private boolean seenNoneLvalue = false;
    public boolean is_lval = true;
    private boolean isInMethodCallStatement = false;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public void setCurrentClass(ClassDeclaration classDeclaration) {
        this.currentClass = classDeclaration;
    }
    public void setCurrentMethod(MethodDeclaration methodDeclaration) {
        this.currentMethod = methodDeclaration;
    }
    public void setIsInMethodCallStatement(boolean isInMethodCall) {
        this.isInMethodCallStatement = isInMethodCall;
    }
    public boolean isSameType(ArrayList<Type> firstTypes, ArrayList<Type> secondTypes) {
        if (firstTypes.size() != secondTypes.size())
            return false;
        for (int i = 0; i < firstTypes.size(); i++) {
            if (!isSameType(firstTypes.get(i), secondTypes.get(i)))
                return false;
        }
        return true;
    }

    public boolean isLvalue(Expression expression) {
        boolean prevIsCatchErrorsActive = Node.isCatchErrorsActive;
        boolean prevSeenNoneLvalue = this.seenNoneLvalue;
        Node.isCatchErrorsActive = false;
        this.seenNoneLvalue = false;
        expression.accept(this);
        boolean isLvalue = !this.seenNoneLvalue;
        this.seenNoneLvalue = prevSeenNoneLvalue;
        Node.isCatchErrorsActive = prevIsCatchErrorsActive;
        return isLvalue;
    }

    public boolean isSameType(Type first, Type second) {
        if (first instanceof NoType || second instanceof NoType)
            return true;
        if (first instanceof IntType && second instanceof IntType)
            return true;
        if (first instanceof BoolType && second instanceof BoolType)
            return true;
        if (first instanceof VoidType && second instanceof VoidType)
            return true;
        if (first instanceof SetType && second instanceof SetType)
            return true;
        if (first instanceof ClassType) {
            if (!(second instanceof ClassType))
                return false;
            return classHierarchy.isSecondNodeAncestorOf(((ClassType) first).getClassName().getName(), ((ClassType) second).getClassName().getName());
        }
        if (first instanceof ArrayType) {
            if (!(second instanceof ArrayType))
                return false;
            return isSameType(((ArrayType) first).getType(), ((ArrayType) second).getType());
        }
        if (first instanceof FptrType) {
            if (!(second instanceof FptrType))
                return false;
            Type firstRetType = ((FptrType) first).getReturnType();
            Type secondRetType = ((FptrType) second).getReturnType();
            if (!isSameType(firstRetType, secondRetType))
                return false;
            ArrayList<Type> firstArgs = ((FptrType) first).getArgumentsTypes();
            ArrayList<Type> secondArgs = ((FptrType) second).getArgumentsTypes();
            return isSameType(firstArgs, secondArgs);
        }
        return false;
    }

    public void checkForUndefinedClasses(Node node, Type type) {

        if (type instanceof ClassType) {
            String className = ((ClassType) type).getClassName().getName();
            if (!this.classHierarchy.doesGraphContainNode(className)) {
                ClassNotDeclared exception = new ClassNotDeclared(node.getLine(), className);
                node.addError(exception);
            }
        }
        else if (type instanceof FptrType) {
            Type returnType = ((FptrType) type).getReturnType();
            ArrayList<Type> argTypes = ((FptrType) type).getArgumentsTypes();
            this.checkForUndefinedClasses(node, returnType);
            for (Type argType : argTypes)
                this.checkForUndefinedClasses(node, argType);
        }
    }


    // Change alot
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //Todo: Probably Done
        if (binaryExpression == null)
            return new NullType();

        Expression left_exp = binaryExpression.getFirstOperand();
        Expression right_exp = binaryExpression.getSecondOperand();
        BinaryOperator binaryOperator = binaryExpression.getBinaryOperator();
        Type t2 = right_exp.accept(this);
        Type t1 = left_exp.accept(this);

        if(t1 instanceof NoType && t2 instanceof NoType){
            is_lval = false;
            return new NoType();
        }

        // operator add   sub  mult  div   mod
        if (binaryOperator.equals(BinaryOperator.add) || binaryOperator.equals(BinaryOperator.sub) ||
                binaryOperator.equals(BinaryOperator.mult) || binaryOperator.equals(BinaryOperator.div)
                || binaryOperator.equals(BinaryOperator.lt) ||
                binaryOperator.equals(BinaryOperator.gt)) {
            if (!(t1 instanceof IntType || t1 instanceof NoType) ||
                    !(t2 instanceof IntType || t2 instanceof NoType)) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                is_lval = false;
                return new NoType();

            }
        }

        // operator and or
        else if (binaryOperator.equals(BinaryOperator.and) ||  binaryOperator.equals(BinaryOperator.or)) {
            if (!(t1 instanceof BoolType || t1 instanceof NoType) ||
                    !(t2 instanceof BoolType || t2 instanceof NoType)) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                is_lval = false;
                return new NoType();

            }
        }

        // op == < >
        else if (binaryOperator.equals(BinaryOperator.eq) || binaryOperator.equals(BinaryOperator.gt) || binaryOperator.equals(BinaryOperator.lt)) {
            if (t1 instanceof ArrayType || t2 instanceof ArrayType) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                is_lval = false;
                return new NoType();
            }
            else if (t1 instanceof ClassType || t2 instanceof ClassType){
                if(!t1.toString().equals(t2.toString()) && !(t1 instanceof NullType) && !(t2 instanceof NullType)){
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                    is_lval = false;
                    return new NoType();
                }
            }
            else if ((t1 instanceof FptrType || t2 instanceof FptrType)){
                if((!isFirstSubTypeOfSecond(t1, t2) || !isFirstSubTypeOfSecond(t2, t1))
                        &&!(t1 instanceof NullType) && !(t2 instanceof NullType)){
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                    is_lval = false;
                    return new NoType();
                }
            }
            else if(!t1.toString().equals(t2.toString()) && !(t1 instanceof NoType || t2 instanceof NoType)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                is_lval = false;
                return new NoType();
            }
        }
        else if(binaryOperator.equals(BinaryOperator.assign)){
            if(!is_lval){
                binaryExpression.addError(new LeftSideNotLvalue(binaryExpression.getLine()));
                return new NoType();
            }
            if(!isFirstSubTypeOfSecond(t2, t1)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.toString()));
                is_lval = false;
                return new NoType();
            }
        }

        if(t1 instanceof NoType || t2 instanceof NoType){
            is_lval = false;
            return new NoType();
        }

        if (binaryOperator.equals(BinaryOperator.add) || binaryOperator.equals(BinaryOperator.sub) ||
                binaryOperator.equals(BinaryOperator.mult) || binaryOperator.equals(BinaryOperator.div)) {
            is_lval = false;
            return new IntType();
        }

        if (binaryOperator.equals(BinaryOperator.lt) || binaryOperator.equals(BinaryOperator.gt)) {
            is_lval = false;
            return new BoolType();
        }

        // operator and or
        if (binaryOperator.equals(BinaryOperator.and) ||  binaryOperator.equals(BinaryOperator.or)) {
            is_lval = false;
            return new BoolType();
        }

        // op ==
        if (binaryOperator.equals(BinaryOperator.eq)) {
            is_lval = false;
            return new BoolType();
        }

        if(binaryOperator.equals(BinaryOperator.assign)){
            is_lval = false;
            return t2;
        }

        return new NoType();

    }

    public boolean isFirstSubTypeOfSecondMultiple(ArrayList<Type> first, ArrayList<Type> second) {
        if(first.size() != second.size())
            return false;
        for(int i = 0; i < first.size(); i++) {
            if(!isFirstSubTypeOfSecond(first.get(i), second.get(i)))
                return false;
        }
        return true;
    }

    public boolean isFirstSubTypeOfSecond(Type first, Type second) {
        if(first instanceof NoType)
            return true;
        else if(first instanceof IntType || first instanceof BoolType)
            return first.toString().equals(second.toString());
        else if(first instanceof NullType)
            return second instanceof NullType || second instanceof FptrType || second instanceof ClassType;
        else if(first instanceof ClassType) {
            if(!(second instanceof ClassType))
                return false;
            return this.classHierarchy.isSecondNodeAncestorOf(((ClassType) first).getClassName().getName(), ((ClassType) second).getClassName().getName());
        }
        else if(first instanceof FptrType) {
            if(!(second instanceof FptrType))
                return false;
            Type firstRetType = ((FptrType) first).getReturnType();
            Type secondRetType = ((FptrType) second).getReturnType();
            if(!isFirstSubTypeOfSecond(firstRetType, secondRetType))
                return false;
            ArrayList<Type> firstArgsTypes = ((FptrType) first).getArgumentsTypes();
            ArrayList<Type> secondArgsTypes = ((FptrType) second).getArgumentsTypes();
            return isFirstSubTypeOfSecondMultiple(secondArgsTypes, firstArgsTypes);
        }
        return false;
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {

        //todo: done
        this.seenNoneLvalue = true;
        String className = newClassInstance.getClassType().getClassName().getName();
        ArrayList<Type> newInstanceTypes = new ArrayList<>();
        for(Expression expression : newClassInstance.getArgs())
            newInstanceTypes.add(expression.accept(this));
        if(this.classHierarchy.doesGraphContainNode(className)) {
            try {
                ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + className, true);
                MethodSymbolTableItem methodSymbolTableItem = (MethodSymbolTableItem) classSymbolTableItem.getClassSymbolTable().getItem(MethodSymbolTableItem.START_KEY + "initialize", true);
                ArrayList<Type> constructorActualTypes = methodSymbolTableItem.getArgTypes();
                if(this.isFirstSubTypeOfSecondMultiple(newInstanceTypes, constructorActualTypes)) {
                    return newClassInstance.getClassType();
                }
                else {
                    ConstructorArgsNotMatchDefinition exception = new ConstructorArgsNotMatchDefinition(newClassInstance);
                    newClassInstance.addError(exception);
                    return new NoType();
                }
            } catch (ItemNotFoundException ignored) {
                if(newInstanceTypes.size() != 0) {
                    ConstructorArgsNotMatchDefinition exception = new ConstructorArgsNotMatchDefinition(newClassInstance);
                    newClassInstance.addError(exception);
                    return new NoType();
                }
                else {
                    return newClassInstance.getClassType();
                }
            }
        }
        else {
            ClassNotDeclared exception = new ClassNotDeclared(newClassInstance.getLine(), className);
            newClassInstance.addError(exception);
            return new NoType();
        }
    }

    // Change alot
    @Override
    public Type visit(UnaryExpression unaryExpression) {

        //Todo
        this.seenNoneLvalue = true;
        Type operandType = unaryExpression.getOperand().accept(this);
        UnaryOperator operator = unaryExpression.getOperator();
        if(operator == UnaryOperator.not) {
            if(operandType instanceof NoType)
                return new NoType();
            if(operandType instanceof BoolType)
                return operandType;
            UnsupportedOperandType exception = new UnsupportedOperandType(unaryExpression.getLine(), operator.name());
            unaryExpression.addError(exception);
            return new NoType();
        }
        else if(operator == UnaryOperator.minus) {
            if(operandType instanceof NoType)
                return new NoType();
            if(operandType instanceof IntType)
                return operandType;
            UnsupportedOperandType exception = new UnsupportedOperandType(unaryExpression.getLine(), operator.name());
            unaryExpression.addError(exception);
            return new NoType();
        }
        else {
            boolean isOperandLvalue = this.isLvalue(unaryExpression.getOperand());
            if(!isOperandLvalue) {
                IncDecOperandNotLvalue exception = new IncDecOperandNotLvalue(unaryExpression.getLine(), operator.name());
                unaryExpression.addError(exception);
            }
            if(operandType instanceof NoType)
                return new NoType();
            if(operandType instanceof IntType) {
                if(isOperandLvalue)
                    return operandType;
                return new NoType();
            }
            UnsupportedOperandType exception = new UnsupportedOperandType(unaryExpression.getLine(), operator.name());
            unaryExpression.addError(exception);
            return new NoType();
        }
    }

    @Override
    public Type visit(MethodCall methodCall) {

        //Todo
        boolean containsError = false;
        boolean prevInFunctionCallStmt = isInMethodCallStatement;
        ArrayList<Type> methodCallArgsType = new ArrayList<>();

        isInMethodCallStatement = false;
        Type instanceType = methodCall.getInstance().accept(this);
        for (Expression expression : methodCall.getArgs()) {
            Type type = expression.accept(this);
            methodCallArgsType.add(type);
        }
        isInMethodCallStatement = prevInFunctionCallStmt;

        if(instanceType instanceof NoType)
            return new NoType();

        if (!(instanceType instanceof FptrType )){
            CallOnNoneCallable exception = new CallOnNoneCallable(methodCall.getLine());
            methodCall.addError(exception);
            return new NoType();
        }

        FptrType fptrType = (FptrType) instanceType;
        ArrayList<Type> fptrArgsType = fptrType.getArgumentsTypes();
        if (fptrType.getReturnType() instanceof VoidType && !isInMethodCallStatement){
            containsError = true;
            CantUseValueOfVoidMethod exception = new CantUseValueOfVoidMethod(methodCall.getLine());
            methodCall.addError(exception);
        }
        isInMethodCallStatement = false;
        // If args of fptr and method call is not the same then print error.
        if (methodCallArgsType.size() != fptrArgsType.size()) {
            containsError = true;
            MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
            methodCall.addError(exception);
        }

        else if (fptrArgsType.size() != 0) {
            for(int i = 0; i < fptrArgsType.size(); i += 1){
                if (!isSameType(fptrArgsType.get(i), methodCallArgsType.get(i))) {
                    containsError = true;
                    MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
                    methodCall.addError(exception);

                    // Don't check any other errors if you found one!
                    break;
                }
            }
        }

        if (containsError)
            return new NoType();
        else
            return fptrType.getReturnType();
    }

    @Override
    public Type visit(Identifier identifier) {
        try {
            ClassSymbolTableItem classSTI = (ClassSymbolTableItem) SymbolTable.root
                    .getItem(ClassSymbolTableItem.START_KEY + this.currentClass.getClassName().getName(), true);
            SymbolTable classST = classSTI.getClassSymbolTable();
            MethodSymbolTableItem methodSTI = (MethodSymbolTableItem) classST
                   .getItem(MethodSymbolTableItem.START_KEY + this.currentMethod.getMethodName().getName(), true);
            SymbolTable methodST = methodSTI.getMethodSymbolTable();
            LocalVariableSymbolTableItem localVarSTI = (LocalVariableSymbolTableItem) methodST
                    .getItem(LocalVariableSymbolTableItem.START_KEY + identifier.getName(), true);
            return localVarSTI.getType();
        } catch (ItemNotFoundException e) {
            VarNotDeclared exception = new VarNotDeclared(identifier.getLine(), identifier.getName());
            identifier.addError(exception);
            return new NoType();
        }
    }

    @Override
    public Type visit(ArrayAccessByIndex arrayAccessByIndex) {
        //Todo: maybe done

        Type instanceType = arrayAccessByIndex.getInstance().accept(this);
        boolean prevSeenNoneLvalue = this.seenNoneLvalue;
        Type indexType = arrayAccessByIndex.getIndex().accept(this);
        this.seenNoneLvalue = prevSeenNoneLvalue;
        boolean indexErrored = false;
        if(!(indexType instanceof NoType || indexType instanceof IntType)) {
            ArrayIndexNotInt exception = new ArrayIndexNotInt(arrayAccessByIndex.getLine());
            arrayAccessByIndex.addError(exception);
            indexErrored = true;
        }
        if(instanceType instanceof ArrayType) {
            ArrayList<Type> types = new ArrayList<>();
            if(indexErrored)
                return new NoType();
        }
        else if(!(instanceType instanceof NoType)) {
            AccessByIndexOnNoneArray exception = new AccessByIndexOnNoneArray(arrayAccessByIndex.getLine());
            arrayAccessByIndex.addError(exception);
        }
        return new NoType();
    }

    @Override
    public Type visit(ObjectMemberAccess objectMemberAccess) {
        //Todo

        Expression e = objectMemberAccess.getInstance();
        Identifier i = objectMemberAccess.getMemberName();
        Type t1 = e.accept(this);
        if(!(t1 instanceof ClassType)){
            objectMemberAccess.addError(new AccessOnNonClass(objectMemberAccess.getLine()));
            return new NoType();
        }
        if(t1 instanceof ClassType){
            try{
                ClassSymbolTableItem currentClass = (ClassSymbolTableItem) SymbolTable.root
                        .getItem(ClassSymbolTableItem.START_KEY + ((ClassType) t1).getClassName().getName(), true);
                ArrayList<FieldDeclaration> fieldDeclarations = currentClass.getClassDeclaration().getFields();
                ArrayList<MethodDeclaration> methodDeclarations = currentClass.getClassDeclaration().getMethods();

                try {
                    FieldSymbolTableItem calledField = (FieldSymbolTableItem) currentClass.getClassSymbolTable()
                            .getItem(FieldSymbolTableItem.START_KEY + i.getName(), true);
                    return calledField.getType();
                }catch (ItemNotFoundException MethodNotFound) {
                    try {
                        MethodSymbolTableItem calledMethod = (MethodSymbolTableItem) currentClass.getClassSymbolTable()
                                .getItem(MethodSymbolTableItem.START_KEY + i.getName(), true);
                        is_lval = false;
                        return new FptrType(calledMethod.getArgTypes(), calledMethod.getReturnType());

                    }catch (ItemNotFoundException MemberNotFound){
                        if(i.getName().equals(currentClass.getName())){
                            return new FptrType(new ArrayList<>(), new NullType());
                        }
                        objectMemberAccess.addError(new MemberNotAvailableInClass(objectMemberAccess.getLine(),
                                i.getName(), currentClass.getClassDeclaration().getClassName().getName()));
                        return new NoType();
                    }
                }
            }catch (ItemNotFoundException classNotFound){

            }
        }

        return new NoType();
    }

    @Override
    public Type visit(SetNew setNew) {
        //Todo
        ArrayList<Expression> args = setNew.getArgs();
        for (Expression arg : args) {
            Type t = arg.accept(this);
            if (!(t instanceof IntType) ){
                NewInputNotSet exception = new NewInputNotSet(setNew.getLine());
                setNew.addError(exception);
                return new NoType();
            }
        }
        return new SetType();
    }

    @Override
    public Type visit(SetInclude setInclude) {
        //Todo
        Expression setArg = setInclude.getSetArg();
        Expression elementArg = setInclude.getElementArg();

        Type setArgAcc = setArg.accept(this);
        Type elementArgAcc = elementArg.accept(this);

        if (!(elementArgAcc instanceof IntType) ){
            SetIncludeInputNotInt exception = new SetIncludeInputNotInt(setInclude.getLine());
            setInclude.addError(exception);
            return new NoType();
        }

        return new NoType();
    }

    @Override
    public Type visit(RangeExpression rangeExpression) {
        //Todo
        Expression lExpr = rangeExpression.getLeftExpression();
        Expression rExpr = rangeExpression.getRightExpression();

        Type lExprAcc = lExpr.accept(this);
        Type rExprAcc = rExpr.accept(this);

        if (!(lExprAcc instanceof IntType) || !(rExprAcc instanceof IntType)){
            EachRangeNotInt exception = new EachRangeNotInt(lExpr.getLine());
            rangeExpression.addError(exception);
            return new NoType();
        }

        return new NoType();
    }

    @Override
    public Type visit(TernaryExpression ternaryExpression) {
        //Todo
        Expression condition = ternaryExpression.getCondition();
        Expression trueExpr = ternaryExpression.getTrueExpression();
        Expression falseExpr = ternaryExpression.getFalseExpression();

        Type condAcc = condition.accept(this);
        Type trueExprAcc = trueExpr.accept(this);
        Type falseExprAcc = falseExpr.accept(this);

        if (!(condAcc instanceof BoolType)){
            // The first operand should be Bool!
            ConditionNotBool exception = new ConditionNotBool(condition.getLine());
            ternaryExpression.addError(exception);
            return new NoType();
        }

        if(!isSameType(trueExprAcc,falseExprAcc))
        {
            UnsupportedOperandType exception = new UnsupportedOperandType(condition.getLine(), TernaryOperator.ternary.name());
            ternaryExpression.addError(exception);
            return new NoType();
        }

        if(trueExprAcc instanceof BoolType)
            return new BoolType();
        else if(trueExprAcc instanceof IntType)
            return new BoolType();
        //Not sure about this
        else if(trueExprAcc instanceof NoType && falseExprAcc instanceof NoType)
            return new NoType();
        else {
            UnsupportedOperandType exception = new UnsupportedOperandType(ternaryExpression.getLine(), TernaryOperator.ternary.name());
            ternaryExpression.addError(exception);
            return new NoType();
        }
    }

    @Override
    public Type visit(IntValue intValue) {
        this.seenNoneLvalue = true;
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        this.seenNoneLvalue = true;
        return new BoolType();
    }

    @Override
    public Type visit(SelfClass selfClass) {
        this.seenNoneLvalue = true;
        return new ClassType(currentClass.getClassName());
    }

    @Override
    public Type visit(SetValue setValue) {
        this.seenNoneLvalue = true;
        return new SetType();
    }

    @Override
    public Type visit(NullValue nullValue) {
        this.seenNoneLvalue = true;
        return new NullType();
    }
}
