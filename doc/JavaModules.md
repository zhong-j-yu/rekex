
# Rekex and Java Modules

Rekex is modularized in 
[Java modules](https://www.oracle.com/corporate/features/understanding-java-9-modules.html)

    Java module     <-->     Maven artifactId

    org.rekex.parser         rekex-parser
    org.rekex.grammar        rekex-grammar
    org.rekex.regexp         rekex-regexp
    org.rekex.common_util    rekex-common_util

If your application is not modularized with Java modules,
there is nothing to worry about. 
Rekex modules are simply jars on the classpath.

If your application is modularized with Java modules,
you are just asking for trouble:) At minimum,
- your module `requires org.rekex.parser;`
- your module `opens` to all rekex modules listed above  

It is simpler to make your module as an `open` module.
See [module-info.java](../rekex-example-modular/src/main/java/module-info.java)
in [rekex-example-modular/](../rekex-example-modular)

`PegParserBuilder.build()` will 
- generate a `.java` file for the parser,
- compile it (in an unnamed module),
- load the compiled class
- create an instance of the class.

If it doesn't work, you may try `PegParserBuilder.javacOptions(...)`
with [javac options](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html) 
about modules.

You may also just invoke `PegParserBuilder.generateJavaFile()`
to put the `.java` file on your source directory,
then compile it in a separate build step.

