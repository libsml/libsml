package com.github.libsml.feature.engineering

import com.github.libsml.model.data.DataUtils
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by huangyu on 15/9/22.
 */
object DataMain {

  def main(args: Array[String]): Unit = {
    if (args.length != 5) {
      println("data <srcPath> <desPath> <numPartitions> <weight1> <weight2>")
    }
    val sc = new SparkContext(new SparkConf().setAppName("Data process"))
    DataUtils.randomSplitAvro(sc, args(0), Array(args(1) + "/train", args(1) + "/test"),
      args(2).toInt, Array(args(3).toDouble, args(4).toDouble))
    sc.stop()
  }
}
