package com.github.libsml.model

import org.apache.spark.rdd.RDD
import com.github.libsml.math.linalg.Vector

/**
 * Created by yellowhuang on 2016/5/9.
 */
object Utils {
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
