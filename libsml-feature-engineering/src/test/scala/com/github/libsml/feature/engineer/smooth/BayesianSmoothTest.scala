package com.github.libsml.feature.engineer.smooth

import com.github.libsml.feature.engineering.smooth.{BayesianSmoothFunction, FixedPointBayesianOptimizer, FixedPointDirichletMultinomial, MedianDirichletMultinomial}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.lbfgs.LBFGS
import com.github.libsml.optimization.{Optimizer, OptimizerUtils}

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
    Source.fromFile("data/bayesian_test", "utf-8").getLines().foreach(line => {
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

  def medianSmoothTest() = {
    //    val n: Int = 3265
    //    val n: Int = 3076
    //    val n: Int = 2714
    val n: Int = 10050645
    val clicks: Vector = Vector(n)
    val unClicks: Vector = Vector(n)
    var index: Int = 0
    Source.fromFile("dataset/2881", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      if (ss(3).toDouble > 0) {
        clicks(index) = ss(2).toDouble
        unClicks(index) = ss(3).toDouble - clicks(index)
        index += 1
      }
    })

    val fun = new DirichletMultinomial(Array(clicks, unClicks))
    //    val fun = new DirichletMultinomial(Array(Vector(Array(0.0,0.0,0.0)), Vector(Array(0.0,0.0,0.0))))

    //    val start = System.currentTimeMillis()
    val optimizer: Optimizer[Vector] = new MedianDirichletMultinomial(fun)
    //            val optimizer: Optimizer[Vector] = new FixedPointDirichletMultinomial(fun)
    //    val optimizer = new LBFGS(Vector(Array(1.0, 1.0)), fun)
    //    val optimizer = OptimizerUtils.instantiateOptimizer("lbfgs", fun.prior(), Map[String, String]("linearSearch.maxLinesearch" -> "14"
    //      , "lbfgs.maxIterations" -> "30"), fun)
    //    optimizer.prior(fun.prior())
    //    optimizer.setFunction(fun)

    //    optimizer.setFunction(fun)
    for (r <- optimizer) {
      println(r)
      //      println(r.w)
      //      println("g:" + r.g)
      //      println("f:" + r.f.get)
      //      println(s"d:${optimizer.d}")
      //      println(s"${r}")
    }

    //    println("****")
    //    optimizer.prior(fun.prior())
    //    optimizer.setFunction(fun)
    //    for (r <- optimizer) {
    //      println(r.w)
    //      println("g:" + r.g)
    //      println("f:" + r.f.get)
    //      //      println(s"${r}")
    //    }
    //    println(s"init:${System.currentTimeMillis() - start}")
    //    println(optimizer.optimize()._1)
    //    println(s"pass:${System.currentTimeMillis() - start}")

  }

  def singleBayesianSmooth() = {
    //    val n: Int = 1843036
    val n: Int = 10050645
    val clicks: Vector = Vector(n)
    val unClicks: Vector = Vector(n)
    var index: Int = 0
    Source.fromFile("dataset/2881", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      //      val c = ss(0).toDouble
      clicks(index) = ss(2).toDouble
      if (ss(3).toDouble == 0) {
        println(line)
      }
      unClicks(index) = ss(3).toDouble - clicks(index)
      index += 1
    })
    val data = Array(clicks, unClicks)

    val fun: DirichletMultinomial = new DirichletMultinomial(data)
    val optimizer: Optimizer[Vector] = new LBFGS(Vector(Array(1.0, 1.0)), fun)
    //        val optimizer: Optimizer[Vector] = new NewtonMethod(fun, Vector(Array(0.5677, 8.839)))
    //        val optimizer: Optimizer[Vector] = new NewtonMethod(fun, Vector(Array(1.0, 1.0)))
    //    val optimizer: Optimizer[Vector] = new NewtonMethod(fun, fun.prior())
    //    val optimizer: Optimizer[Vector] = new Tron(Vector(Array(1.0, 1.0)), new LiblinearParameter(), fun)
    var isStop = false
    for (r <- optimizer if !isStop) {
      val alphaBeta = r.w
      isStop = r.f.map(_ < 10E-10).getOrElse(isStop)
      isStop = if (alphaBeta(0) < 10E-10) true else isStop
      isStop = if (alphaBeta(1) < 10E-10) true else isStop
      println(r)
      //      println("f:" + r.f.get)
      //      println("w:" + r.w)
      //      println("g:" + r.g.get)
      //      println(s"msg:${r.msg.get}")
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
    //    singleBayesianSmooth()
    medianSmoothTest()
    //        singleFixPointDirichletMul()
    //        var v1: Vector = Vector(Array(1.0, 1.0))
    //        var v2: Vector = Vector()
    //        v2=v1
    //        println(v1)
    //        println(v2)

  }
}
