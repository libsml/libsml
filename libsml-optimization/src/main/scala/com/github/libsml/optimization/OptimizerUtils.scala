package com.github.libsml.optimization

import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function

/**
 * Created by huangyu on 15/8/23.
 */
object OptimizerUtils {


  val shortFullMap: Map[String, String] = Map("Tron" -> "com.github.libsml.optimization.liblinear.Tron",
    "FixedPoint" -> "com.github.libsml.feature.engineering.smooth.FixedPointDirichletMultinomial")

  private[this] def fullClassName(className: String): String = {
    shortFullMap.getOrElse(className, className)
  }

  // Create an instance of the class with the given name
  def instantiateOptimizer(className: String): Optimizer[Vector] = {
    instantiateOptimizer(fullClassName(className), Map[String, String]())
  }

  // Create an instance of the class with the given name
  def instantiateOptimizer(_className: String, map: Map[String, String]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Map[String, String]])
        .newInstance(map)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        cls.getConstructor().newInstance().asInstanceOf[Optimizer[Vector]]
    }
  }


  def instantiateOptimizer(_className: String, weight: Vector): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)

    try {
      cls.getConstructor(classOf[Vector])
        .newInstance(weight)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        cls.getConstructor().newInstance().asInstanceOf[Optimizer[Vector]].prior(weight)
    }
  }

  def instantiateOptimizer(_className: String, function: Function[Vector]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Function[Vector]])
        .newInstance(function)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        cls.getConstructor().newInstance().asInstanceOf[Optimizer[Vector]].setFunction(function)
    }
  }

  def instantiateOptimizer(_className: String, weight: Vector, map: Map[String, String]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Vector], classOf[Map[String, String]])
        .newInstance(weight, map)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          instantiateOptimizer(className, weight)
        } catch {
          case _: NoSuchMethodException =>
            cls.getConstructor(classOf[Map[String, String]])
              .newInstance(map)
              .asInstanceOf[Optimizer[Vector]].prior(weight)
        }
    }
  }

  def instantiateOptimizer(_className: String, weight: Vector, function: Function[Vector]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Vector], classOf[Function[Vector]])
        .newInstance(weight, function)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          instantiateOptimizer(className, weight).setFunction(function)
        } catch {
          case _: NoSuchMethodException =>
            cls.getConstructor(classOf[Function[Vector]])
              .newInstance(function)
              .asInstanceOf[Optimizer[Vector]]
        }
    }
  }

  def instantiateOptimizer(_className: String, map: Map[String, String], function: Function[Vector]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Map[String, String]], classOf[Function[Vector]])
        .newInstance(map, function)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          instantiateOptimizer(className, map).setFunction(function)
        } catch {
          case _: NoSuchMethodException =>
            cls.getConstructor(classOf[Function[Vector]])
              .newInstance(function)
              .asInstanceOf[Optimizer[Vector]]
        }
    }
  }

  def instantiateOptimizer(_className: String, weight: Vector, map: Map[String, String], function: Function[Vector]): Optimizer[Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Vector], classOf[Map[String, String]], classOf[Function[Vector]])
        .newInstance(weight, map, function)
        .asInstanceOf[Optimizer[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          instantiateOptimizer(className, weight, map)
        } catch {
          case _: NoSuchMethodException =>
            try {
              instantiateOptimizer(className, weight, function)
            } catch {
              case _: NoSuchMethodException =>
                instantiateOptimizer(className, map, function)
            }
        }
    }
  }


}
