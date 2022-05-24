package main.visitor.typeChecker;

import main.ast.nodes.*;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VariableDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.SetValue;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.set.*;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.ClassType;
import main.ast.types.primitives.IntType;
import main.ast.types.set.SetType;
import main.compileError.typeError.CannotExtendFromMainClass;
import main.compileError.typeError.MainClassCantInherit;
import main.compileError.typeError.NoConstructorInMainClass;
import main.compileError.typeError.UnsupportedTypeForPrint;
import main.symbolTable.utils.graph.Graph;
import main.visitor.*;

import javax.lang.model.type.ArrayType;

public class TypeChecker extends Visitor<Void> {
    private Graph<String> classHierarchy;
    private ExpressionTypeChecker expressionTypeChecker;
    private ClassDeclaration currentClass;
    private MethodDeclaration currentMethod;

    public TypeChecker(Graph<String> classHierarchy){
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
    }

    @Override
    public Void visit(Program program) {
        // Not complete!
        for (ClassDeclaration classDeclaration : program.getClasses()) {
            this.expressionTypeChecker.setCurrentClass(classDeclaration);
            this.currentClass = classDeclaration;
            classDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        // Not complete!
        if (classDeclaration.getParentClassName() != null) {
            this.expressionTypeChecker.checkNodeType(classDeclaration, new ClassType(classDeclaration.getParentClassName()));
            if (classDeclaration.getClassName().getName().equals("Main")) {
                MainClassCantInherit exception = new MainClassCantInherit(classDeclaration.getLine());
                classDeclaration.addError(exception);
            }
            if (classDeclaration.getParentClassName().getName().equals("Main")) {
                CannotExtendFromMainClass exception = new CannotExtendFromMainClass(classDeclaration.getLine());
                classDeclaration.addError(exception);
            }
        }
        for (FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            fieldDeclaration.accept(this);
        }
        if (classDeclaration.getConstructor() != null) {
            if (classDeclaration.getClassName().getName() == "Main") {
                NoConstructorInMainClass exception = new NoConstructorInMainClass(classDeclaration);
                classDeclaration.addError(exception);
            }
            this.expressionTypeChecker.setCurrentMethod(classDeclaration.getConstructor());
            this.currentMethod = classDeclaration.getConstructor();
            classDeclaration.getConstructor().accept(this);
        }
        for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            this.expressionTypeChecker.setCurrentMethod(methodDeclaration);
            this.currentMethod = methodDeclaration;
            methodDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        //todo
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        //todo
        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        //todo
        return null;
    }

    @Override
    public Void visit(VariableDeclaration varDeclaration) {
        //todo
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(ElsifStmt elsifStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type argType = print.getArg().accept(expressionTypeChecker);
        if(!(argType instanceof IntType || argType instanceof ArrayType || argType instanceof SetType ||
                argType instanceof BoolType || argType instanceof NoType)) {
            UnsupportedTypeForPrint exception = new UnsupportedTypeForPrint(print.getLine());
            print.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(EachStmt eachStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(SetDelete setDelete) {
        //todo
        return null;
    }

    @Override
    public Void visit(SetMerge setMerge) {
        //todo
        return null;
    }

    @Override
    public Void visit(SetAdd setAdd) {
        //todo
        return null;
    }

}
