## What is this?

This is an experiment - an attempt to write something that can translate from 
[Apache Groovy](http://groovy-lang.org)'s AST to the specification DSL consumed
by the [`AstBuilder.buildFromSpec`](http://docs.groovy-lang.org/next/html/gapi/org/codehaus/groovy/ast/builder/AstBuilder.html#buildFromSpec(groovy.lang.Closure))
method, which itself generates AST from that specification DSL. 

## Ok, but why? That seems...circuitous.

It is! I am not denying this. As I'm starting this, I'm working on an AST 
transformation to translate a [builder-like DSL](https://jenkins.io/doc/book/pipeline/) 
into corresponding model classes directly, without having to go with the 
[rather demented `Closure` evaluation](https://github.com/jenkinsci/pipeline-model-definition-plugin/blob/6c65ef8bb0b186f0d1b9c1c81a6bf40e61e1b56d/pipeline-model-definition/src/main/resources/org/jenkinsci/plugins/pipeline/modeldefinition/ClosureModelTranslator.groovy) 
I've been doing historically. 

Certain specific requirements make that...well, 
hard - like trying to allow environment variables to reference each other and
be declared in any order. To get out of the hole of bad hacks (loops of 
evaluations until everything's resolved!) I've put myself in, I'm starting from
as close to scratch as I can, with the only requirement being that the existing
syntax continues to work.

On top of that, there are two (or three, sorta) other cases of 
`CompilationCustomizer`s interacting with the code - one that drives both 
[validation of the model and translating to other formats](https://github.com/jenkinsci/pipeline-model-definition-plugin/blob/master/pipeline-model-definition/src/main/groovy/org/jenkinsci/plugins/pipeline/modeldefinition/parser/ModelParser.groovy),
and the [Jenkins Pipeline CPS transformer](https://github.com/cloudbees/groovy-cps/blob/75903bafce3a46a235260ae0446e77294d2abea1/lib/src/main/java/com/cloudbees/groovy/cps/CpsTransformer.java).
Combining all this, I really, really want to cut out the multiple layers of
middlemen and just transform to the runtime model from the DSL via AST
transformation.

## Wait, I get *that*, but why `AstBuilder.buildFromSpec` from existing AST?

Yeah, here's where I got goofy and fell down an experimental rabbit hole. To
properly translate the AST into the model, I need to know my context within 
the model, and I found that a comparatively simple 
`ClassCodeExpressionTransformer`-derived solution was a pain due to state
tracking, etc. So, I started playing with `AstBuilder.buildFromSpec`. But!
Sometimes, I want to grab a chunk of AST directly from the original AST and 
copy it into a new place in the translated-to-model AST. There's no elegant way
I can find to mash up `AstBuilder.buildFromSpec`'s easy of writing and reuse of
existing AST chunks. 

*EDIT*: Turns out I was wrong. `expression.add(ASTNode)` actually does the 
trick there. So...this may be stupid. But still, gonna play with it a bit more.

But what if I could convert to the specification DSL consumed by 
`AstBuilder.buildFromSpec` *from* existing AST nodes? Well, that'd be handy!

## Right, that's a lot of writing. Have you actually got anything working?

Kinda yeah! Not sure if this actually worthwhile, and I still have some 
`ASTNode`s left to transform (and some various `BUG! ClassNode#getTypeClass 
for MyClass is called before the type class is set` errors I have to figure
out), but I'm a *lot* farther along than I was, like, an hour and a half ago.
