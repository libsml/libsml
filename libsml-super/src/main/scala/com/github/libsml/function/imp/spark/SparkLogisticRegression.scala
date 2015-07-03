package com.github.libsml.function.imp.spark

import com.github.libsml.MLContext
import com.github.libsml.commons.util.HadoopUtils
import com.github.libsml.data.avro.Entry
import com.github.libsml.data.liblinear.DataPoint
import com.github.libsml.data.liblinear.RDDData._
import com.github.libsml.function.LossFunction
import com.github.libsml.function.imp.spark.SparkLogisticRegression._
import org.apache.avro.mapred.AvroKey
import org.apache.avro.mapreduce.AvroKeyOutputFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

/**
 * Created by huangyu on 15/6/6.
 */
class SparkLogisticRegression(val dataPoints: RDD[DataPoint],
                              val bias: Float,
                              val l2C: Float,
                              val lessMemory: Boolean = false) extends LossFunction {


  private[this] val sc = dataPoints.sparkContext
  private[this] var reduceByKeyNum = getExecutorNum(sc)
  private[this] var saveIterationResult = false
  private[this] var mode = MLContext.LOCAL_AVRO
  private[this] var rootPath: String = null
  private var wBroad: Broadcast[Array[Float]] = null

  def this(dataPoints: RDD[DataPoint],
           bias: Float,
           l2C: Float,
           lessMemory: Boolean,
           saveIterationResult: Boolean,
           mode: String,
           rootPath: String,
           reduceByKeyNum: Int = -1) {

    this(dataPoints, bias, l2C, lessMemory)
    this.reduceByKeyNum = if (reduceByKeyNum <= 0) this.reduceByKeyNum else reduceByKeyNum
    this.saveIterationResult = saveIterationResult
    this.mode = mode
    this.rootPath = rootPath
  }

  override def lossAndGrad(w: Array[Float], g: Array[Float], k: Int): Float = {
    if (wBroad != null) {
      wBroad.unpersist()
    }
    wBroad = sc.broadcast(w)
    if (!lessMemory) lossAndGrad(dataPoints, w, wBroad, bias, l2C, g, k)
    else lossAndGradLessMemory(dataPoints, w, wBroad, bias, l2C, g, k)

  }

  private def lossAndGrad(dataPoints: RDD[DataPoint],
                          weightMem: Array[Float],
                          wBroad: Broadcast[Array[Float]],
                          bias: Float, l2C: Float,
                          g: Array[Float], k: Int) = {

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

    }).reduceByKey(_ + _, reduceByKeyNum)
    //L2 norm
    lgRDD = l2Norm(lgRDD, wBroad, l2C)
    lgRDD.persist()

    //save iteration result
    if (saveIterationResult) {
      saveResultFromRDD(lgRDD, k, mode, rootPath + "/w", sc.hadoopConfiguration)
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

    var fun: Float = 0
    var lgRDD = dataPoints.mapPartitions(ds => {
      new Iterator[(Int, Double)] {
        val weight = wBroad.value
        var isLossed = false
        var loss: Double = 0
        var data: DataPoint = null
        var index = 0
        var dsHasNext = false

        override def hasNext: Boolean = {
          if (data == null || index >= data.index.length) {
            dsHasNext = ds.hasNext
            if (dsHasNext) {
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
            if (dsHasNext) {
              data = ds.next()
              index = 0
            }
          }

          if (dsHasNext) {
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
    }

    ).reduceByKey(_ + _, reduceByKeyNum)

    //L2 norm
    lgRDD = l2Norm(lgRDD, wBroad, l2C)
    lgRDD.persist()

    //save iteration result
    if (saveIterationResult) {
      saveResultFromRDD(lgRDD, k, mode, rootPath + "/w", sc.hadoopConfiguration)
    }
    fun = readLossAndGradientFromRDD(lgRDD, g)
    lgRDD.unpersist()

    fun
  }


  override def Hv(w: Array[Float], d: Array[Float], Hs: Array[Float], k: Int, cgIter: Int): Unit = {
    if (!lessMemory) Hv(w, wBroad, bias, l2C, dataPoints.sparkContext.broadcast(d), Hs, k, cgIter)
    else HvLessMemory(w, wBroad, bias, l2C, dataPoints.sparkContext.broadcast(d), Hs, k, cgIter)
  }

  def Hv(weightMem: Array[Float],
         wBroad: Broadcast[Array[Float]],
         bias: Float, l2C: Float,
         dBroad: Broadcast[Array[Float]],
         Hs: Array[Float],
         k: Int, cgIter: Int): Unit = {


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
    }).reduceByKey(_ + _, reduceByKeyNum)

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
    }).reduceByKey(_ + _, reduceByKeyNum)

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

  val REDUCE_SUM_NUMBER = "reduce.sum.number"

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

  def xTv(x: DataPoint, v: Double, hv: Array[Double], b: Float) {
    var i = 0
    while (i < x.index.length) {
      hv(x.index(i)) += v * x.value(i)
      i += 1
    }
    hv(hv.length - 1) += b * v
  }

  def saveResultFromRDD(rdd: RDD[(Int, Double)], k: Int, mode: String, path: String,
                        configuration: Configuration): Unit = {

    HadoopUtils.delete(configuration, new Path(path))
    if (MLContext.HDFS_TEXT == mode) {
      configuration.set(TextOutputFormat.SEPERATOR, ":")
      rdd.saveAsNewAPIHadoopFile(path, classOf[Int], classOf[Double], classOf[TextOutputFormat[Int, Double]])
      //      rdd.saveAsHadoopFile(path,)
    } else if (MLContext.HDFS_AVRO == mode) {
      rdd.map(l => {
        (new Entry(l._1, l._2.toFloat), NullWritable.get())
      }).
        saveAsNewAPIHadoopFile(path, classOf[AvroKey[Entry]], classOf[NullWritable], classOf[AvroKeyOutputFormat[Entry]])
    } else {
      throw new IllegalStateException("Save iteration result exception:"
        + MLContext.OUTPUT_ITERATION_MODE + "=" + mode)
    }
  }


  def l2Norm(rdd: RDD[(Int, Double)], wBroad: Broadcast[Array[Float]], l2C: Float): RDD[(Int, Double)] = {

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

  def getExecutorNum(sc: SparkContext) = {
    sc.getConf.getInt("spark.executor.instances", 1)
  }

  def getReduceSumNum(sc: MLContext): Int = {
    val re = sc.getConf.getInt(REDUCE_SUM_NUMBER, getExecutorNum(sc.sparkContext()) / 2)
    re
  }


  def main(args: Array[String]) {
    val it = generateLossAndGradientIterator(Array(3f, 3.4f, 4.2f), 10f)
    while (it.hasNext) {
      print(it.next())
    }
    //    println("ss")
    //    val a: Float = 3f
    //    println(a.toFloat)

    //    val conf = new SparkConf().setAppName("ScalaWordCount").setMaster("local[2]")
    //    val ctx = new SparkContext(conf)
    //    val as = new Array[Float](10)
    //    val loss=readLossAndGradientFromRDD(ctx.parallelize(List(-1->3d,1->3.3d,2->3.2d)),as)
    //    println(loss);

  }

}
