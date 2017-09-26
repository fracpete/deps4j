# deps4j
Tools for determining dependencies of classes. 

## Minimal dependencies
The following class allows you to determine the minimal set of classes
required to make a supplied list of classes compile:
```
com.github.fracpete.deps4j.MinDeps
```

### Help
```
usage: com.github.fracpete.deps4j.MinDeps
       [-h] --java-home JAVAHOME --class-path CLASSPATH
       --classes CLASSES [--additional ADDITIONAL] [--output OUTPUT]
       packages [packages ...]

positional arguments:
  packages               The packages to keep, eg 'weka'.

optional arguments:
  -h, --help             show this help message and exit
  --java-home JAVAHOME   The java home directory  of  the JDK that includes
                         the jdeps binary, default  is taken from JAVA_HOME
                         environment variable.
  --class-path CLASSPATH
                         The CLASSPATH to use for jdeps.
  --classes CLASSES      The file containing the  classes  to determine the
                         dependencies for. Empty  lines  and lines starting
                         with # get ignored.
  --additional ADDITIONAL
                         The file  with  additional  class  names  to  just
                         include.
  --output OUTPUT        The file for  storing  the  determined class names
                         in.
```

### Example
**Use case:** determine a minimal subset of classes for an embedded version of Weka.

The following Weka classes are required (`./classes.txt`):
```
weka.classifiers.Evaluation
weka.classifiers.functions.Logistic
weka.classifiers.functions.MultilayerPerceptron
weka.core.Attribute
weka.core.DenseInstance
weka.core.Instances
weka.core.SerializationHelper
weka.filters.Filter
weka.filters.unsupervised.attribute.Discretize
weka.filters.supervised.instance.Resample
```

The JDK is located at `/some/where/jdk1.8.0_144-64bit` and the Weka 
jar at `./weka.jar`. We are looking for Weka packages, hence
using `weka` as positional argument.

This gives the following commandline, which will output the determined 
classes on stdout:
```bash
com.github.fracpete.deps4j.MinDeps \
  --java-home
  /some/where/jdk1.8.0_144-64bit
  --classes
  ./classes.txt
  --class-path
  ./weka.jar
  weka
```
