package com.github.libsml.test

import java.io.PrintWriter

import com.github.libsml.driver.optimization.DefaultOptimizationAlgorithm
import com.github.libsml.math.function.Function._
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.classification.{LogisticRegression, LogisticRegressionModel}
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator, SingleBinaryClassificationMetrics}
import com.github.libsml.model.regularization.L2Regularization
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}

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

    val data = DataUtils.loadSVMData(1, 124, "dataset/a9a.txt")
    val methods = Array("areaUnderROC", "areaUnderPR")
    val lrm = new LogisticRegressionModel(Vector())
    val lr = LogisticRegression(data)
    val tron = new Tron(Vector(), new LiblinearParameter(), lr)
    val evaluator = new BinaryDefaultEvaluator(Right(data), methods)
    val algorithm = new DefaultOptimizationAlgorithm(tron, lrm, Some(evaluator))
    algorithm.optimize()

  }

  def tronTest(): Unit = {

    //    val lrm = new LogisticRegressionModel(Vector())
    val lrm = new LogisticRegressionModel(Vector(new Array[Double](30000)))
    val data = DataUtils.loadSVMData(1, 30000, "dataset/hy_all")
    val lr = LogisticRegression(data) + new L2Regularization(1.0)
    val tron = new Tron(Vector(), new LiblinearParameter(maxIterations = 500), lr)
    //    val it = tron.iterator()
    //    while (it.hasNext) {
    //      println(it.next())
    //    }
    var iter = 0
    for (r <- tron) {
      lrm.update(r.w)
      val scoreAndLabels = data.map(lv => {
        (lrm.score(lv.features, 1.0), lv.label)
      })
      val evaluator = new SingleBinaryClassificationMetrics(scoreAndLabels, 20)
      println(r.msg)
      println("auc:" + evaluator.areaUnderROC())
      //      println("recall:")
      //      evaluator.precisionByThreshold().foreach(d => {
      //        print(d + " ")
      //      })
      iter += 1
      //      if (iter % 10 == 0) {
      val p = new PrintWriter("result/hy_model_sch/" + (iter), "utf-8")
      r.w.foreachNoZero((i, v) => {
        p.println(i + "\t" + v)
      })
      p.close()
      //      }
    }
    val p = new PrintWriter("result/hy_model_sch/" + (iter), "utf-8")
    lrm.w.foreachNoZero((i, v) => {
      p.println(i + "\t" + v)
    })
    p.close()

  }

  def lbfgsTest(): Unit = {

    val lrm = new LogisticRegressionModel(Vector(new Array[Double](1411607)))
    val data = DataUtils.loadSVMData(1, 1411606, "dataset/part-00166")
    val lr = LogisticRegression(data)
    //    val lbfgs = new Tron(Vector(), new LiblinearParameter(), lr)
    val lbfgs = new LBFGS(Vector(), lr)
    //    val it = tron.iterator()
    //    while (it.hasNext) {
    //      println(it.next())
    //    }
    var iter = 0
    for (r <- lbfgs) {
      println(s"iter:${iter}")
      print("f:")
      r.f.foreach(println _)
      lrm.update(r.w)
      val scoreAndLabels = data.map(lv => {
        (lrm.score(lv.features, 1.0), lv.label)
      })
      val evaluator = new SingleBinaryClassificationMetrics(scoreAndLabels, 20)
      //      println(r.msg)
      println("auc:" + evaluator.areaUnderROC())
      //      println("recall:")
      //      evaluator.precisionByThreshold().foreach(d => {
      //        print(d + " ")
      //      })
      iter += 1
    }
  }

  def main(args: Array[String]) {
    //    tronTest()
    //    evaluatorTest()
    algorithmTest()
    //    lbfgsTest()
    //    tronTest()
  }
}
