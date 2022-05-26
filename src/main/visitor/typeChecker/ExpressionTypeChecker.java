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

    public void checkNode(Node node, Type type) {

        if (type instanceof ClassType) {
            String className = ((ClassType) type).getClassName().getName();
            if (!this.classHierarchy.doesGraphContainNode(className)) {
                ClassNotDeclared exception = new ClassNotDeclared(node.getLine(), className);
                node.addError(exception);
            }
        }
        else if (type instanceof ArrayType) {
            for( Expression dimension : ((ArrayType) type).getDimensions())
                if (((IntValue)dimension).getConstant() == 0)
                    node.addError(new CannotHaveEmptyArray(node.getLine()));
        }

        else if (type instanceof FptrType) {
            Type returnType = ((FptrType) type).getReturnType();
            ArrayList<Type> argTypes = ((FptrType) type).getArgumentsTypes();
            this.checkNode(node, returnType);
            for (Type argType : argTypes)
                this.checkNode(node, argType);
        }
    }


    // Change alot
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //Todo: Probably Done
        this.seenNoneLvalue = true;
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        Type firstType = binaryExpression.getFirstOperand().accept(this);
        Type secondType = binaryExpression.getSecondOperand().accept(this);
        if((operator == BinaryOperator.eq)) {
            if(firstType instanceof NoType && secondType instanceof NoType)
                return new NoType();
            else if((firstType instanceof NoType && secondType instanceof ArrayType) ||
                    (secondType instanceof NoType && firstType instanceof ArrayType)) {
                UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
                binaryExpression.addError(exception);
                return new NoType();
            }
            else if(firstType instanceof NoType || secondType instanceof NoType)
                return new NoType();
            if(firstType instanceof IntType || firstType instanceof BoolType)
                if(firstType.toString().equals(secondType.toString()))
                    return new BoolType();
            if((firstType instanceof ClassType && secondType instanceof NullType) ||
                    (firstType instanceof NullType && secondType instanceof ClassType) ||
                    (firstType instanceof ClassType && secondType instanceof ClassType &&
                            ((ClassType)firstType).getClassName().getName().equals(((ClassType)secondType).getClassName().getName()))) {
                return new BoolType();
            }
            if((firstType instanceof FptrType && secondType instanceof NullType) ||
                    (firstType instanceof NullType && secondType instanceof FptrType) ||
                    (firstType instanceof FptrType && secondType instanceof FptrType)) {
                return new BoolType();
            }
            if(firstType instanceof NullType && secondType instanceof NullType)
                return new BoolType();
        }
        if((operator == BinaryOperator.gt) || (operator == BinaryOperator.lt)) {
            if(firstType instanceof NoType && secondType instanceof NoType)
                return new NoType();
            else if((firstType instanceof NoType && !(secondType instanceof IntType)) ||
                    (secondType instanceof NoType && !(firstType instanceof IntType))) {
                UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
                binaryExpression.addError(exception);
                return new NoType();
            }
            else if(firstType instanceof NoType || secondType instanceof NoType)
                return new NoType();
            if((firstType instanceof IntType) && (secondType instanceof IntType))
                return new BoolType();
        }
        if((operator == BinaryOperator.add) || (operator == BinaryOperator.sub) ||
                (operator == BinaryOperator.mult) || (operator == BinaryOperator.div)) {
            if(firstType instanceof NoType && secondType instanceof NoType)
                return new NoType();
            else if((firstType instanceof NoType && !(secondType instanceof IntType)) ||
                    (secondType instanceof NoType && !(firstType instanceof IntType))) {
                UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
                binaryExpression.addError(exception);
                return new NoType();
            }
            else if(firstType instanceof NoType || secondType instanceof NoType)
                return new NoType();
            if((firstType instanceof IntType) && (secondType instanceof IntType))
                return new IntType();
        }

        if((operator == BinaryOperator.or) || (operator == BinaryOperator.and)) {
            if(firstType instanceof NoType && secondType instanceof NoType)
                return new NoType();
            else if((firstType instanceof NoType && !(secondType instanceof BoolType)) ||
                    (secondType instanceof NoType && !(firstType instanceof BoolType))) {
                UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
                binaryExpression.addError(exception);
                return new NoType();
            }
            else if(firstType instanceof NoType || secondType instanceof NoType)
                return new NoType();
            if((firstType instanceof BoolType) && (secondType instanceof BoolType))
                return new BoolType();
        }
        if(operator == BinaryOperator.assign) {
            boolean isFirstLvalue = this.isLvalue(binaryExpression.getFirstOperand());
            if(!isFirstLvalue) {
                LeftSideNotLvalue exception = new LeftSideNotLvalue(binaryExpression.getLine());
                binaryExpression.addError(exception);
            }
            if(firstType instanceof NoType || secondType instanceof NoType) {
                return new NoType();
            }
            boolean isSubtype = this.isFirstSubTypeOfSecond(secondType, firstType);
            if(isSubtype) {
                if(isFirstLvalue)
                    return secondType;
                return new NoType();
            }
            UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
            binaryExpression.addError(exception);
            return new NoType();
        }
        UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(), operator.name());
        binaryExpression.addError(exception);
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
        boolean prevSeenNoneLvalue = this.seenNoneLvalue;
        Type instanceType = objectMemberAccess.getInstance().accept(this);
        if(objectMemberAccess.getInstance() instanceof SelfClass)
            this.seenNoneLvalue = prevSeenNoneLvalue;
        String memberName = objectMemberAccess.getMemberName().getName();
        if(instanceType instanceof NoType)
            return new NoType();
        else if(instanceType instanceof ClassType) {
            String className = ((ClassType) instanceType).getClassName().getName();
            SymbolTable classSymbolTable;
            try {
                classSymbolTable = ((ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + className, true)).getClassSymbolTable();
            } catch (ItemNotFoundException classNotFound) {
                return new NoType();
            }
            try {
                FieldSymbolTableItem fieldSymbolTableItem = (FieldSymbolTableItem) classSymbolTable.getItem(FieldSymbolTableItem.START_KEY + memberName, true);
                return fieldSymbolTableItem.getType();
            } catch (ItemNotFoundException memberNotField) {
                try {
                    MethodSymbolTableItem methodSymbolTableItem = (MethodSymbolTableItem) classSymbolTable.getItem(MethodSymbolTableItem.START_KEY + memberName, true);
                    this.seenNoneLvalue = true;
                    return new FptrType(methodSymbolTableItem.getArgTypes(), methodSymbolTableItem.getReturnType());
                } catch (ItemNotFoundException memberNotFound) {
                    if(memberName.equals(className)) {
                        this.seenNoneLvalue = true;
                        return new FptrType(new ArrayList<>(), new NullType());
                    }
                    MemberNotAvailableInClass exception = new MemberNotAvailableInClass(objectMemberAccess.getLine(), memberName, className);
                    objectMemberAccess.addError(exception);
                    return new NoType();
                }
            }
        }
//        else if(instanceType instanceof ArrayType) {
//            ArrayList<ListNameType> elementsTypes = ((ListType) instanceType).getElementsTypes();
//            for(ListNameType elementType : elementsTypes) {
//                if(elementType.getName().getName().equals(memberName))
//                    return this.refineType(elementType.getType());
//            }
//            ListMemberNotFound exception = new ListMemberNotFound(objectMemberAccess.getLine(), memberName);
//            objectMemberAccess.addError(exception);
//            return new NoType();
//        }
        else {
            AccessOnNonClass exception = new AccessOnNonClass(objectMemberAccess.getLine());
            objectMemberAccess.addError(exception);
            return new NoType();
        }
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

        return new BoolType();
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

        // Change it to ArrayType
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
