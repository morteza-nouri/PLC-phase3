package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
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
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    private Graph<String> classHierarchy;
    private ClassDeclaration currentClass;
    private MethodDeclaration currentMethod;
    private boolean seenNoneLvalue = false;
    public boolean is_lval = true;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public void setCurrentClass(ClassDeclaration classDeclaration) {
        this.currentClass = classDeclaration;
    }
    public void setCurrentMethod(MethodDeclaration methodDeclaration) {
        this.currentMethod = methodDeclaration;
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

    public void checkNodeType(Node node, Type type) {
    // Not complete! --> maybe develop for other types too! Fptr and ?
        if (type instanceof ClassType) {
            String className = ((ClassType) type).getClassName().getName();
            if (!this.classHierarchy.doesGraphContainNode(className)) {
                ClassNotDeclared exception = new ClassNotDeclared(node.getLine(), className);
                node.addError(exception);
            }
        }
    }

    // Change alot
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //Todo: Not Done
        Expression left = binaryExpression.getFirstOperand();
        Expression right = binaryExpression.getSecondOperand();

        Type tl = left.accept(this);
        Type tr = right.accept(this);
        BinaryOperator operator =  binaryExpression.getBinaryOperator();

        if (operator.equals(BinaryOperator.and) || operator.equals(BinaryOperator.or)) {
            if (tl instanceof BoolType && tr instanceof BoolType)
                return new BoolType();

            if ((tl instanceof NoType || tl instanceof BoolType) &&
               (tr instanceof BoolType || tr instanceof NoType))
                return new NoType();
        }

        else if(operator.equals(BinaryOperator.eq)){
            if(!isSameType(tl,tr)){
                UnsupportedOperandType exception = new UnsupportedOperandType(right.getLine(), operator.name());
                binaryExpression.addError(exception);
                return new NoType();
            }
            else {
                if(tl instanceof NoType || tr instanceof NoType)
                    return new NoType();
                else
                    return new BoolType();
            }
        }

        else if(operator.equals(BinaryOperator.gt) || operator.equals(BinaryOperator.lt)){
            if (tl instanceof IntType && tr instanceof IntType)
                return new BoolType();

            if ((tl instanceof NoType || tl instanceof IntType) &&
                    (tr instanceof IntType || tr instanceof NoType))
                return new NoType();
        }

        else { // + - / *
            if (tl instanceof IntType && tr instanceof IntType)
                return new IntType();

            if ((tl instanceof NoType || tl instanceof IntType) &&
                    (tr instanceof IntType || tr instanceof NoType))
                return new NoType();
        }

        UnsupportedOperandType exception = new UnsupportedOperandType(left.getLine(), operator.name());
        left.addError(exception);
        return new NoType();
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        //todo
        is_lval = false;
        boolean t;
        String class_name = newClassInstance.getClassType().getClassName().getName();

        try {
            ClassSymbolTableItem currentClass = (ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + class_name, true);

            try {
                MethodSymbolTableItem methodCallSymTable = (MethodSymbolTableItem) currentClass.getClassSymbolTable().getItem(MethodSymbolTableItem.START_KEY + class_name, true);
                ArrayList<Expression> params = newClassInstance.getArgs();
                ArrayList<Type> paramsTypes = new ArrayList<>();
                for(Expression actualParam : params){
                    t = is_lval;
                    is_lval = true;
                    Type type = actualParam.accept(this);
                    is_lval = t;
                    paramsTypes.add(type);
                }
                ArrayList<Type> formalParamsTypes = methodCallSymTable.getArgTypes();

                // Implement later
//                if (!areParamTypeCorrect(formalParamsTypes,paramsTypes)){
//                    newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
//                }

            }
            catch (ItemNotFoundException methodNotFound) {
                ArrayList<Expression> params = newClassInstance.getArgs();
                if(!params.isEmpty()) {
                    newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
                    for(Expression actualParam : params){
                        t = is_lval;
                        is_lval = true;
                        actualParam.accept(this);
                        is_lval = t;
                    }
                    return new NoType();
                }
            }
        } catch (ItemNotFoundException classNotFound) {
            ClassNotDeclared cnd = new ClassNotDeclared(newClassInstance.getLine(), class_name);
            newClassInstance.addError(cnd);
            return new NoType();
        }
        return newClassInstance.getClassType();
    }

    // Change alot
    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //Todo
        Expression uExpr = unaryExpression.getOperand();
        Type ut = uExpr.accept(this);
        UnaryOperator operator = unaryExpression.getOperator();

        if(operator.equals(UnaryOperator.not)) {
            if(ut instanceof BoolType)
                return new BoolType();
            if(ut instanceof NoType)
                return new NoType();
            else {
                UnsupportedOperandType exception = new UnsupportedOperandType(uExpr.getLine(), operator.name());
                uExpr.addError(exception);
                return new NoType();
            }
        }

        else { //-
            if (ut instanceof IntType)
                return new IntType();
            if(ut instanceof NoType)
                return new NoType();
            else{
                UnsupportedOperandType exception = new UnsupportedOperandType(uExpr.getLine(), operator.name());
                uExpr.addError(exception);
                return new NoType();
            }
        }
    }

    @Override
    public Type visit(MethodCall methodCall) {
        //Todo
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        // Not complete
        try {
            ClassSymbolTableItem classSTI = (ClassSymbolTableItem) SymbolTable.root
                    .getItem(ClassSymbolTableItem.START_KEY + this.currentClass.getClassName().getName(), true);
            SymbolTable classST = classSTI.getClassSymbolTable();
            MethodSymbolTableItem methodSTI = (MethodSymbolTableItem) classST
                   .getItem(MethodSymbolTableItem.START_KEY + this.currentMethod.getMethodName().getName(), true);
            SymbolTable methodST = methodSTI.getMethodSymbolTable();
            LocalVariableSymbolTableItem localVarSTI = (LocalVariableSymbolTableItem) methodST
                    .getItem(LocalVariableSymbolTableItem.START_KEY + identifier.getName(), true);

        } catch (ItemNotFoundException e) {
            VarNotDeclared exception = new VarNotDeclared(identifier.getLine(), identifier.getName());
            identifier.addError(exception);
            return new NoType();
        }
        return null;
    }

    @Override
    public Type visit(ArrayAccessByIndex arrayAccessByIndex) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ObjectMemberAccess objectMemberAccess) {
        //Todo
        Expression objInst = objectMemberAccess.getInstance();
        Identifier objMemAccessName = objectMemberAccess.getMemberName();
        Type t1 = objInst.accept(this);
        if(!(t1 instanceof ClassType)){
            objectMemberAccess.addError(new AccessOnNonClass(objectMemberAccess.getLine()));
            return new NoType();
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

        }

        return new NoType();
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
