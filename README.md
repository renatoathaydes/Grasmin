# Grasmin
> Write Jasmin (JVM assembly) code directly in your Groovy files

Grasmin allows you to write [Jasmin](http://jasmin.sourceforge.net/) code (which is basically JVM assembly bytecode instructions)
directly on your Groovy files by annotating methods with the `@JasminCode` annotation.

This annotation is a Groovy AST Transformation, which allows manipulation of classes during the compilation process.

For example, you could write `Hello World` as follows:

```groovy
class Hello {

  @JasminCode
  static void main(args) {
     """
     .limit stack 2
     getstatic java/lang/System/out Ljava/io/PrintStream;
     ldc "Hello World!"
     invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
     return
     """
  }

}
```

## Using Grasmin

Currently, you need to clone this repo and build it with Gradle:

```
gradle install
```

This will put the following artifact in your local Maven repo:

```
group = 'com.athaydes.grasmin'
name = 'grasmin-core'
version = 0.1
```

If you don't use Maven, just do `gradle jar` to create a jar in the `build/libs` folder.

Currently, only methods can be annotated with `@JasminCode`. Annotated methods can have any signature.
If your method needs to return a particular type, just add a dummy value as the last statement of your method to
make your IDE compiler happy... during Groovy compilation, `Grasmin` will only use the first statement of your
method (which should be a String or a property that evaluates to a String), ignoring any subsequent statements.

The following method returns the sum of two integers **(notice the use of both `@JasminCode` and `@GroovyStatic` -
you almost always want to use them together to avoid the cost of calling a method via Groovy's normal dynamic method
dispatch)**:

```groovy
class Hello {
    @CompileStatic
    @JasminCode
    int sum( int a, int b ) {
        """
        .limit stack 2
        .limit locals 2
        iload_0
        iload_1
        iadd
        ireturn
        """
        0 // ignored, but makes the IDE happy
    }
}
```

## How it works (currently)

Grasmin turns any method annotated with `@JasminCode` into a call to a static method (called `run`) of a class created by
**Jasmin** from the Jasmin code provided in the annotated method.

The example above (the `sum` function) produces the following class, as output by `javap`:

```
Compiled from "Hello.groovy"
public class com.athaydes.grasmin.Hello implements groovy.lang.GroovyObject {

  // ... lots of Java/Groovy boilerplate

  public int sum(int, int);
    Code:
       0: iload_1       
       1: iload_2       
       2: invokestatic  #121                // Method com_athaydes_grasmin_Hello_sum.run:(II)I
       5: ireturn       
       6: ldc           #35                 // int 0
       8: ireturn       

  // ... more boilerplate
}
```

> the two last lines in `sum` are dead-code, probably introduced by the Groovy compiler as a default return value

And the new class produced with Jasmin to hold the implementation of `sum`:

```
Compiled from "1291300980882852.j"
public class com_athaydes_grasmin_Hello_sum {
  public static int run(int, int);
    Code:
       0: iload_0       
       1: iload_1       
       2: iadd          
       3: ireturn       
}
```

The above is the whole output of `javap` (without the *verbose* key).
The `j` file mentioned in the first line is the temporary file used by Grasmin as input for Jasmin.

## Future and performance

Unfortunately, delegating a method call to a static method of an external class does not seem to be very efficient for
a short algorithm, at least, so gains in performance cannot be guaranteed! However, I am sure there would be cases where
handcrafted Assembly cannot be beaten either by `javac` or `JIT` optimizations. I would love to hear of any examples.

There are some performance tests in [this directory](grasmin-tests/src/test/groovy/grasmin/) which show that, for example,
writing the simple GCD Euclidean algorithm in Jasmin is actually less efficient than just writing the non-recursive algorithm
in either Java or Groovy (Groovy with @CompileStatic actually runs consistently faster than Java).

Here are some results:

> each row represents 10_000 runs of the GCD algorithm with a pair of random integers, run 100 times with each implementation
 (values are average time taken in nano-seconds)

Java         | Groovy @CompileStatic | Grovy @TailRecursive | @JasminCode
-------------|-----------------------|----------------------|------------
2.794.043,06 | 2.497.728,88          | 2.870.736,07         | 3.249.961,17
2.915.521,62 | 2.684.497,9           | 2.813.709,1          | 3.455.433,63

Implementations:

* Java - non-recursive algorithm written in Java
* Groovy @CompileStatic - non-recursive algorithm written in Groovy, annotated with @CompileStatic
* Groovy @TailRecursive - recursive algorithm written in Groovy, annotated with both @CompileStatic and @TaileRecursive
* @JasminCode - non-recursive algorithm written in Jasmin assembly