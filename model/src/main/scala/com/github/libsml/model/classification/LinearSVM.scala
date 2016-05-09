package com.github.libsml.model.classification

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.model.Model
import com.github.libsml.model.data.{DataUtils, WeightedLabeledVector}
import com.github.libsml.commons.util.MapWrapper._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import com.github.libsml.model.Utils._


class LinearSVMModel(var w: Vector) extends Model[Vector, Vector] {

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
    k match {
      case 1 =>
        BLAS.dot(testPoint, w)
      case _ =>
        throw new UnsupportedOperationException("LinearSVM exception.")
    }
  }

  override def value(testPoint: Vector): Double = {
    val p = score(testPoint, 1)
    if (p >= 0.0) 1 else 0
  }

  override def save(path: String): Unit = {
    DataUtils.writeVector2Avro(path, w)
  }

}

class SingleLinearSVM(val data: Array[WeightedLabeledVector]) extends Function[Vector] {

  def this(data: Array[WeightedLabeledVector], map: Map[String, String]) = {
    this(data)
  }

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {

    if (setZero) {
      BLAS.zero(g)
    }
    (g, 0.0)
  }


  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): (Vector, Double) = {
    BLAS.copy(g, sg)
    var fx: Double = f
    var i = 0
    while (i < data.length) {
      fx += LinearSVM.subGradient(data(i).features, data(i).label, w, sg, data(i).weight)
      i += 1
    }
    (sg, fx)
  }

  override def isDerivable: Boolean = false

  override def isSecondDerivable: Boolean = false

  override def isInBound(w: Vector): Boolean = true

}

class LinearSVM(val data: RDD[WeightedLabeledVector], val reduceNum: Int)
  extends Function[Vector] {

  private[this] var wBroadcast: Broadcast[Vector] = null

  def this(data: RDD[WeightedLabeledVector], map: Map[String, String]) = {
    this(data, map.getInt("reduceNum", 1))
  }

  require(reduceNum > 0, s"Spark linearSVM regression exception:reduceNumber=${reduceNum}")

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {

    if (setZero) {
      BLAS.zero(g)
    }
    (g, 0.0)
  }


  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): (Vector, Double) = subGradient(w, f, g, sg, reduceNum)

  private[this] def subGradient(w: Vector, f: Double, g: Vector, sg: Vector, reduceNum: Int): (Vector, Double) = {

    BLAS.copy(g, sg)

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
        loss += LinearSVM.subGradient(d.features, d.label, weight, gradient, d.weight)
      }
      VectorUtils.vectorWithAttachIterator(gradient, loss)
    }
    ).reduceByKey(_ + _, reduceNum)
    val loss = getVectorFromRdd(sg, rdd) + f
    (sg, loss)

  }

  override def isDerivable: Boolean = false

  override def isSecondDerivable: Boolean = false

  override def isInBound(w: Vector): Boolean = true

}

/**
 * Created by yellowhuang on 2016/5/9.
 */
object LinearSVM {
  /**
   * :: DeveloperApi ::
   * Compute gradient and loss for a Hinge loss function, as used in SVM binary classification.
   * See also the documentation for the precise formulation.
   */
  def subGradient(x: Vector, label: Double, w: Vector, g: Vector, dw: Double = 1): Double = {

    val y = if (label == 1) 1 else -1
    val yz: Double = BLAS.dot(x, w) * y
    if (yz < 1.0) {
      BLAS.axpy(-y * dw, x, g)
      (1 - yz) * dw
    } else {
      0.0
    }

  }

}
