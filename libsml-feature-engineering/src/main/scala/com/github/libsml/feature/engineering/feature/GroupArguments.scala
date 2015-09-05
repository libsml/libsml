package com.github.libsml.feature.engineering.feature

import com.github.libsml.feature.engineering.feature.GroupArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class GroupArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  var numReduce: Int = 100
  var numMap: Int = 100
  var index: Int = -1

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

      case ("-ix") :: value :: tail =>
        index = value.toInt
        parse(tail)
      case ("-r") :: value :: tail =>
        numReduce = value.toInt
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

object GroupArguments {
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: BayesianSmooth [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i  INPUT,  --input INPUT                         Input path \n" +
        "  -o  OUTPUT, --output OUTPUT                       Output path\n" +
        "  -r  REDUCE NUMBER                                 Reduce number (default: 1)\n" +
        "  -m  MAP NUMBER                                    Map number (default: 1)\n" +
        "  -ix INDEX                                         Key index\n"
    )
    System.exit(exitCode)
  }
}
