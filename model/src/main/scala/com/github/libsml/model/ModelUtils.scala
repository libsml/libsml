package com.github.libsml.model

import com.github.libsml.math.linalg.Vector
import com.github.libsml.commons.util.Utils

/**
 * Created by huangyu on 15/9/7.
 */
object ModelUtils {

  val shortFullMap: Map[String, String] = Map(
    "lr" -> "com.github.libsml.model.classification.LogisticRegressionModel")

  private[this] def fullClassName(className: String): String = {
    shortFullMap.getOrElse(className, className)
  }

  // Create an instance of the class with the given name
  def instantiateModel(_className: String, map: Map[String, String]): Model[Vector, Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Map[String, String]])
        .newInstance(map)
        .asInstanceOf[Model[Vector, Vector]]
    } catch {
      case _: NoSuchMethodException =>
        cls.getConstructor().newInstance().asInstanceOf[Model[Vector, Vector]]
    }
  }

  // Create an instance of the class with the given name
  def instantiateModel(className: String): Model[Vector, Vector] = {
    instantiateModel(fullClassName(className), Map[String, String]())
  }


  def instantiateModel(_className: String, weight: Vector): Model[Vector, Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)

    try {
      cls.getConstructor(classOf[Vector])
        .newInstance(weight)
        .asInstanceOf[Model[Vector, Vector]]
    } catch {
      case _: NoSuchMethodException =>
        cls.getConstructor().newInstance().asInstanceOf[Model[Vector, Vector]].update(weight)
    }
  }

  def instantiateModel(_className: String, weight: Vector, map: Map[String, String]): Model[Vector, Vector] = {
    val className = fullClassName(_className)
    val cls = Class.forName(className)
    try {
      cls.getConstructor(classOf[Vector], classOf[Map[String, String]])
        .newInstance(weight, map)
        .asInstanceOf[Model[Vector, Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          instantiateModel(className, weight)
        } catch {
          case _: NoSuchMethodException =>
            cls.getConstructor(classOf[Map[String, String]])
              .newInstance(map)
              .asInstanceOf[Model[Vector, Vector]].update(weight)
        }
    }
  }



}
