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
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
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
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
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
    public Closure astToSpec(ASTNode e) {
        throw new IllegalArgumentException("Unhandled ASTNode type: ${e.class}")
    }

    public Closure astToSpec(ConstantExpression e) {
        return {
            constant e.value
        }
    }

    public Closure astToSpec(GStringExpression e) {
        return {
            gString e.text, {
                strings {
                    e.strings.each { s ->
                        constant s.value
                    }
                }
                values {
                    e.values.each { v ->
                        astToSpec(v)
                    }
                }
            }
        }
    }

    public Closure astToSpec(CastExpression e) {
        return {
            cast(e.type.typeClass) {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(ConstructorCallExpression e) {
        return {
            constructorCall(e.type.typeClass) {
                argumentList {
                    ((TupleExpression)e.arguments).expressions.each { expr ->
                        astToSpec(expr)
                    }
                }
            }
        }
    }

    public Closure astToSpec(MethodCallExpression e) {
        return {
            methodCall {
                astToSpec(e.objectExpression)
                constant e.methodAsString
                argumentList {
                    ((TupleExpression)e.arguments).expressions.each { expr ->
                        astToSpec(expr)
                    }
                }
            }
        }
    }

    // TODO: AnnotationConstantExpression

    public Closure astToSpec(PostfixExpression e) {
        return {
            postfix {
                astToSpec(e.expression)
                token e.operation.text
            }
        }
    }

    // TODO: FieldExpression

    public Closure astToSpec(MapExpression e) {
        return {
            map {
                e.mapEntryExpressions.each { entry ->
                    astToSpec(entry)
                }
            }
        }
    }

    public Closure astToSpec(TupleExpression e) {
        return {
            tuple {
                e.expressions.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    public Closure astToSpec(MapEntryExpression e) {
        return {
            mapEntry {
                astToSpec(e.keyExpression)
                astToSpec(e.valueExpression)
            }
        }
    }

    // TODO: MethodPointerExpression
    // TODO: PropertyExpression

    public Closure astToSpec(RangeExpression e) {
        return {
            range {
                astToSpec(e.from)
                astToSpec(e.to)
                inclusive e.inclusive
            }
        }
    }

    public Closure astToSpec(EmptyStatement e) {
        return {
            empty()
        }
    }

    // TODO: label
    // TODO: ImportNode

    public Closure astToSpec(CatchStatement e) {
        return {
            catchStatement {
                astToSpec(e.variable)
                astToSpec(e.code)
            }
        }
    }

    public Closure astToSpec(ThrowStatement e) {
        return {
            throwStatement {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(SynchronizedStatement e) {
        return {
            synchronizedStatement {
                astToSpec(e.expression)
                astToSpec(e.code)
            }
        }
    }

    public Closure astToSpec(ReturnStatement e) {
        return {
            returnStatement {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(TernaryExpression e) {
        return {
            astToSpec(e.booleanExpression)
            astToSpec(e.trueExpression)
            astToSpec(e.falseExpression)
        }
    }

    public Closure astToSpec(ElvisOperatorExpression e) {
        return {
            elvisOperator {
                astToSpec(e.trueExpression)
                astToSpec(e.falseExpression)
            }
        }
    }

    // TODO: BreakStatement
    // TODO: ContinueStatement

    public Closure astToSpec(CaseStatement e) {
        return {
            astToSpec(e.expression)
            astToSpec(e.code)
        }
    }

    // Don't need to do defaultCase()

    public Closure astToSpec(PrefixExpression e) {
        return {
            token e.operation.text
            astToSpec(e.expression)
        }
    }

    public Closure astToSpec(NotExpression e) {
        return {
            not {
                astToSpec(e.expression)
            }
        }
    }

    // TODO: DynamicVariable? Probably not.
    // TODO: exceptions - probably not
    // TODO: annotations
    // TODO: methods? Do we need this?
    // TODO: constructors?
    // TODO: properties?
    // TODO: fields?
    // don't need strings or values, I think.
    // don't need inclusive

    public Closure astToSpec(IfStatement e) {
        return {
            ifStatement {
                astToSpec(e.booleanExpression)
                astToSpec(e.ifBlock)
                if (e.elseBlock != null) {
                    astToSpec(e.elseBlock)
                }
            }
        }
    }

    // TODO: SpreadExpression and SpreadMapExpression, wwhich we don't really support anyway

    public Closure astToSpec(WhileStatement e) {
        return {
            whileStatement {
                astToSpec(e.booleanExpression)
                astToSpec(e.loopBlock)
            }
        }
    }

    public Closure astToSpec(ForStatement e) {
        return {
            forStatement {
                astToSpec(e.variable)
                astToSpec(e.collectionExpression)
                astToSpec(e.loopBlock)
            }
        }
    }

    public Closure astToSpec(ClosureListExpression e) {
        return {
            closureList {
                e.expressions.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    public Closure astToSpec(DeclarationExpression e) {
        return {
            declaration {
                astToSpec(e.leftExpression)
                token e.operation.text
                if (e.rightExpression != null) {
                    astToSpec(e.rightExpression)
                }
            }
        }
    }

    public Closure astToSpec(ListExpression e) {
        return {
            list {
                e.expressions.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    public Closure astToSpec(BitwiseNegationExpression e) {
        return {
            bitwiseNegation {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(ClosureExpression e) {
        return {
            closure {
                parameters {
                    e.parameters.each { p ->
                        astToSpec(p)
                    }
                }
                astToSpec(e.code)
            }
        }
    }

    public Closure astToSpec(BooleanExpression e) {
        return {
            booleanExpression {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(UnaryPlusExpression e) {
        return {
            unaryPlus {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(ClassExpression e) {
        return {
            classExpression e.type.typeClass
        }
    }

    public Closure astToSpec(UnaryMinusExpression e) {
        return {
            unaryMinus {
                astToSpec(e.expression)
            }

        }
    }

    public Closure astToSpec(AttributeExpression e) {
        return {
            attribute {
                astToSpec(e.objectExpression)
                astToSpec(e.property)
            }
        }
    }

    public Closure astToSpec(ExpressionStatement e) {
        return {
            expression {
                astToSpec(e.expression)
            }
        }
    }

    public Closure astToSpec(NamedArgumentListExpression e) {
        return {
            namedArgumentList {
                e.mapEntryExpressions.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    // TODO: interfaces, mixins, GenericTypes?

    public Closure astToSpec(ClassNode e) {
        return {
            classNode e.typeClass
        }
    }

    // Don't need parameters

    public Closure astToSpec(BlockStatement e) {
        return {
            block {
                e.statements.each { s ->
                    astToSpec(s)
                }
            }
        }
    }

    public Closure astToSpec(Parameter e) {
        return {
            parameter((e.name): e.type.typeClass)
        }
    }

    public Closure astToSpec(ArrayExpression e) {
        return {
            array(e.elementType) {
                e.expressions.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    // TODO: GenericsType
    // TODO: upperBound, lowerBound, member

    public Closure astToSpec(ArgumentListExpression e) {
        return {
            if (e.expressions.isEmpty()) {
                argumentList()
            } else {
                argumentList {
                    e.expressions.each { expr ->
                        astToSpec(expr)
                    }
                }
            }
        }
    }

    // TODO: AnnotationNode
    // TODO: MixinNode
    // TODO: maybe - new ClassNode

    public Closure astToSpec(AssertStatement e) {
        return {
            assertStatement {
                astToSpec(e.booleanExpression)
                if (e.messageExpression != null) {
                    astToSpec(e.messageExpression)
                }
            }
        }
    }

    public Closure astToSpec(TryCatchStatement e) {
        return {
            tryCatch {
                astToSpec(e.tryStatement)
                if (e.finallyStatement != null) {
                    astToSpec(e.finallyStatement)
                } else {
                    empty()
                }
                e.catchStatements.each { expr ->
                    astToSpec(expr)
                }
            }
        }
    }

    public Closure astToSpec(VariableExpression e) {
        return {
            variable e.name
        }
    }

    public Closure astToSpec(MethodNode e) {
        return {
            method(e.name, e.modifiers, e.returnType) {
                parameters {
                    e.parameters.each { p ->
                        astToSpec(p)
                    }
                }
                exceptions {
                    e.exceptions.each { c ->
                        astToSpec(c)
                    }
                }
                astToSpec(e.code)
                annotations {
                    // TODO: Translate these sucker.
                }
            }
        }
    }

    public Closure astToSpec(SwitchStatement e) {
        return {
            switchStatement {
                astToSpec(e.expression)
                defaultCase {
                    ((BlockStatement)e.defaultStatement).statements.each { s ->
                        astToSpec(s)
                    }
                }
                e.caseStatements.each { c ->
                    astToSpec(c)
                }
            }
        }
    }

    // TODO: FieldNode
    // TODO: InnerClassNode
    // TODO: PropertyNode

    public Closure astToSpec(StaticMethodCallExpression e) {
        return {
            staticMethodCall(e.ownerType.typeClass, e.methodAsString) {
                argumentList {
                    e.arguments.each { expr ->
                        astToSpec(expr)
                    }
                }
            }
        }
    }

    // TODO: ConstructorNode
}
