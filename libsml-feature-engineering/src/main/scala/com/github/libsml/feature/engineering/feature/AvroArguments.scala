package com.github.libsml.feature.engineering.feature

import com.github.libsml.feature.engineering.feature.AvroArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class AvroArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  //  var numReduce: Int = -1

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

      case ("--help" | "-h") :: tail =>
        printUsageAndExit(0)

      case Nil => {}

      case _ =>
        printUsageAndExit(1)
    }
  }

}

object AvroArguments {
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature process avro [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i INPUT,  --input INPUT                         Input path \n" +
        "  -o OUTPUT, --output OUTPUT                       Output path\n"
    )
    System.exit(exitCode)
  }
}


