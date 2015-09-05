package com.github.libsml.feature.engineering.feature

import scala.collection.mutable.ArrayBuffer
import com.github.libsml.feature.engineering.feature.MapArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class MapArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  var labelIndex: Int = -1
  var weightIndex: Int = -1
  var numReduce: Int = 1
  var numMap: Int = 1
  var indices: ArrayBuffer[Int] = new ArrayBuffer[Int]()
  var functions: ArrayBuffer[String] = new ArrayBuffer[String]()

  parse(args.toList)

  if (input == null || indices.length != functions.length) {
    printUsageAndExit(1)
  }

  def parse(args: List[String]): Unit = {
    args match {
      case ("--input" | "-i") :: value :: tail =>
        input = value
        parse(tail)

      case ("--output" | "-o") :: value :: tail =>
        output = value
        parse(tail)
      case ("-r") :: value :: tail =>
        numReduce = value.toInt
        parse(tail)
      case ("-l") :: value :: tail =>
        labelIndex = value.toInt
        parse(tail)
      case ("-m") :: value :: tail =>
        numMap = value.toInt
        parse(tail)
      case ("-wi") :: value :: tail =>
        weightIndex = value.toInt
        parse(tail)
      case ("-is") :: value :: tail =>
        for (s <- value.split(",")) {
          indices += s.toInt
        }
        parse(tail)

      case ("-fs") :: value :: tail =>
        for (s <- value.split(",")) {
          functions += s
        }
        parse(tail)
      case ("--help" | "-h") :: tail =>
        printUsageAndExit(0)

      case Nil => {}

      case _ =>
        printUsageAndExit(1)
    }
  }


}


object MapArguments {
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature process map [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i  INPUT,  --input INPUT                         Input path \n" +
        "  -o  OUTPUT, --output OUTPUT                       Output path\n" +
        "  -r  REDUCE NUMBER                                 Reduce number (default: 1)\n" +
        "  -m  MAP NUMBER                                    Map number (default: 1)\n" +
        "  -wi WEIGHT INDEX                                  Weight index (default: -1,meaning the weight is 1.0)\n" +
        "  -is INDEX1,INDEX1,...                             Indices of rows needed to map\n" +
        "  -fs  FUNCTION1,FUNCTION,...                       Map functions\n"
    )
    System.exit(exitCode)
  }
}