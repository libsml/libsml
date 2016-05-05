package com.github.libsml.model.data

/**
 * Created by huangyu on 15/8/18.
 */
object DataUtilsTest {

  def loadSVMDataTest(): Unit = {

    val data = DataUtils.loadSVMData(1, 124, "data/a9a.txt")
    data.foreach(println _)
  }

  def main(args: Array[String]) {

    loadSVMDataTest()
  }

}
