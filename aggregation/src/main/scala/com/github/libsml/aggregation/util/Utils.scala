package com.github.libsml.aggregation.util

import com.github.libsml.model.data.DataUtils

/**
 * Created by huangyu on 15/9/24.
 */
object Utils {

  def avroModel2TextModel(avroPath: String, textPath: String): Unit = {
    DataUtils.avroVector2TextVector(avroPath, textPath)
  }

  def main(args: Array[String]): Unit = {

    def printHelp(): Unit = {
      println("Usage:avro2text [parameter*]")
    }

    if (args.length < 1) {
      printHelp()
      System.exit(1)
    }

    args(0) match {
      case "avro2text" =>
        if (args.length < 3) {
          println("Usage:avro2text avroPath textPath")
          System.exit(1)
        }
        avroModel2TextModel(args(1), args(2))
      case _ =>
    }
  }

}
