package com.github.libsml.commons.util

/**
 * Created by huangyu on 15/8/23.
 */
class MapWrapper(val map: Map[String, String]) {

  def getInt(key: String, default: Int) = {
    if (map.contains(key)) {
      map.get(key).get.toInt
    } else {
      default
    }
  }

  def getBoolean(key: String, default: Boolean) = {
    if (map.contains(key)) {
      map.get(key).get.toBoolean
    } else {
      default
    }
  }

  def getShort(key: String, default: Short) = {
    if (map.contains(key)) {
      map.get(key).get.toShort
    } else {
      default
    }
  }

  def getFloat(key: String, default: Float) = {
    if (map.contains(key)) {
      map.get(key).get.toFloat
    } else {
      default
    }
  }

  def getDouble(key: String, default: Double) = {
    if (map.contains(key)) {
      map.get(key).get.toDouble
    } else {
      default
    }
  }

  def get(key: String, default: String) = {
    map.getOrElse(key, default)
  }

}

object MapWrapper {
  implicit def toMapWrapper(map: Map[String, String]): MapWrapper = {
    new MapWrapper(map)
  }
}
