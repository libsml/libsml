package com.github.libsml.test

import com.github.libsml.model.classification.LogisticRegressionModel
import com.github.libsml.model.classification.{LogisticRegression, LogisticRegressionModel}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator, SingleBinaryClassificationMetrics}
import com.github.libsml.model.regularization.{L1Regularization, L2Regularization}
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.function.Function._

/**
 * Created by yellowhuang on 2016/5/4.
 */
object LBFGSTest {
  def test(): Unit = {

    val data = DataUtils.loadSVMData(1, 124, "dataset/a9a.txt")
    //    val methods = Array("areaUnderROC", "areaUnderPR")
    val methods = Array("areaUnderROC")
    val lr = LogisticRegression(data) + new L1Regularization(1.) + new L2Regularization(1.0)
    //    val lr = LogisticRegression(data)
    val para = Map[String,String]("lbfgs.maxIterations"->"100")
    val op = new LBFGS(Vector(), para, lr)
    //    val op = new Tron(Vector(), new LiblinearParameter(), lr)
    val evaluator = new BinaryDefaultEvaluator(Right(data), methods, testOutput = Some(System.out))
    var iter = 0
    for (r <- op) {
      println(s"############iter:${iter}###################")
      println("loss:" + r.f.get)
      println("nozero:" + r.w.noZeroSize)
      evaluator.evaluator(new LogisticRegressionModel(r.w))
      iter += 1
    }
  }

  def main(args: Array[String]) {
    test()
  }

}
