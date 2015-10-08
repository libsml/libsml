package com.github.libsml

/**
 * Created by huangyu on 15/8/27.
 */

import org.apache.spark.LibsmlFunSuite

object LoggingSuite extends LibsmlFunSuite{

  test("Logging info") {
    logInfo("info")
    val a = 1
    assert(a == 1)
  }

}
