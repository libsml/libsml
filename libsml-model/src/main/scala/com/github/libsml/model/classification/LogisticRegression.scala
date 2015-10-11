package com.github.libsml.model.classification


import com.github.libsml.model.classification.LogisticRegression._
import com.github.libsml.math.function.Function
import com.github.libsml.math.util.{VectorUtils, MLMath}
import com.github.libsml.model.Model
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.model.data.DataUtils
import com.github.libsml.model.data.WeightedLabeledVector
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import com.github.libsml.commons.util.RDDFunctions._
import com.github.libsml.commons.util.MapWrapper._

/**
 * Created by huangyu on 15/7/26.
 */

class LogisticRegressionModel(var w: Vector) extends Model[Vector, Vector] {

  def this() = {
    this(null)
  }

  def this(w: Vector, threshold: Double) = {
    this(w)
    this.threshold = threshold
  }

  def this(w: Vector, map: Map[String, String]) = {
    this(w)
    this.threshold = map.getDouble("logisticModel.threshold", threshold)
  }

  private var threshold: Double = 0.5

  override def update(w: Vector): this.type = {
    this.w = w
    this
  }

  override def score(testPoint: Vector, k: Double): Double = {
    k match {
      case 1 =>
        1 / (1 + Math.exp(-xv(testPoint, w)))
      case _ =>
        1 - 1 / (1 + Math.exp(-xv(testPoint, w)))
    }
  }

  override def value(testPoint: Vector): Double = {
    val p = 1 / (1 + Math.exp(-xv(testPoint, w)))
    if (p >= threshold) 1 else 0
  }

  override def save(path: String): Unit = {
    DataUtils.writeVectorAddition2Avro(path, w, threshold)
  }

}


class LogisticRegression(val data: RDD[WeightedLabeledVector], val reduceNum: Int,
                         val featureNum: Int = -1, val classNum: Int = 2)
  extends Function[Vector] {


  def this(data: RDD[WeightedLabeledVector], map: Map[String, String]) = {
    this(data, map.getInt("reduceNum", 1), map.getInt("featureNumber", -1), map.getInt("classNumber", 2))
  }


  require(featureNum > 0, s"Spark logistic regression exception:featureNumber=${featureNum}")
  require(reduceNum > 0, s"Spark logistic regression exception:reduceNumber=${reduceNum}")
  require(classNum > 0, s"Spark logistic regression exception:classNumber=${classNum}")

  private[this] var wBroadcast: Broadcast[Vector] = null

  override def isInBound(w: Vector): Boolean = true

  //  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double = {
  //    f
  //  }


  private[this] def gradient(w: Vector, g: Vector, setZero: Boolean,
                             reduceNum: Int, featureNum: Int, classNum: Int): (Vector, Double) = {

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
      classNum match {
        case 2 =>
          while (ds.hasNext) {
            val d = ds.next()
            loss += gradientBinary(d.features, d.label, weight, gradient, d.weight)
          }
        case _ =>
          while (ds.hasNext) {
            val d = ds.next()
            loss += gradientMultinomial(d.features, d.label, weight, gradient, classNum, featureNum, d.weight)
          }
      }
      //      Seq((loss, gradient)).iterator
      VectorUtils.vectorWithAttachIterator(gradient, loss)
    }).reduceByKey(_ + _, reduceNum)
    //      slaveReduce((lossGradient1, lossGradient2) => {
    //      BLAS.axpy(1, lossGradient1._2, lossGradient2._2)
    //      (lossGradient1._1 + lossGradient2._1, lossGradient2._2)
    //    }, slaveNum)
    val loss = getVectorFromRdd(g, rdd)

    //    BLAS.axpy(1, gradient, g)
    (g, loss)
  }

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {
    gradient(w, g, setZero, reduceNum, featureNum, classNum)
  }

  override def isDerivable: Boolean = true


  def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean,
                    setZero: Boolean, reduceNum: Int, featureNum: Int, classNum: Int): Unit = {
    if (setZero) {
      BLAS.zero(hv)
    }

    val dBroadcast = data.sparkContext.broadcast(d)
    val _wBroadcast = wBroadcast
    //    val hessianVector = {
    classNum match {
      case 2 =>
        val rdd = data.mapPartitions(ds => {
          val weight = _wBroadcast.value
          val dWeight = dBroadcast.value
          val hv: Vector = VectorUtils.newVectorAs(weight)
          while (ds.hasNext) {
            val data = ds.next()
            hessianVectorBinary(data.features, data.label, weight, dWeight, hv, dw = data.weight)
          }
          //            Seq(hv).iterator
          VectorUtils.vectorIterator(hv)
        }
        ).reduceByKey(_ + _, reduceNum)
        getVectorFromRdd(hv, rdd)
      //        rdd.unpersist()
      //            .slaveReduce((hv1, hv2) => {
      //            BLAS.axpy(1, hv1, hv2)
      //            hv2
      //          }, slaveNum)
      case _ =>
        val rdd = data.mapPartitions(ds => {
          val weight = _wBroadcast.value
          val dWeight = dBroadcast.value
          val hv: Vector = VectorUtils.newVectorAs(weight)
          while (ds.hasNext) {
            val data = ds.next()
            hessianVectorMultinomial(data.features, data.label, weight, dWeight, hv, classNum, featureNum, dw = data.weight)
          }
          //            Seq(hv).iterator
          VectorUtils.vectorIterator(hv)
        }
        )
        getVectorFromRdd(hv, rdd)
      //        rdd.unpersist()
      //            .slaveReduce((hv1, hv2) => {
      //            BLAS.axpy(1, hv1, hv2)
      //            hv2
      //          }, reduceNum)
      //      }

    }

    //    BLAS.axpy(1, hessianVector, hv)


  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {
    hessianVector(w, d, hv, isUpdateHessian, setZero, reduceNum, featureNum, classNum)

  }

  override def isSecondDerivable: Boolean = true

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {
    throw new UnsupportedOperationException("Invert hessian vector!")
  }
}


class SingleLogisticRegressionLoss(val data: Array[WeightedLabeledVector],
                                   val featureNum: Int = -1, val classNum: Int = 2)
  extends Function[Vector] {

  def this(data: Array[WeightedLabeledVector], map: Map[String, String]) = {
    this(data, map.getInt("featureNumber", -1), map.getInt("classNumber", 2))
  }

  private var D: Option[Array[Double]] = None
  private var KD: Option[Array[Array[Double]]] = None

  override val isDerivable: Boolean = true

  //  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double = {
  //    f
  //  }


  override def gradient(w: Vector, g: Vector, setZero: Boolean = true): (Vector, Double) = {


    if (setZero) {
      BLAS.zero(g)
    }

    var fx: Double = 0f

    classNum match {
      case 2 =>
        var i = 0
        while (i < data.length) {

          fx += gradientBinary(data(i).features, data(i).label, w, g, data(i).weight)
          i += 1
        }

      case _ =>

        var i = 0
        while (i < data.length) {
          fx += gradientMultinomial(data(i).features, data(i).label, w, g, classNum, featureNum, data(i).weight)
          i += 1
        }

    }
    (g, fx)

  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean = true): Unit = {

    if (setZero) {
      BLAS.zero(hv)
    }

    classNum match {
      case 2 =>
        var update = isUpdateHessian
        if (!D.isDefined) {
          update = true
          D = Some(new Array[Double](data.length))
        }
        var i = 0
        while (i < data.length) {
          hessianVectorBinary(data(i).features, data(i).label, w, d, hv, i, update, D, data(i).weight)
          i += 1
        }
      case _ =>

        var update = isUpdateHessian
        if (!KD.isDefined) {
          update = true
          KD = Some(Array.tabulate(data.length) { i => new Array[Double](classNum - 1) })
        }
        var i = 0
        while (i < data.length) {
          hessianVectorMultinomial(data(i).features, data(i).label, w, d, hv,
            classNum, featureNum, i, update, KD, data(i).weight)
          i += 1
        }

    }

  }

  override val isSecondDerivable: Boolean = true

  //  private def getDataWeight(i: Int): Double = {
  //    if (dataWeight.isDefined) (dataWeight.get)(i) else 1
  //  }

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {
    throw new UnsupportedOperationException("Invert hessian vector!")
  }

  override def isInBound(w: Vector): Boolean = true
}


object LogisticRegression {

  def apply(data: Array[WeightedLabeledVector]): Function[Vector] = {
    new SingleLogisticRegressionLoss(data)
  }


  def xv(x: Vector, v: Vector): Double = {
    BLAS.dot(x, v)
  }

  def xTv(x: Vector, v: Double, xTv: Vector) = {

    x.foreachNoZero((index, value) => {
      xTv(index) = _ + v * value
    })

  }

  /**
   * For Binary Logistic Regression.
   *
   * Although the loss and gradient calculation for multinomial one is more generalized,
   * and multinomial one can also be used in binary case, we still implement a specialized
   * binary version for performance reason.
   */
  def gradientBinary(x: Vector, label: Double, w: Vector, g: Vector, dw: Double = 1): Double = {


    val y = if (label == 1) 1 else -1
    val yz: Double = xv(x, w) * y
    val z: Double = 1 / (1 + Math.exp(-yz))
    if (dw == 1) {
      xTv(x, y * (z - 1), g)
      MLMath.log1pExp(-yz)
    } else {
      xTv(x, y * (z - 1) * dw, g)
      MLMath.log1pExp(-yz) * dw
    }

  }

  /**
   * For Multinomial Logistic Regression.
   */
  def gradientMultinomial(x: Vector, label: Double, w: Vector, g: Vector, numClasses: Int,
                          featureNum: Int, dw: Double = 1): Double = {

    // marginY is margins(label - 1) in the formula.
    var marginY = 0.0
    var maxMargin = Double.NegativeInfinity
    var maxMarginIndex = 0

    val margins = Array.tabulate(featureNum - 1) { i =>
      var margin = 0.0
      x.foreachNoZero { (index, value) =>
        if (value != 0.0) margin += value * w((i * featureNum) + index)
      }
      if (i == label.toInt - 1) marginY = margin
      if (margin > maxMargin) {
        maxMargin = margin
        maxMarginIndex = i
      }
      margin
    }

    /**
     * When maxMargin > 0, the original formula will cause overflow as we discuss
     * in the previous comment.
     * We address this by subtracting maxMargin from all the margins, so it's guaranteed
     * that all of the new margins will be smaller than zero to prevent arithmetic overflow.
     */
    val sum = {
      var temp = 0.0
      if (maxMargin > 0) {
        for (i <- 0 until numClasses - 1) {
          margins(i) -= maxMargin
          if (i == maxMarginIndex) {
            temp += math.exp(-maxMargin)
          } else {
            temp += math.exp(margins(i))
          }
        }
      } else {
        for (i <- 0 until numClasses - 1) {
          temp += math.exp(margins(i))
        }
      }
      temp
    }

    for (i <- 0 until numClasses - 1) {
      var multiplier = math.exp(margins(i)) / (sum + 1.0) - {
        if (label != 0.0 && label == i + 1) 1.0 else 0.0
      }
      multiplier *= dw
      x.foreachNoZero { (index, value) =>
        if (value != 0.0) g(i * featureNum + index) = _ + multiplier * value
      }
    }
    val loss = if (label > 0.0) math.log1p(sum) - marginY else math.log1p(sum)

    if (maxMargin > 0) {
      (loss + maxMargin) * dw
    } else {
      loss * dw
    }
  }


  def hessianVectorBinary(x: Vector, label: Double, w: Vector, d: Vector, hv: Vector, i: Int = -1,
                          isUpdate: Boolean = true, D: Option[Array[Double]] = None, dw: Double = 1): Unit = {

    def computeD(x: Vector, y: Double, w: Vector): Double = {

      val z = (1 / (1 + Math.exp(-xv(x, w) * y)))
      z * (1 - z)
    }

    val y = if (label == 1) 1 else -1

    val di = {
      if (isUpdate) computeD(x, y, w)
      else (D.get)(i)
    }
    if (isUpdate && D.isDefined) {
      (D.get)(i) = di
    }
    xTv(x, xv(x, d) * dw * di, hv)

  }


  def hessianVectorMultinomial(x: Vector, y: Double, w: Vector, d: Vector, hv: Vector, classNum: Int, featureNum: Int, i: Int = -1,
                               isUpdate: Boolean = true, KD: Option[Array[Array[Double]]] = None, dw: Double = 1): Unit = {

    val kd = KD.map(_(i)).getOrElse(new Array[Double](classNum - 1))

    def computeKD(kd: Array[Double]): Array[Double] = {


      var maxMargin = Double.NegativeInfinity
      var maxMarginIndex = 0

      var i = 0
      while (i < classNum) {
        var margin = 0.0
        x.foreachNoZero { (index, value) =>
          if (value != 0.0) margin += value * w((i * featureNum) + index)
        }
        if (margin > maxMargin) {
          maxMargin = margin
          maxMarginIndex = i
        }
        kd(i) = margin
        i += 1
      }

      val sum = {
        var temp = 0.0
        if (maxMargin > 0) {
          for (i <- 0 until classNum - 1) {
            kd(i) -= maxMargin
            if (i == maxMarginIndex) {
              temp += math.exp(-maxMargin)
            } else {
              temp += math.exp(kd(i))
            }
          }
        } else {
          for (i <- 0 until classNum - 1) {
            temp += math.exp(kd(i))
          }
        }
        temp
      }

      i = 0
      while (i < classNum) {

        kd(i) /= (1 + sum)
        i += 1
      }

      kd
    }

    def computePi(k1: Int, k2: Int, kd: Array[Double]) = {
      if (k1 == k2) kd(k1) * (1 - kd(k2))
      else -kd(k1) * kd(k2)
    }

    computeKD(kd)

    for (k1 <- 0 until classNum) {

      for (k2 <- 0 until classNum) {

        var xv: Double = 0
        x.foreachNoZero((index, value) => {
          xv += value * d(k2 * featureNum + index)
        })

        x.foreachNoZero((index, value) => {
          hv(k1 * featureNum + index) = _ + value * computePi(k1, k2, kd) * xv * dw
        })

      }

    }


  }

  def getVectorFromRdd(vector: Vector, rdd: RDD[(Int, Double)]): Double = {
    var attach: Double = 0

    val rddc = if (rdd.partitions.length < 200) rdd else rdd.coalesce(100)
    //    BLAS.zero(vector)

    //    val parts = rddc.partitions
    //    for (p <- parts) {
    //      val idx = p.index
    //      val partRdd = rddc.mapPartitionsWithIndex((index, it) => if (index == idx) it else Iterator[(Int, Double)](), true)
    //      val dataPartitioned = partRdd.collect
    //      var i: Int = 0
    //      while (i < dataPartitioned.length) {
    //        if (dataPartitioned(i)._1 == -1) {
    //          attach = dataPartitioned(i)._2
    //        } else {
    //          vector(dataPartitioned(i)._1) = dataPartitioned(i)._2
    //        }
    //        i += 1
    //      }
    //    }

    rddc.collect().foreach(kv => {
      if (kv._1 == -1) {
        attach = kv._2
      } else {
        vector(kv._1) = _ + kv._2
      }
    })
    attach
  }


}
