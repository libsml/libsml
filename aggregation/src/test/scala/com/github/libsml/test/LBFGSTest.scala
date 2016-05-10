package com.github.libsml.test

import com.github.libsml.model.classification.{LogisticRegression, LogisticRegressionModel}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator}
import com.github.libsml.model.regularization.{L1Regularization, L2Regularization}
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.function.Function._
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by yellowhuang on 2016/5/4.
 */
object LBFGSTest {
  def test(): Unit = {


    val sc = new SparkContext(new SparkConf().setAppName("test"))
    //    val data = DataUtils.loadSVMData(1, "data/sample_libsvm_data.txt")
    val (data, featureNum) = DataUtils.loadSVMData2RDD(sc, 1.0, "data/sample_libsvm_data.txt")
    val parallelNum = 2
    // Split data into training (60%) and test (40%).
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0).cache()
    val test = splits(1)

    //    val data = DataUtils.loadSVMData(1, 124, "dataset/a9a.txt")
    //    val methods = Array("areaUnderROC", "areaUnderPR")
    val methods = Array("areaUnderROC")
    val lr = new LogisticRegression(data, featureNum, parallelNum) + new L1Regularization(1.) + new L2Regularization(1.0)
    //    val lr = LogisticRegression(data) + new L2Regularization(1.0)
    //    val lr = LogisticRegression(data) + new L1Regularization(1.0)
    //    val lr = LogisticRegression(data)
    //    val lr = LogisticRegression(data)
    val para = Map[String,String]("lbfgs.maxIterations"->"100")
    val op = new LBFGS(Vector(), para, lr)
    //    val op = new Tron(Vector(), new LiblinearParameter(), lr)
    val evaluator = new BinaryDefaultEvaluator(Left(test), methods, testOutput = Some(System.out))
    val logisticRegressionModel = new LogisticRegressionModel()
    var iter = 0
    for (r <- op) {
      logisticRegressionModel.update(r.w)
      println(s"############iter:${iter}###################")
      println("loss:" + r.f.get)
      println("nozero:" + r.w.noZeroSize)
      evaluator.evaluator(new LogisticRegressionModel(r.w))
      iter += 1
    }
    logisticRegressionModel.save("lr_model")
  }

  def main(args: Array[String]) {
    test()
  }

}
