#Large Scale Machine Learning Library.

LIBSML is a large-scale machine learning library. Its goal is to make practical machine learning scalable and easy.
It consists of common learning models, including logistic regression, linear supporting vector machine,
collaborative filtering, as well as effective optimization methods, including L-BFGS, TRON, SGD, FTRL.

##Using LIBSML as a Library

LIBSML is built using Apache Maven. To compile from source:

    mvn -DskipTests clean install
    To run tests do mvn test

To use maven, add the appropriate setting to your pom.xml.

    <dependency>
        <groupId>com.github.libsml</groupId>
        <artifactId>libsml-aggregation</artifactId>
        <version>1.0</version>
    </dependency>

##Examples

The following code illustrates how to load a sample dataset, split it into train and test, and use optimization method
to fit a machine learning model, such as logistic regression, linear supporting vector machine.
Then the model is evaluated against the test dataset and saved to disk.

```scala
import com.github.libsml.model.classification.LogisticRegressionModel
import com.github.libsml.model.classification.{LogisticRegression, LogisticRegressionModel}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator, SingleBinaryClassificationMetrics}
import com.github.libsml.model.regularization.{L1Regularization, L2Regularization}
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.function.Function._
```

##[Distributed L-BFGS](https://github.com/libsml/libsml/tree/master/libsml-lbfgs)

A distributed implemention of L-BFGS, supporting L1-regularization, L2-regularization and three modes(local, mr and spark) to train/test a model.

##[Distributed liblinear](https://github.com/libsml/libsml/tree/master/libsml-liblinear)

A distributed implemention of liblinear, supporting TRON and three modes(local, mr and spark) to train/test a model.
