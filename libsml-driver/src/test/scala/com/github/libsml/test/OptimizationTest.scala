package com.github.libsml.test

import com.github.libsml.driver.optimization.{Optimization, OptimizationConf}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.model.data.DataUtils

/**
 * Created by huangyu on 15/9/8.
 */
object OptimizationTest {

  def confTest(): Unit = {
    val conf = new OptimizationConf("./src/conf/optimization")
    println(conf.functionClass)
    println(conf.featureNum)
    conf.metricsMethods.foreach(println _)
  }

  def getDataTest(): Unit = {
    val conf = new OptimizationConf("./src/conf/optimization")
    val data = Optimization.getData(None, conf.input, conf.mapNum, conf)
    println(data.getClass)
  }

  def getFunctionTest(): Unit = {
    val conf = new OptimizationConf("./src/conf/optimization")
    val data = Optimization.getData(None, conf.input, conf.mapNum, conf)
    val fun = Optimization.getFunction(data, conf)
    println(fun.getClass)
  }

  def priorTest(): Unit = {
    val conf = new OptimizationConf("./src/conf/optimization")
    val prior: Vector = VectorUtils.newVector(conf.vectorType, conf.featureNum + (if (conf.bias > 0) 1 else 0))
    println(prior.getClass)
    println(prior)
    conf.prior.foreach(DataUtils.readAvro2Vector(_, prior))
    println(prior)
  }

  def test(): Unit = {
    val conf = new OptimizationConf("./src/conf/optimization")
    Optimization.optimize(conf)
  }


  def main(args: Array[String]) {

    //    val conf = new OptimizationConf("./src/conf/optimization")
    //    getDataTest()
    //    confTest()
    //    getFunctionTest()
//    priorTest()
    test()


  }
}
