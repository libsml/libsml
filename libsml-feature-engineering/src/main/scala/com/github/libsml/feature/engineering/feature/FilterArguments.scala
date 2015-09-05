package com.github.libsml.feature.engineering.feature

import scala.collection.mutable.ArrayBuffer
import com.github.libsml.feature.engineering.feature.FilterArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class FilterArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  var numMap: Int = 1
  var indices: ArrayBuffer[Int] = new ArrayBuffer[Int]()
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
      case ("-is") :: value :: tail =>
        for (s <- value.split(",")) {
          indices += s.toInt
        }
        parse(tail)
      case ("-m") :: value :: tail =>
        numMap = value.toInt
        parse(tail)
      case ("--help"|"-h") :: tail =>
        printUsageAndExit(0)
      case Nil => {}
      case _ =>
        printUsageAndExit(1)
    }
  }


}

object FilterArguments{
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature process filter [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i  INPUT,  --input INPUT                         Input path \n" +
        "  -o  OUTPUT, --output OUTPUT                       Output path\n" +
        "  -m  MAP NUMBER                                    Map number (default: 1)\n" +
        "  -is INDEX1,INDEX1,...                             Indices of rows needed to filter\n"
    )
    System.exit(exitCode)
  }
}
