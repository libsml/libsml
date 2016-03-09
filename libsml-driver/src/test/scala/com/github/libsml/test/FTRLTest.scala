package com.github.libsml.test

import java.io.PrintWriter

import com.github.libsml.driver.optimization.DefaultOptimizationAlgorithm
import com.github.libsml.math.function.Function._
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.classification.{LogisticRegression, LogisticRegressionModel}
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator, SingleBinaryClassificationMetrics}
import com.github.libsml.model.regularization.L2Regularization
import com.github.libsml.optimization.FTRL
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
 * Created by huangyu on 15/8/18.
 */
object FTRLTest {


  def test(dataFile: String, resultFile: String, featureNum: Int) = {

    val ds = new ArrayBuffer[(Long, Vector, Double)]()
    Source.fromFile(dataFile, "utf-8").getLines.foreach(line => {
      val ss = line.split("\\s+")
      val label = ss(0).toInt
      val is = new Array[Int](ss.length)
      val vs = new Array[Double](ss.length)
      for (i <- 1 until ss.length) {
        val kv = ss(i).split(":")
        is(i - 1) = kv(0).toInt
        vs(i - 1) = kv(1).toDouble
      }
      is(ss.length - 1) = featureNum
      vs(ss.length - 1) = 1
      ds += ((1, Vector(is, vs), label))
    })
    val op = new FTRL(1.0, 1.0, 1.0, 1.0, 10, 0.5, 0.05, 0.001, 0.001)
    //    for (i <- 0 until 20)
    ds.foreach(d => {
      op.update(d._2, d._3, 1.0)
    })
    val p = new PrintWriter(resultFile, "utf-8")
    op.weight().foreachNoZero((k, v) => {
      p.println(k + "\t" + v)
    })
    p.close()

    val zp = new PrintWriter("result/z", "utf-8")
    op.getZ().foreachNoZero((k, v) => {
      zp.println(k + "\t" + v)
    })
    zp.close()
    val np = new PrintWriter("result/n", "utf-8")
    op.getN().foreachNoZero((k, v) => {
      np.println(k + "\t" + v)
    })
    np.close()

  }

  def main(args: Array[String]): Unit = {
    test("dataset/hy_all", "result/ftrl", 693654)
  }

}
