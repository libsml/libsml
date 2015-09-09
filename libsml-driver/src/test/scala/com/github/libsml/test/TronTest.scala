package com.github.libsml.test

import com.github.libsml.driver.optimization.DefaultOptimizationAlgorithm
import com.github.libsml.model.classification.{LogisticRegressionModel, LogisticRegression}
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{SingleBinaryClassificationMetrics, BinaryClassificationMetrics}
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.evaluation.BinaryDefaultEvaluator

/**
 * Created by huangyu on 15/8/18.
 */
object TronTest {


  def evaluatorTest(): Unit = {
    val data = DataUtils.loadSVMData(1, 124, "data/a9a.txt")
    val methods = Array("areaUnderROC", "areaUnderPR")
    val lrm = new LogisticRegressionModel(Vector())
    val lr = LogisticRegression(data)
    val tron = new Tron(Vector(), new LiblinearParameter(), lr)
    val evaluator = new BinaryDefaultEvaluator(Right(data), methods)
    for (r <- tron) {
      lrm.update(r.w)
      evaluator.evaluator(lrm)
    }

  }

  def algorithmTest(): Unit = {

    val data = DataUtils.loadSVMData(1, 124, "data/a9a.txt")
    val methods = Array("areaUnderROC", "areaUnderPR")
    val lrm = new LogisticRegressionModel(Vector())
    val lr = LogisticRegression(data)
    val tron = new Tron(Vector(), new LiblinearParameter(), lr)
    val evaluator = new BinaryDefaultEvaluator(Right(data), methods)
    val algorithm = new DefaultOptimizationAlgorithm(tron, lrm,Some(evaluator))
    algorithm.optimize()

  }

  def tronTest(): Unit = {

    val lrm = new LogisticRegressionModel(Vector())
    val data = DataUtils.loadSVMData(1, 124, "data/a9a.txt")
    val lr = LogisticRegression(data)
    val tron = new Tron(Vector(), new LiblinearParameter(), lr)
    //    val it = tron.iterator()
    //    while (it.hasNext) {
    //      println(it.next())
    //    }
    for (r <- tron) {
      lrm.update(r.w)
      val scoreAndLabels = data.map(lv => {
        (lrm.score(lv.features, 1.0), lv.label)
      })
      val evaluator = new SingleBinaryClassificationMetrics(scoreAndLabels, 20)
      println(r.msg)
      println("auc:" + evaluator.areaUnderROC())
      println("recall:")
      evaluator.precisionByThreshold().foreach(d => {
        print(d + " ")
      })
    }
  }

  def main(args: Array[String]) {
    //    tronTest()
    //    evaluatorTest()
    algorithmTest()
  }
}
