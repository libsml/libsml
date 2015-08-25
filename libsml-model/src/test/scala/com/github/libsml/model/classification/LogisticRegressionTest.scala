package com.github.libsml.model.classification

import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.data.LabeledVector
import com.github.libsml.math.function.Function._
import com.github.libsml.model.regularization.L2Regularization

/**
 * Created by huangyu on 15/8/14.
 */
object LogisticRegressionTest {

  def modelTest(): Unit = {
    val a = Array(1.0, 3.0)
    val model = new LogisticRegressionModel(Vector(a))

    assert(model.probability(Vector(Array(1.0))) == 1.0 / (1.0 + Math.exp(-1.0)))
  }

  def singleLossTest(): Unit = {
    val data = Array(new LabeledVector(1, Vector(Array(1.0, 0.0))),
      new LabeledVector(-1, Vector(Array(0.0, 1.0))))
    val lr = LogisticRegression(data)
    val w = Vector(Array(1.0, 1.0))
    val g = Vector(Array(0.0, 0.0))
    val d = Vector(Array(1.0, 1.0))
    val hv = Vector(Array(0.0, 0.0))

    val l2 = new L2Regularization(1.0)

    val l2lr = lr + l2

    println(l2lr.gradient(w, g))
    g.foreachNoZero((i, v) => println(i + ":" + v))
    l2lr.hessianVector(w, d, hv, true)
    hv.foreachNoZero((i, v) => println(i + ":" + v))

  }

  def main(args: Array[String]): Unit = {
    //    modelTest()
    singleLossTest()
  }

}
