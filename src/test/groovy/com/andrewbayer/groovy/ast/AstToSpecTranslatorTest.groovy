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
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression


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

    void assertTranslation(ASTNode astNode) {
        def result = new AstBuilder().buildFromSpec(new AstToSpecTranslator().astToSpec(astNode))

        AstAssert.assertSyntaxTree([astNode], result)
    }

}
