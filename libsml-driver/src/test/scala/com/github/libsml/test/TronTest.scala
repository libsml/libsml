package com.github.libsml.test

import com.github.libsml.model.classification.{LogisticRegressionModel, LogisticRegression}
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{SingleBinaryClassificationMetrics, BinaryClassificationMetrics}
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/8/18.
 */
object TronTest {


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
      val scoreAndLabels = data.map(lv => {
        (lrm.probability(lv.features, r.w), lv.label)
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
    tronTest()
  }
}
