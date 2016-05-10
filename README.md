#Large Scale Machine Learning Library.

LIBSML is a large-scale machine learning library. Its goal is to make practical machine learning scalable and easy.
It consists of common learning models, including linear regression, logistic regression, linear supporting vector machine,
collaborative filtering, as well as effective optimization methods, including L-BFGS, TRON, FTRL.

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
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator}
import com.github.libsml.model.regularization.{L1Regularization, L2Regularization}
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.function.Function._
import org.apache.spark.{SparkConf, SparkContext}

// Load training data in LIBSVM format.
val (data, featureNum) = DataUtils.loadSVMData2RDD(sc, 1.0, "data/sample_libsvm_data.txt")

// Split data into training (60%) and test (40%).
val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)
val training = splits(0).cache()
val test = splits(1)

val parallelNum = 2
val methods = Array("areaUnderROC")
//Logistic regression with L1 and L2 regularization
val lr = new LogisticRegression(data, featureNum, parallelNum) + new L1Regularization(1.) + new L2Regularization(1.0)
//Logistic regression with L1 regularization
//val lr = new LogisticRegression(data, featureNum, parallelNum) + new L1Regularization(1.)
//Logistic regression with L2 regularization
//val lr = new LogisticRegression(data, featureNum, parallelNum) + new L2Regularization(1.)
//Logistic regression without regularization
//val lr = new LogisticRegression(data, featureNum, parallelNum)

//L-BFGS configuration
val conf = Map[String,String]("lbfgs.maxIterations"->"100")

//Using MapVector
val w = Vector()
//Using DenseVector
//val w = Vector(new Array[Double](featureNum))

//Using L-BFGS as optimizer
val op = new LBFGS(w, conf, lr)
//Using TRON as optimizer
//Note:TRON doesn't support L1 regularization
//val op = new Tron(Vector(), new LiblinearParameter(), lr)
val logisticRegressionModel = new LogisticRegressionModel()
for (r <- op) {
    logisticRegressionModel.update(r.w)
    evaluator.evaluator(logisticRegressionModel)
}
logisticRegressionModel.save("lr_model")

```

