package com.github.libsml.function.imp.spark

import com.github.libsml.data.liblinear.DataPoint
import com.github.libsml.function.imp.spark.SparkLogisticRegression._
import com.github.libsml.{MLContext, Config}
import com.github.libsml.function.LossFunction
import com.github.libsml.data.RDDDatas._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

/**
 * Created by huangyu on 15/6/6.
 */
class SparkLogisticRegression(val mlCtx: MLContext) extends LossFunction {


  private[this] val ctx = mlCtx.sparkContext()
  private[this] val dataPoints=mlCtx.getInputDataRDD

  private val bias = mlCtx.getBias
  private val l2C = mlCtx.getL2C
  private var wBroad: Broadcast[Array[Float]] = null

  private var oldK = -1

  override def lossAndGrad(w: Array[Float], g: Array[Float], k: Int): Float = {
    if (k > oldK) {

      if (wBroad != null) {
        wBroad.unpersist()
      }

      wBroad = ctx.broadcast(w)
      oldK = k
    }
    lossAndGrad(mlCtx.getInputDataRDD(), w, wBroad, bias, l2C, g, k)

  }

  private def lossAndGrad(dataPoints: RDD[DataPoint],
                          weightMem: Array[Float],
                          wBroad: Broadcast[Array[Float]],
                          bias: Float, l2C: Float,
                          g: Array[Float], k: Int) = {

    var reduceNum = weightMem.length / REDUCE_LEGHT
    if (reduceNum > REDUCE_MAX_NUM) {
      reduceNum = REDUCE_MAX_NUM
    }

    var fun: Float = 0
    var lgRDD = dataPoints.mapPartitions(ds => {
      val weight = wBroad.value
      var loss: Double = 0
      val gradient: Array[Double] = new Array[Double](weight.length)
      while (ds.hasNext) {
        val data = ds.next()
        val w: Float = data.weight
        val y: Float = data.y
        val yz: Double = xv(data, weight, bias) * y

        loss += (if (yz >= 0) Math.log(1 + Math.exp(-yz)) * w else (-yz + Math.log(1 + Math.exp(yz))) * w)

        val z: Double = 1 / (1 + Math.exp(-yz))

        xTv(data, y * (z - 1) * w, gradient, bias)
      }

      //      ((-1, loss) :: generateListFromArray(gradient)).iterator
      generateLossAndGradientIterator(gradient, loss)

    }).reduceByKey(_ + _, reduceNum)

    //L2 norm
    lgRDD = l2Norm(lgRDD, wBroad, l2C)
    lgRDD.persist()

    //save iteration result
    if (mlCtx.isSaveIterationResult) {
      //TODO:save iteration result
    }
    fun = readLossAndGradientFromRDD(lgRDD, g)
    lgRDD.unpersist()

    fun
  }


  private def lossAndGradLessMemory(dataPoints: RDD[DataPoint],
                                    weightMem: Array[Float],
                                    wBroad: Broadcast[Array[Float]],
                                    bias: Float, l2C: Float,
                                    g: Array[Float], k: Int) = {

    var reduceNum = weightMem.length / REDUCE_LEGHT
    if (reduceNum > REDUCE_MAX_NUM) {
      reduceNum = REDUCE_MAX_NUM
    }

    var fun: Float = 0
    var lgRDD = dataPoints.mapPartitions(ds => {
      new Iterator[(Int, Double)] {
        val weight = wBroad.value
        var isLossed = false
        var loss: Double = 0
        var data: DataPoint = null
        var index = 0

        override def hasNext: Boolean = {
          if (data == null || index >= data.index.length) {
            if (ds.hasNext) {
              true
            } else {
              !isLossed
            }
          } else {
            true
          }
        }

        override def next() = {
          if (data == null || index >= data.index.length) {
            data = ds.next()
            index = 0
          }
          if (ds.hasNext) {
            index += 1

            val w: Float = data.weight
            val y: Float = data.y
            val yz: Double = xv(data, weight, bias) * y

            loss += (if (yz >= 0) Math.log(1 + Math.exp(-yz)) * w else (-yz + Math.log(1 + Math.exp(yz))) * w)

            val gradientTemp: Double = (1 / (1 + Math.exp(-yz)) - 1) * y * w

            (data.index(index - 1), gradientTemp * data.value(index - 1))

          } else {
            isLossed = true
            (-1, loss)
          }
        }
      }
    }).reduceByKey(_ + _, reduceNum)

    //L2 norm
    lgRDD = l2Norm(lgRDD, wBroad, l2C)
    lgRDD.persist()

    //save iteration result
    if (mlCtx.isSaveIterationResult) {
      //TODO:save iteration result
    }
    fun = readLossAndGradientFromRDD(lgRDD, g)
    lgRDD.unpersist()

    fun
  }


  override def Hv(w: Array[Float], d: Array[Float], Hs: Array[Float], k: Int, cgIter: Int): Unit = {
    Hv(w, wBroad, bias, l2C, dataPoints.sparkContext.broadcast(d), Hs, k, cgIter)
  }

  def Hv(weightMem: Array[Float],
         wBroad: Broadcast[Array[Float]],
         bias: Float, l2C: Float,
         dBroad: Broadcast[Array[Float]],
         Hs: Array[Float],
         k: Int, cgIter: Int): Unit = {

    var reduceNum = weightMem.length / REDUCE_LEGHT
    if (reduceNum > REDUCE_MAX_NUM) {
      reduceNum = REDUCE_MAX_NUM
    }

    var lgRDD = dataPoints.mapPartitions(ds => {
      val weight = wBroad.value
      val dWeight = dBroad.value
      val hv: Array[Double] = new Array[Double](weight.length)
      while (ds.hasNext) {
        val data = ds.next()
        var d = 1 / (1 + Math.exp(-xv(data, weight, bias) * data.y))
        d = d * (1 - d)
        xTv(data, xv(data, dWeight, bias) * data.weight * d, hv, bias)
      }
      generateArrayIterator(hv)
    }).reduceByKey(_ + _, reduceNum)

    //L2 norm
    lgRDD = l2Norm(lgRDD, dBroad, l2C)

    lgRDD.persist()
    readArrayFromRDD(lgRDD, Hs)
    lgRDD.unpersist()
  }

  def HvLessMemory(weightMem: Array[Float],
                   wBroad: Broadcast[Array[Float]],
                   bias: Float, l2C: Float,
                   dBroad: Broadcast[Array[Float]],
                   Hs: Array[Float],
                   k: Int, cgIter: Int): Unit = {

    var reduceNum = weightMem.length / REDUCE_LEGHT
    if (reduceNum > REDUCE_MAX_NUM) {
      reduceNum = REDUCE_MAX_NUM
    }

    var lgRDD = dataPoints.mapPartitions(ds => {
      new Iterator[(Int, Double)] {

        val weight = wBroad.value
        val dWeight = dBroad.value
        var data: DataPoint = null
        var index = 0

        override def hasNext: Boolean = {
          if (data == null || index >= data.index.length) {
            ds.hasNext
          } else {
            true
          }
        }

        override def next(): (Int, Double) = {
          if (data == null || index >= data.index.length) {
            data = ds.next()
            index = 0
          }

          index += 1
          var d = 1 / (1 + Math.exp(-xv(data, weight, bias) * data.y))
          d = d * (1 - d)
          d = xv(data, dWeight, bias) * data.weight * d
          (data.index(index - 1), data.value(index - 1) * d)
        }
      }
    }).reduceByKey(_ + _, reduceNum)

    //L2 norm
    lgRDD = l2Norm(lgRDD, dBroad, l2C)

    lgRDD.persist()
    readArrayFromRDD(lgRDD, Hs)
    lgRDD.unpersist()
  }

}

object SparkLogisticRegression {

  val REDUCE_LEGHT = 500000
  val REDUCE_MAX_NUM = 100

  def xv(x: DataPoint, v: Array[Float], b: Float): Double = {
    var xv: Double = 0
    var i = 0
    while (i < x.index.length) {
      xv += x.value(i) * v(x.index(i))
      i += 1
    }
    if (b > 0) {
      xv += v(v.length - 1) * b
    }
    xv
  }

  def xTv(x: DataPoint, v: Double, xTv: Array[Double], b: Float) {
    var i = 0
    while (i < x.index.length) {
      xTv(x.index(i)) += v * x.value(i)
      i += 1
    }
    xTv(xTv.length - 1) += b * v
  }

  def l2Norm(rdd: RDD[(Int, Double)], wBroad: Broadcast[Array[Float]], l2C: Float) = {

    if (l2C > 0) {
      rdd.map(d => {
        val weight = wBroad.value
        if (d._1 < 0) {
          var l2Norm: Double = 0
          var i: Int = 0
          while (i < weight.length) {
            l2Norm += weight(i) * weight(i)
            i += 1
          }
          (d._1, d._2 + 0.5 * l2C * l2Norm)
        } else {
          (d._1, d._2 + l2C * weight(d._1))
        }
      }
      )
    }
    else rdd
  }


  def main(args: Array[String]) {
    println("ss")
  }

}
