package com.github.libsml.feature.engineer.smooth

import com.github.libsml.feature.engineering.smooth.{FixedPointDirichletMultinomial, FixedPointBayesianOptimizer, BayesianSmoothFunction}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.{OptimizerUtils, Optimizer}
import com.github.libsml.optimization.liblinear.{LiblinearParameter, Tron}
import com.github.libsml.math.function.Function
import com.github.libsml.optimization.newton.NewtonMethod

import scala.io.Source
import scala.reflect.ClassTag

/**
 * Created by huangyu on 15/8/23.
 */
object BayesianSmoothTest {


  def singleFixPointTest() = {
    val n: Int = 6482156
    val clicks: Vector = Vector(6482156 * 2)
    val unClicks: Vector = Vector(6482156 * 2)
    var index: Int = 0
    Source.fromFile("data/2963", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      val c = ss(0).toDouble
      clicks(index) = ss(0).toDouble
      unClicks(index) = ss(1).toDouble - clicks(index)
      index += 1
    })

    val fun: BayesianSmoothFunction = new BayesianSmoothFunction(clicks, unClicks)
    val optimizer: Optimizer[Vector] = new FixedPointBayesianOptimizer()
    optimizer.setFunction(fun)
    for (r <- optimizer) {
      //      println("f:" + r.f)
      println(r.w)
      //      println(r.msg)
    }

  }

  def singleFixPointDirichletMul() = {
    val n: Int = 6482156
    val clicks: Vector = Vector(6482156 * 2)
    val unClicks: Vector = Vector(6482156 * 2)
    var index: Int = 0
    Source.fromFile("data/2963", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      clicks(index) = ss(0).toDouble
      unClicks(index) = ss(1).toDouble - clicks(index)
      index += 1
    })

    val fun = new DirichletMultinomial(Array(clicks, unClicks))
    val optimizer: Optimizer[Vector] = new FixedPointDirichletMultinomial(fun)
    //    optimizer.setFunction(fun)
    for (r <- optimizer) {
      println(r.w)
    }

  }

  def singleBayesianSmooth() = {
    //    val n: Int = 1843036
    val n: Int = 6482156
    val clicks: Vector = Vector(6482156 * 2)
    val unClicks: Vector = Vector(6482156 * 2)
    var index: Int = 0
    Source.fromFile("data/2963", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      val c = ss(0).toDouble
      clicks(index) = ss(0).toDouble
      unClicks(index) = ss(1).toDouble - clicks(index)
      index += 1
    })
    val data = Array(clicks, unClicks)

    val fun: DirichletMultinomial = new DirichletMultinomial(data)
            val optimizer: Optimizer[Vector] = new NewtonMethod(fun, Vector(Array(0.5677, 8.839)))
//    val optimizer: Optimizer[Vector] = new NewtonMethod(fun, fun.prior())
    //    val optimizer: Optimizer[Vector] = new Tron(Vector(Array(1.0, 1.0)), new LiblinearParameter(), fun)
    for (r <- optimizer) {
      println("f:" + r.f.get)
      println("w:" + r.w)
      println("g:" + r.g.get)
    }
  }

  def classRefect(): Unit = {
    val fun: DirichletMultinomial = new DirichletMultinomial(Array(Vector(Array(1.1, 1.1)), Vector(Array(2.3, 2.3))))
    OptimizerUtils.instantiateOptimizer("com.github.libsml.feature.engineering.smooth.FixedPointDirichletMultinomial", fun)
  }

  def classTest[W](v: W)(implicit ctg: ClassTag[W]) = {
    println(v.getClass)
    println(ctg.runtimeClass)
    //    val cls = Class.forName("com.github.libsml.optimization.liblinear.Tron")
    //    val cons=cls.getConstructor(ctg.runtimeClass,classOf[Map[String,String]])
    //    println(cons)

    val cons = Class.forName("com.github.libsml.feature.engineer.smooth.t").getConstructor(ctg.runtimeClass)
    val a: java.lang.Double = 2.2
    //    cons.newInstance(2.2)
    println(cons)

  }

  def main(args: Array[String]) {

    //    val a: Double = 3.3
    //    val b: java.lang.Double = a
    //
    //    println((2.2).getClass)
    //    println(Vector.getClass)
    //    classTest(2.2)
    //    classRefect()
    singleBayesianSmooth()
    //    singleFixPointTest()
//        singleFixPointDirichletMul()
    //        var v1: Vector = Vector(Array(1.0, 1.0))
    //        var v2: Vector = Vector()
    //        v2=v1
    //        println(v1)
    //        println(v2)

  }
}
