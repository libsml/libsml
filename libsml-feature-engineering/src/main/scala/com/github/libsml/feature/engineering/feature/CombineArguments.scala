package com.github.libsml.feature.engineering.feature

import com.github.libsml.feature.engineering.feature.CombineArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class CombineArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  var isPutBack: Boolean = false
  var index1: Int = -1
  var index2: Int = 1
  var numMap: Int = 1
  parse(args.toList)

  if (input == null) {
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
      case ("-i1") :: value :: tail =>
        index1 = value.toInt
        parse(tail)
      case ("-i2") :: value :: tail =>
        index2 = value.toInt
        parse(tail)
      case ("-p") :: tail =>
        isPutBack = true
        parse(tail)
      case ("-m") :: value :: tail =>
        numMap = value.toInt
        parse(tail)
      case ("--help" | "-h") :: tail =>
        printUsageAndExit(0)
      case Nil => {}
      case _ =>
        printUsageAndExit(1)
    }
  }


}

object CombineArguments {
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature process combine [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i  INPUT,  --input INPUT                         Input path \n" +
        "  -o  OUTPUT, --output OUTPUT                       Output path\n" +
        "  -m  MAP NUMBER                                    Map number (default: 1)\n" +
        "  -p  IS PUT BACK                                   Is put rows of index1 and index2 back(default:false)\n" +
        "  -i1 INDEX1                                        Index1 of row needed to combine\n" +
        "  -i2 INDEX2                                        Index2 of row needed to combine\n"
    )
    System.exit(exitCode)
  }
}
