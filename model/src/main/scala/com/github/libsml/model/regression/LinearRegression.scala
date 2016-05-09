package com.github.libsml.model.regression

import com.github.libsml.commons.util.MapWrapper._
import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.model.Model
import com.github.libsml.model.Utils._
import com.github.libsml.model.data.{DataUtils, WeightedLabeledVector}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD


class LinearRegressionModel(var w: Vector) extends Model[Vector, Vector] {

  def this() = {
    this(null)
  }

  def this(w: Vector, map: Map[String, String]) = {
    this(w)
  }

  override def update(w: Vector): this.type = {
    this.w = w
    this
  }

  override def score(testPoint: Vector, k: Double): Double = {
    BLAS.dot(testPoint, w)
  }

  override def value(testPoint: Vector): Double = {
    score(testPoint, 1)
  }

  override def save(path: String): Unit = {
    DataUtils.writeVector2Avro(path, w)
  }

}

class SingleLinearRegression(val data: Array[WeightedLabeledVector]) extends Function[Vector] {

  def this(data: Array[WeightedLabeledVector], map: Map[String, String]) = {
    this(data)
  }

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {

    if (setZero) {
      BLAS.zero(g)
    }
    var fx: Double = 0.0
    var i = 0
    while (i < data.length) {
      fx += LinearRegression.gradient(data(i).features, data(i).label, w, g, data(i).weight)
      i += 1
    }
    (g, fx)
  }


  override def isDerivable: Boolean = false

  override def isSecondDerivable: Boolean = false

  override def isInBound(w: Vector): Boolean = true

}

class LinearRegression(val data: RDD[WeightedLabeledVector], val reduceNum: Int)
  extends Function[Vector] {

  private[this] var wBroadcast: Broadcast[Vector] = null

  def this(data: RDD[WeightedLabeledVector], map: Map[String, String]) = {
    this(data, map.getInt("reduceNum", 1))
  }

  require(reduceNum > 0, s"Spark linearSVM regression exception:reduceNumber=${reduceNum}")

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {
    gradient(w, g, setZero, reduceNum)
  }


  private[this] def gradient(w: Vector, g: Vector, setZero: Boolean, reduceNum: Int): (Vector, Double) = {

    if (setZero) {
      BLAS.zero(g)
    }

    if (wBroadcast != null) {
      wBroadcast.unpersist()
    }
    wBroadcast = data.sparkContext.broadcast(w)
    val _wBroadcast = wBroadcast
    val rdd = data.mapPartitions(ds => {
      val weight = _wBroadcast.value
      var loss: Double = 0
      val gradient: Vector = VectorUtils.newVectorAs(weight)
      while (ds.hasNext) {
        val d = ds.next()
        loss += LinearRegression.gradient(d.features, d.label, weight, gradient, d.weight)
      }
      VectorUtils.vectorWithAttachIterator(gradient, loss)
    }
    ).reduceByKey(_ + _, reduceNum)
    val loss = getVectorFromRdd(g, rdd)
    (g, loss)

  }

  override def isDerivable: Boolean = false

  override def isSecondDerivable: Boolean = false

  override def isInBound(w: Vector): Boolean = true

}

/**
 * Created by yellowhuang on 2016/5/9.
 */
object LinearRegression {

  /**
   * :: DeveloperApi ::
   * Compute gradient and loss for a Least-squared loss function, as used in linear regression.
   * This is correct for the averaged least squares loss function (mean squared error)
   * L = 1/2n ||A weights-y||^2
   */
  def gradient(x: Vector, label: Double, w: Vector, g: Vector, dw: Double = 1): Double = {

    val diff = (BLAS.dot(x, w) - label)
    BLAS.axpy(diff * dw, x, g)
    diff * diff * dw / 2.0

  }

}
