package com.github.libsml.feature.engineer.feature

import com.github.libsml.feature.engineering.feature.FeatureProcess
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by huangyu on 15/8/31.
 */
object FeatureProcessTest {

  def joinTest(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf().setAppName("Join Test").setMaster("local"))
    val rdd = FeatureProcess.joinMain(sc, args)
    rdd.foreach(println _)
  }

  def main(args: Array[String]) {
    joinTest(args)
  }

}
