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
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.MixinNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.VariableScope
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
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.SpreadMapExpression
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
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC as ACC_PUBLIC
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC as ACC_STATIC


class AstToSpecTranslatorTest extends GroovyTestCase {
    void testVariableExpression() {
        assertTranslation(new VariableExpression("foo"))
    }

    void testConstantExpression() {
        assertTranslation(new ConstantExpression("Hi there"))
    }

    void testSimpleMethodCall() {
        assertTranslation(new MethodCallExpression(
            new VariableExpression("this"),
            new ConstantExpression("println"),
            new ArgumentListExpression(
                [new ConstantExpression("Hello")]
            )
        ))
    }

    void testArgumentListExpression_NoArgs() {
        assertTranslation(new ArgumentListExpression())
    }

    void testArgumentListExpression_OneListArg() {
        assertTranslation(new ArgumentListExpression(
            [new ConstantExpression("constant1"),
             new ConstantExpression("constant2"),
             new ConstantExpression("constant3"),
             new ConstantExpression("constant4"),
            ]
        ))
    }

    void testAttributeExpression() {
        // represents foo.bar attribute invocation
        assertTranslation(new AttributeExpression(
            new VariableExpression("foo"),
            new ConstantExpression("bar")
        ))
    }

    void testIfStatement() {
        // if (foo == bar) println "Hello" else println "World"
        assertTranslation(new IfStatement(
            new BooleanExpression(
                new BinaryExpression(
                    new VariableExpression("foo"),
                    new Token(Types.COMPARE_EQUAL, "==", -1, -1),
                    new VariableExpression("bar")
                )
            ),
            new ExpressionStatement(
                new MethodCallExpression(
                    new VariableExpression("this"),
                    new ConstantExpression("println"),
                    new ArgumentListExpression(
                        [new ConstantExpression("Hello")]
                    )
                )
            ),
            new ExpressionStatement(
                new MethodCallExpression(
                    new VariableExpression("this"),
                    new ConstantExpression("println"),
                    new ArgumentListExpression(
                        [new ConstantExpression("World")]
                    )
                )
            )
        ))
    }

    void testDeclarationAndListExpression() {
        // represents def foo = [1, 2, 3]
        assertTranslation(new DeclarationExpression(
            new VariableExpression("foo"),
            new Token(Types.EQUALS, "=", -1, -1),
            new ListExpression(
                [new ConstantExpression(1),
                 new ConstantExpression(2),
                 new ConstantExpression(3),]
            )
        ))
    }

    void testArrayExpression() {
        // new Integer[]{1, 2, 3}
        assertTranslation(new ArrayExpression(
            ClassHelper.make(Integer, false),
            [
                new ConstantExpression(1),
                new ConstantExpression(2),
                new ConstantExpression(3),]
        ))
    }

    void testBitwiseNegationExpression() {
        assertTranslation(new BitwiseNegationExpression(
            new ConstantExpression(1)
        ))
    }

    void testCastExpression() {
        assertTranslation(new CastExpression(
            ClassHelper.make(Integer, false),
            new ConstantExpression("")
        ))
    }

    void testClosureExpression() {
        // { parm -> println parm }
        assertTranslation(new ClosureExpression(
            [new Parameter(
                ClassHelper.make(Object, false), "parm"
            )] as Parameter[],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            new VariableExpression("parm")
                        )
                    )
                )],
                new VariableScope()
            )
        ))
    }

    void testClosureExpression_MultipleParameters() {
        // { x,y,z -> println z }
        assertTranslation(new ClosureExpression(
            [
                new Parameter(ClassHelper.make(Object, false), "x"),
                new Parameter(ClassHelper.make(Object, false), "y"),
                new Parameter(ClassHelper.make(Object, false), "z")] as Parameter[],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            new VariableExpression("z")
                        )
                    )
                )],
                new VariableScope()
            )
        ))
    }

    void testConstructorCallExpression() {
        // new Integer(4)
        assertTranslation new ConstructorCallExpression(
            ClassHelper.make(Integer, false),
            new ArgumentListExpression(
                new ConstantExpression(4)
            )
        )
    }

    void testNotExpression() {
        // !true
        assertTranslation new NotExpression(
            new ConstantExpression(true)
        )
    }

    void testPostfixExpression() {
        // 1++
        assertTranslation new PostfixExpression(
            new ConstantExpression(1),
            new Token(Types.PLUS_PLUS, "++", -1, -1)
        )
    }

    void testPrefixExpression() {
        // ++1
        assertTranslation new PrefixExpression(
            new Token(Types.PLUS_PLUS, "++", -1, -1),
            new ConstantExpression(1)
        )
    }

    void testUnaryMinusExpression() {
        // (-foo)
        assertTranslation new UnaryMinusExpression(
            new VariableExpression("foo")
        )
    }

    void testUnaryPlusExpression() {
        // (+foo)
        assertTranslation new UnaryPlusExpression(
            new VariableExpression("foo")
        )
    }

    void testClassExpression() {
        // def foo = String
        assertTranslation new DeclarationExpression(
            new VariableExpression("foo"),
            new Token(Types.EQUALS, "=", -1, -1),
            new ClassExpression(ClassHelper.make(String, false))
        )
    }

    void testFieldExpression() {
        // public static String foo = "a value"
        def fieldNode = new FieldNode(
            "foo",
            ACC_PUBLIC | ACC_STATIC,
            ClassHelper.make(String, false),
            ClassHelper.make(this.class, false),
            new ConstantExpression("a value")
        )
        fieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(Deprecated, false)))
        assertTranslation new FieldExpression(fieldNode)
    }

    void testMapAndMapEntryExpression() {
        // [foo: 'bar', baz: 'buz']
        assertTranslation new MapExpression([
            new MapEntryExpression(new ConstantExpression('foo'), new ConstantExpression('bar')),
            new MapEntryExpression(new ConstantExpression('baz'), new ConstantExpression('buz')),
        ])
    }

    void testMapAndMapEntryExpression_SimpleCase() {
        // [foo: 'bar', baz: 'buz']
        assertTranslation new MapExpression([
            new MapEntryExpression(new ConstantExpression('foo'), new ConstantExpression('bar')),
            new MapEntryExpression(new ConstantExpression('baz'), new ConstantExpression('buz')),
            new MapEntryExpression(new ConstantExpression('qux'), new ConstantExpression('quux')),
            new MapEntryExpression(new ConstantExpression('corge'), new ConstantExpression('grault')),
        ])
    }

    void testGStringExpression() {
        // "$foo"
        assertTranslation new GStringExpression('$foo astring $bar',
            [new ConstantExpression(''), new ConstantExpression(' astring '), new ConstantExpression('')],
            [new VariableExpression('foo'), new VariableExpression('bar')])
    }

    void testMethodPointerExpression() {
        // Integer.&toString
        assertTranslation new MethodPointerExpression(
            new ClassExpression(ClassHelper.make(Integer, false)),
            new ConstantExpression("toString")
        )
    }

    void testRangeExpression() {
        // (0..10)
        assertTranslation new RangeExpression(
            new ConstantExpression(0),
            new ConstantExpression(10),
            true
        )
    }

    void testRangeExpression_Exclusive() {
        // (0..10)
        assertTranslation new RangeExpression(
            new ConstantExpression(0),
            new ConstantExpression(10),
            false
        )
    }

    void testRangeExpression_SimpleForm() {
        // (0..10)
        assertTranslation new RangeExpression(
            new ConstantExpression(0),
            new ConstantExpression(10),
            true
        )
    }

    void testPropertyExpression() {
        // foo.bar
        assertTranslation new PropertyExpression(
            new VariableExpression("foo"),
            new ConstantExpression("bar")
        )
    }

    void testSwitchAndCaseAndBreakStatements() {
        /*
                  switch (foo) {
                      case 0: break "some label"
                      case 1:
                      case 2:
                          println "<3"
                          break;
                      default:
                          println ">2"
                  }
                   */
        assertTranslation new SwitchStatement(
            new VariableExpression("foo"),
            [
                new CaseStatement(
                    new ConstantExpression(0),
                    new BreakStatement("some label")
                ),
                new CaseStatement(
                    new ConstantExpression(1),
                    EmptyStatement.INSTANCE
                ),
                new CaseStatement(
                    new ConstantExpression(2),
                    new BlockStatement(
                        [
                            new ExpressionStatement(
                                new MethodCallExpression(
                                    new VariableExpression("this"),
                                    new ConstantExpression("println"),
                                    new ArgumentListExpression(
                                        [new ConstantExpression("<3")]
                                    )
                                )
                            ),
                            new BreakStatement()
                        ], new VariableScope()
                    )
                )
            ],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            [new ConstantExpression(">2")]
                        )
                    )
                )],
                new VariableScope()
            )
        )
    }

    void testAssertStatement() {
        /*
                  assert true : "should always be true"
                  assert 1 == 2
                  */
        assertTranslation new BlockStatement(
            [
                new AssertStatement(
                    new BooleanExpression(
                        new ConstantExpression(true)
                    ),
                    new ConstantExpression("should always be true")
                ),
                new AssertStatement(
                    new BooleanExpression(
                        new BinaryExpression(
                            new ConstantExpression(1),
                            new Token(Types.COMPARE_EQUAL, "==", -1, -1),
                            new ConstantExpression(2)
                        )
                    )
                ),
            ],
            new VariableScope()
        )
    }

    void testReturnAndSynchronizedStatement() {
        /*
                  synchronized (this) {
                      return 1
                  }
          */
        assertTranslation new SynchronizedStatement(
            new VariableExpression("this"),
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression(1)
                )],
                new VariableScope()
            )
        )
    }

    void testTryCatchAndCatchAndThrowStatements() {
        /*
                  try {
                      return 1
                  } catch (Exception e) {
                       throw e
                  }
          */
        TryCatchStatement toTest = new TryCatchStatement(
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression(1)
                )],
                new VariableScope()
            ),
            EmptyStatement.INSTANCE
        )
        toTest.addCatch(
            new CatchStatement(
                new Parameter(
                    ClassHelper.make(Exception, false), "e"
                ),
                new BlockStatement(
                    [new ThrowStatement(
                        new VariableExpression("e")
                    )],
                    new VariableScope()
                )
            )
        )
        
        assertTranslation(toTest)
    }

    void testFinallyStatement() {
        /*
                  try {
                      return 1
                  } finally {
                       x.close()
                  }
          */
        TryCatchStatement expected = new TryCatchStatement(
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression(1)
                )],
                new VariableScope()
            ),
            new BlockStatement(
                [
                    new BlockStatement(
                        [
                            new ExpressionStatement(
                                new MethodCallExpression(
                                    new VariableExpression('x'),
                                    'close',
                                    new ArgumentListExpression()
                                )
                            )
                        ],
                        new VariableScope())
                ],
                new VariableScope()
            )
        )
    }

    void testForStatementAndClosureListExpression() {
        /*
              for (int x = 0; x < 10; x++) {
                  println x
              }
          */
        assertTranslation new ForStatement(
            new Parameter(ClassHelper.make(Object, false), "forLoopDummyParameter"),
            new ClosureListExpression(
                [
                    new DeclarationExpression(
                        new VariableExpression("x"),
                        new Token(Types.EQUALS, "=", -1, -1),
                        new ConstantExpression(0)
                    ),
                    new BinaryExpression(
                        new VariableExpression("x"),
                        new Token(Types.COMPARE_LESS_THAN, "<", -1, -1),
                        new ConstantExpression(10)
                    ),
                    new PostfixExpression(
                        new VariableExpression("x"),
                        new Token(Types.PLUS_PLUS, "++", -1, -1)
                    )
                ]
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new VariableExpression("this"),
                            new ConstantExpression("println"),
                            new ArgumentListExpression(
                                new VariableExpression("x"),
                            )
                        )
                    )
                ],
                new VariableScope()
            )
        )
    }

    void testStaticMethodCallExpression_MethodAsString() {
        // Math.min(1,2)
        assertTranslation new StaticMethodCallExpression(
            ClassHelper.make(Math, false),
            "min",
            new ArgumentListExpression(
                new ConstantExpression(1),
                new ConstantExpression(2)
            )
        )
    }

    void testSpreadExpression() {
        // ['foo', *['bar','baz']]
        assertTranslation new ListExpression([
            new ConstantExpression('foo'),
            new SpreadExpression(
                new ListExpression([
                    new ConstantExpression('bar'),
                    new ConstantExpression('baz'),
                ])
            )
        ])
    }

    void testSpreadMapExpression() {
        // func (*:m)
        assertTranslation new MethodCallExpression(
            new VariableExpression('this', ClassHelper.make(Object, false)),
            'func',
            new MapEntryExpression(
                new SpreadMapExpression(new VariableExpression('m', ClassHelper.make(Object, false))),
                new VariableExpression('m', ClassHelper.make(Object, false))
            )

        )
    }

    void testTernaryExpression() {
        // true ? "male" : "female"
        assertTranslation new TernaryExpression(
            new BooleanExpression(new ConstantExpression(true)),
            new ConstantExpression('male'),
            new ConstantExpression('female')
        )
    }

    void testWhileStatementAndContinueStatement() {
        /*
              while (true) {
                  x++
                  continue
              }
          */
        assertTranslation new WhileStatement(
            new BooleanExpression(
                new ConstantExpression(true)
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new PostfixExpression(
                            new VariableExpression("x"),
                            new Token(Types.PLUS_PLUS, "++", -1, -1),
                        )
                    ),
                    new ContinueStatement()
                ],
                new VariableScope()
            )
        )
    }

    void testWhileStatementAndContinueToLabelStatement() {
        /*
              while (true) {
                  x++
                  continue "some label"
              }
          */
        assertTranslation new WhileStatement(
            new BooleanExpression(
                new ConstantExpression(true)
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new PostfixExpression(
                            new VariableExpression("x"),
                            new Token(Types.PLUS_PLUS, "++", -1, -1),
                        )
                    ),
                    new ContinueStatement("some label")
                ],
                new VariableScope()
            )
        )
    }

    void testElvisOperatorExpression() {
        // name ?: 'Anonymous'
        assertTranslation new ElvisOperatorExpression(
            new VariableExpression('name'),
            new ConstantExpression('Anonymous')
        )
    }

    void testNamedArgumentListExpression() {
        // new String(foo: 'bar')
        assertTranslation new ConstructorCallExpression(
            ClassHelper.make(String),
            new TupleExpression(
                new NamedArgumentListExpression(
                    [
                        new MapEntryExpression(
                            new ConstantExpression('foo'),
                            new ConstantExpression('bar'),
                        )
                    ]
                )
            )
        )
    }

    void testParameters_DefaultValues() {
        /*
          public String myMethod(String parameter = null) {
            'some result'
          }
         */
        assertTranslation new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String.class, false),
            [new Parameter(ClassHelper.make(String, false), "parameter", new ConstantExpression(null))] as Parameter[],
            [] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression('some result')
                )],
                new VariableScope()
            ))
    }

    void testParameters_VarArgs() {
        /*
          public String myMethod(String... parameters) {
            'some result'
          }
         */
        // vararg methods are just array methods.
        assertTranslation new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String.class, false),
            [new Parameter(ClassHelper.make(String[], false), "parameters")] as Parameter[],
            [] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression('some result')
                )],
                new VariableScope()
            ))
    }

    void testInnerClassNode() {
        /*
            class Foo {
              static class Bar {
              }
            }
        */
        assertTranslation new InnerClassNode(
            new ClassNode(
                "Foo",
                ACC_PUBLIC,
                ClassHelper.make(Object, false),
                [ClassHelper.make(GroovyObject, false)] as ClassNode[],
                [] as MixinNode[]
            ),
            'Foo$Bar',
            ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [ClassHelper.make(GroovyObject, false)] as ClassNode[],
            [] as MixinNode[]
        )
    }

    void testConstructorNode() {

        // public <init>(String foo, Integer bar) throws IOException, Exception {}
        assertTranslation new ConstructorNode(
            ACC_PUBLIC,
            [
                new Parameter(ClassHelper.make(String, false), "foo"),
                new Parameter(ClassHelper.make(Integer, false), "bar")
            ] as Parameter[],
            [
                ClassHelper.make(Exception, false),
                ClassHelper.make(IOException, false)
            ] as ClassNode[],
            new BlockStatement()
        )
    }

    void testGenericsType() {
        // class MyClass<T, U extends Number> {}
        def toTest = new ClassNode(
            "MyClass", ACC_PUBLIC, ClassHelper.make(Object, false)
        )
        toTest.setGenericsTypes(
            [
                new GenericsType(ClassHelper.make(Object, false)),
                new GenericsType(ClassHelper.make(Number, false), [ClassHelper.make(Number, false)] as ClassNode[], null),
            ] as GenericsType[]
        )
        assertTranslation(toTest)
    }

    void testClassWithMethods() {
        // class MyClass {
        //   String myProp = 'foo'
        //   String myMethod(String parameter) throws IOException { 'some result' }
        //   String myOtherMethod() { 'some other result' }
        // }
        def toTest = new ClassNode("MyClass", ACC_PUBLIC, ClassHelper.make(Object, false))
        toTest.addAnnotation(new AnnotationNode(ClassHelper.make(Deprecated, false)))
        def pNode = new PropertyNode("myProp", ACC_PUBLIC, ClassHelper.make(String, false),
            ClassHelper.make(this.class, false), new ConstantExpression("foo"), null, null)
        pNode.addAnnotation(new AnnotationNode(ClassHelper.make(Deprecated, false)))
        toTest.addProperty(pNode)
        toTest.addMethod(new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            [new Parameter(ClassHelper.make(String, false), "parameter")] as Parameter[],
            [ClassHelper.make(IOException, false)] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(new ConstantExpression('some result'))], new VariableScope()
            )))
        toTest.addMethod(new MethodNode(
            "myOtherMethod",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            [] as Parameter[],
            [] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(new ConstantExpression('some other result'))], new VariableScope()
            )))

        assertTranslation(toTest)
    }

    void testGenericsType_WithLowerBounds() {
        // class MyClass<T, U extends Number> {}
        def toTest = new ClassNode(
            "MyClass", ACC_PUBLIC, ClassHelper.make(Object, false)
        )
        toTest.setGenericsTypes(
            [
                new GenericsType(ClassHelper.make(Object, false)),
                new GenericsType(
                    ClassHelper.make(Number, false),
                    [ClassHelper.make(Number, false), ClassHelper.make(Comparable, false)] as ClassNode[],
                    ClassHelper.make(Integer, false)),
            ] as GenericsType[]
        )
        
        assertTranslation(toTest)
    }

    void testImportNode() {
        // what source will trigger this node?
        assertTranslation new ImportNode(ClassHelper.make(String, false), "string")
        assertTranslation new ImportNode(ClassHelper.make(Integer, false), null)
    }

    void testMethodNode() {
        /*
          @Override
          public String myMethod(String parameter) throws Exception, IOException {
            'some result'
          }
        }
         */
        def toTest = new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            [new Parameter(ClassHelper.make(String, false), "parameter")] as Parameter[],
            [ClassHelper.make(Exception, false), ClassHelper.make(IOException, false)] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression('some result')
                )],
                new VariableScope()
            ))
        toTest.addAnnotation(new AnnotationNode(ClassHelper.make(Override, false)))

        assertTranslation(toTest)
    }

    void testAnnotation_WithParameter() {
        // @org.junit.Test(timeout=50L) def myMethod() {}
        def toTest = new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [] as Parameter[],
            [] as ClassNode[],
            new BlockStatement([], new VariableScope()))

        def annotation = new AnnotationNode(ClassHelper.make(Override, false))
        annotation.setMember('timeout', new ConstantExpression(50L))
        toTest.addAnnotation(annotation)

        assertTranslation(toTest)
    }


    void testMixinNode() {

        // todo: what source code will generate a MixinNode?
        assertTranslation new ClassNode(
            "MyClass", ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [ClassHelper.make(GroovyObject, false)] as ClassNode[],
            [
                new MixinNode("ClassA", ACC_PUBLIC, ClassHelper.make(String, false)),
                new MixinNode(
                    "ClassB",
                    ACC_PUBLIC,
                    ClassHelper.make(String, false),
                    [ClassHelper.make(GroovyObject, false)] as ClassNode[]), // interfaces
            ] as MixinNode[]
        )
    }

    void testPropertyNode() {
        //  def myField = "foo"
        assertTranslation new PropertyNode(
            "MY_VALUE",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            ClassHelper.make(this.class, false),
            new ConstantExpression("foo"),
            null,
            null        //todo: do we need to support getter and setter blocks?
        )
    }


    void testAnnotationConstantExpression() {
        assertTranslation new AnnotationConstantExpression(
            new AnnotationNode(
                ClassHelper.make(Override.class, false)
            )
        )
    }
    void assertTranslation(ASTNode astNode) {
        def result = new AstBuilder().buildFromSpec(new AstToSpecTranslator().astToSpec(astNode))

        AstAssert.assertSyntaxTree([astNode], result)
    }

}
