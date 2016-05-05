package com.github.libsml.test

import java.util.StringTokenizer

import scala.collection.mutable.ArrayBuffer

/**
 * Created by huangyu on 15/8/31.
 */
object Submit {

  def main(args: Array[String]): Unit = {
    //    args.foreach(println _)
    val value = "a,,,"
    val st: StringTokenizer = new StringTokenizer(value, ",")
    while (st.hasMoreTokens) {
      println("s:"+st.nextToken())
//      functions += (if (s.trim == "") None else Some(s))
    }
  }

}
