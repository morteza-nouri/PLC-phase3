package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.SetValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.array.ArrayType;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.ClassType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.ast.types.set.SetType;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    private Graph<String> classHierarchy;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
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
        if (first instanceof ArrayType && second instanceof ArrayType)
            return isSameType(((ArrayType) first).getType(), ((ArrayType) second).getType());
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

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //Todo
        return null;
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        //todo
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //Todo
        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        //Todo
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //Todo
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
        return null;
    }

    @Override
    public Type visit(SetNew setNew) {
        //Todo
        return null;
    }

    @Override
    public Type visit(SetInclude setInclude) {
        //Todo
        return null;
    }

    @Override
    public Type visit(RangeExpression rangeExpression) {
        //Todo
        return null;
    }

    @Override
    public Type visit(TernaryExpression ternaryExpression) {
        //Todo
        return null;
    }

    @Override
    public Type visit(IntValue intValue) {
        //Todo
        return null;
    }

    @Override
    public Type visit(BoolValue boolValue) {
        //Todo
        return null;
    }

    @Override
    public Type visit(SelfClass selfClass) {
        //todo
        return null;
    }

    @Override
    public Type visit(SetValue setValue) {
        //todo
        return null;
    }

    @Override
    public Type visit(NullValue nullValue) {
        //todo
        return null;
    }
}
