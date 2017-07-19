/*
 * Copyright 2017 Andrew Bayer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrewbayer.groovy.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ClosureListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement


class AstToSpecTranslator {
    private AstBuilder builder = new AstBuilder()

    private ASTNode translate(ASTNode original) {
        return builder.buildFromSpec(astToSpec(original))[0]
    }

    Closure astToSpec(ASTNode e) {
        throw new IllegalArgumentException("Unhandled ASTNode type: ${e.class}")
    }

    Closure astToSpec(ConstantExpression e) {
        return {
            constant e.value
        }
    }

    Closure astToSpec(GStringExpression e) {
        return {
            gString e.text, {
                strings {
                    e.strings.each { s ->
                        constant s.value
                    }
                }
                values {
                    e.values.each { v ->
                        expression.add(translate(v))
                    }
                }
            }
        }
    }

    Closure astToSpec(CastExpression e) {
        return {
            cast(e.type.typeClass) {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(ConstructorCallExpression e) {
        return {
            constructorCall(e.type.typeClass) {
                expression.add(translate(e.arguments))
            }
        }
    }

    Closure astToSpec(MethodCallExpression e) {
        return {
            methodCall {
                expression.add(translate(e.objectExpression))
                constant e.methodAsString
                expression.add(translate(e.arguments))
            }
        }
    }

    Closure astToSpec(AnnotationConstantExpression e) {
        return {
            annotationConstant {
                expression.add(translate((AnnotationNode)e.value))
            }
        }
    }

    Closure astToSpec(PostfixExpression e) {
        return {
            postfix {
                expression.add(translate(e.expression))
                token e.operation.text
            }
        }
    }

    Closure astToSpec(FieldExpression e) {
        return {
            field {
                expression.add(translate(e.field))
            }
        }
    }

    Closure astToSpec(MapExpression e) {
        return {
            map {
                e.mapEntryExpressions.each { entry ->
                    expression.add(translate(entry))
                }
            }
        }
    }

    Closure astToSpec(TupleExpression e) {
        return {
            tuple {
                e.expressions.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    Closure astToSpec(MapEntryExpression e) {
        return {
            mapEntry {
                expression.add(translate(e.keyExpression))
                expression.add(translate(e.valueExpression))
            }
        }
    }

    Closure astToSpec(MethodPointerExpression e) {
        return {
            methodPointer {
                expression.add(translate(e.expression))
                expression.add(translate(e.methodName))
            }
        }
    }

    Closure astToSpec(PropertyExpression e) {
        return {
            property {
                expression.add(translate(e.objectExpression))
                expression.add(translate(e.property))
            }
        }
    }

    Closure astToSpec(RangeExpression e) {
        return {
            range {
                expression.add(translate(e.from))
                expression.add(translate(e.to))
                inclusive e.inclusive
            }
        }
    }

    Closure astToSpec(EmptyStatement e) {
        return {
            empty()
        }
    }

    Closure astToSpec(ImportNode e) {
        return {
            importNode {
                type e.type.typeClass
                alias e.alias
            }
        }
    }

    Closure astToSpec(CatchStatement e) {
        return {
            catchStatement {
                expression.add(translate(e.variable))
                expression.add(translate(e.code))
            }
        }
    }

    Closure astToSpec(ThrowStatement e) {
        return {
            throwStatement {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(SynchronizedStatement e) {
        return {
            synchronizedStatement {
                expression.add(translate(e.expression))
                expression.add(translate(e.code))
            }
        }
    }

    Closure astToSpec(ReturnStatement e) {
        return {
            returnStatement {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(TernaryExpression e) {
        return {
            ternary {
                expression.add(translate(e.booleanExpression))
                expression.add(translate(e.trueExpression))
                expression.add(translate(e.falseExpression))
            }
        }
    }

    Closure astToSpec(ElvisOperatorExpression e) {
        return {
            elvisOperator {
                expression.add(translate(e.trueExpression))
                expression.add(translate(e.falseExpression))
            }
        }
    }

    Closure astToSpec(BreakStatement e) {
        return {
            if (e.label != null) {
                breakStatement(e.label)
            } else {
                breakStatement()
            }
        }
    }

    Closure astToSpec(ContinueStatement e) {
        return {
            if (e.label != null) {
                continueStatement {
                    label(e.label)
                }
            } else {
                continueStatement()
            }
        }
    }

    Closure astToSpec(CaseStatement e) {
        return {
            expression.add(translate(e.expression))
            expression.add(translate(e.code))
        }
    }

    // Don't need to do defaultCase()

    Closure astToSpec(PrefixExpression e) {
        return {
            prefix {
                token e.operation.text
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(NotExpression e) {
        return {
            not {
                expression.add(translate(e.expression))
            }
        }
    }

    // TODO: DynamicVariable
    // TODO: exceptions
    // TODO: annotations
    // TODO: methods
    // TODO: constructors
    // TODO: properties
    // TODO: fields
    // don't need strings or values, I think.
    // don't need inclusive

    Closure astToSpec(IfStatement e) {
        return {
            ifStatement {
                expression.add(translate(e.booleanExpression))
                expression.add(translate(e.ifBlock))
                if (e.elseBlock != null) {
                    expression.add(translate(e.elseBlock))
                }
            }
        }
    }

    // TODO: SpreadExpression and SpreadMapExpression, wwhich we don't really support anyway

    Closure astToSpec(WhileStatement e) {
        return {
            whileStatement {
                expression.add(translate(e.booleanExpression))
                expression.add(translate(e.loopBlock))
            }
        }
    }

    Closure astToSpec(ForStatement e) {
        return {
            forStatement {
                expression.add(translate(e.variable))
                expression.add(translate(e.collectionExpression))
                expression.add(translate(e.loopBlock))
            }
        }
    }

    Closure astToSpec(ClosureListExpression e) {
        return {
            closureList {
                e.expressions.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    Closure astToSpec(DeclarationExpression e) {
        return {
            declaration {
                expression.add(translate(e.leftExpression))
                token e.operation.text
                if (e.rightExpression != null) {
                    expression.add(translate(e.rightExpression))
                }
            }
        }
    }

    Closure astToSpec(ListExpression e) {
        return {
            list {
                e.expressions.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    Closure astToSpec(BitwiseNegationExpression e) {
        return {
            bitwiseNegation {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(ClosureExpression e) {
        return {
            closure {
                parameters {
                    e.parameters.each { p ->
                        expression.add(translate(p))
                    }
                }
                expression.add(translate(e.code))
            }
        }
    }

    Closure astToSpec(BooleanExpression e) {
        return {
            booleanExpression {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(BinaryExpression e) {
        return {
            binary {
                expression.add(translate(e.leftExpression))
                token e.operation.text
                expression.add(translate(e.rightExpression))
            }
        }
    }

    Closure astToSpec(UnaryPlusExpression e) {
        return {
            unaryPlus {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(ClassExpression e) {
        return {
            classExpression e.type.typeClass
        }
    }

    Closure astToSpec(UnaryMinusExpression e) {
        return {
            unaryMinus {
                expression.add(translate(e.expression))
            }

        }
    }

    Closure astToSpec(AttributeExpression e) {
        return {
            attribute {
                expression.add(translate(e.objectExpression))
                expression.add(translate(e.property))
            }
        }
    }

    Closure astToSpec(ExpressionStatement e) {
        return {
            expression {
                expression.add(translate(e.expression))
            }
        }
    }

    Closure astToSpec(NamedArgumentListExpression e) {
        return {
            namedArgumentList {
                e.mapEntryExpressions.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    // TODO: interfaces, mixins, GenericTypes?

    Closure astToSpec(ClassNode e) {
        return {
            if (e.isPrimaryClassNode()) {
                classNode(e.name, e.modifiers) {
                    if (e.superClass != null) {
                        expression.add(translate(e.superClass))
                    }
                    interfaces {
                        e.interfaces.each { i ->
                            expression.add(translate(i))
                        }
                    }
                    mixins {
                        e.mixins.each { m ->
                            expression.add(translate(m))
                        }
                    }
                }
            } else {
                classNode e.typeClass
            }
        }
    }

    // Don't need parameters

    Closure astToSpec(BlockStatement e) {
        return {
            block {
                e.statements.each { s ->
                    expression.add(translate(s))
                }
            }
        }
    }

    Closure astToSpec(Parameter e) {
        return {
            parameter((e.name): e.type.typeClass)
        }
    }

    Closure astToSpec(ArrayExpression e) {
        return {
            array(e.elementType.typeClass) {
                e.expressions.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    // TODO: GenericsType
    // TODO: upperBound, lowerBound, member

    Closure astToSpec(ArgumentListExpression e) {
        return {
            if (e.expressions.isEmpty()) {
                argumentList()
            } else {
                argumentList {
                    e.expressions.each { expr ->
                        expression.add(translate(expr))
                    }
                }
            }
        }
    }

    // TODO: AnnotationNode
    // TODO: MixinNode

    Closure astToSpec(AssertStatement e) {
        return {
            assertStatement {
                expression.add(translate(e.booleanExpression))
                if (e.messageExpression != null) {
                    expression.add(translate(e.messageExpression))
                }
            }
        }
    }

    Closure astToSpec(TryCatchStatement e) {
        return {
            tryCatch {
                expression.add(translate(e.tryStatement))
                if (e.finallyStatement != null) {
                    expression.add(translate(e.finallyStatement))
                } else {
                    empty()
                }
                e.catchStatements.each { expr ->
                    expression.add(translate(expr))
                }
            }
        }
    }

    Closure astToSpec(VariableExpression e) {
        return {
            variable e.name
        }
    }

    Closure astToSpec(MethodNode e) {
        return {
            method(e.name, e.modifiers, e.returnType) {
                parameters {
                    e.parameters.each { p ->
                        expression.add(translate(p))
                    }
                }
                exceptions {
                    e.exceptions.each { c ->
                        expression.add(translate(c))
                    }
                }
                expression.add(translate(e.code))
                annotations {
                    // TODO: Translate these sucker.
                }
            }
        }
    }

    Closure astToSpec(SwitchStatement e) {
        return {
            switchStatement {
                expression.add(translate(e.expression))
                defaultCase {
                    ((BlockStatement)e.defaultStatement).statements.each { s ->
                        expression.add(translate(s))
                    }
                }
                e.caseStatements.each { c ->
                    expression.add(translate(c))
                }
            }
        }
    }

    // TODO: FieldNode
    // TODO: InnerClassNode
    // TODO: PropertyNode

    Closure astToSpec(StaticMethodCallExpression e) {
        return {
            staticMethodCall(e.ownerType.typeClass, e.methodAsString) {
                argumentList {
                    e.arguments.each { expr ->
                        expression.add(translate(expr))
                    }
                }
            }
        }
    }

    // TODO: ConstructorNode
}
