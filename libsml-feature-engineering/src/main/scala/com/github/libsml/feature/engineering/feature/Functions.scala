package com.github.libsml.feature.engineering.feature

import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/8/31.
 */
object Functions {

  def default(length: Int)(s: String): FeatureGroup = {
    val ss = s.trim.split("#|\\|")
    val group = new FeatureGroup(Vector(), length)
    var i = 0
    while (i < ss.length) {
      val kv = ss(i).trim.split(":")
      group.features(kv(0).toInt) = if (kv.length >= 2) kv(1).toDouble else 1.0
      i += 1
    }
    group
  }

  def map(length: Int,map: Map[String, Int] = Map.empty)(s: String): FeatureGroup = {
    val ss = s.trim.split("#|\\|")
    val group = new FeatureGroup(Vector(), length)
    var i = 0
    while (i < ss.length) {
      val kv = ss(i).trim.split(":")
      group.features(map.getOrElse(kv(0), kv(0).toInt)) = if (kv.length >= 2) kv(1).toDouble else 1.0
      i += 1
    }
    group
  }

}
