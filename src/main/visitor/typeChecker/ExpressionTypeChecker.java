package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.expression.*;
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
import main.compileError.typeError.ClassNotDeclared;
import main.compileError.typeError.VarNotDeclared;
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

        return null;
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
