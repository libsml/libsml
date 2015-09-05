package com.github.libsml.feature.engineering.feature

import scala.collection.mutable.ArrayBuffer
import com.github.libsml.feature.engineering.feature.JoinArguments._

/**
 * Created by huangyu on 15/8/24.
 */
class JoinArguments(args: Array[String]) {

  var mainPath: String = null
  var joinMessage: ArrayBuffer[(String, Int, Int)] = new ArrayBuffer[(String, Int, Int)]()
  var featurePath: String = null
  var output: String = null
  var numReduce: Int = 1
  var numMap: Int = 1

  parse(args.toList)

  if (mainPath == null || joinMessage.size == 0) {
    printUsageAndExit(1)
  }

  def parse(args: List[String]): Unit = {
    args match {
      case ("--input" | "-i") :: value :: tail =>
        mainPath = value
        parse(tail)

      case ("--output" | "-o") :: value :: tail =>
        output = value
        parse(tail)

      case ("-jf") :: value :: tail =>
        val ss = value.split(",")
        if (ss.length != 3) {
          printUsageAndExit(1)
        }
        joinMessage += ((ss(0), ss(1).toInt, ss(2).toInt))
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

object JoinArguments {
  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature process join [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i INPUT,  --input INPUT                         Input path \n" +
        "  -o OUTPUT, --output OUTPUT                       Output path\n" +
        "  -r REDUCE NUMBER                                 Reduce number (default: 100)\n" +
        "  -m MAP NUMBER                                    Map number (default: 100)\n" +
        "  -jf FEATURE PATH,FEATURE KEY,MAIN KEY            Joined feature\n"
    )
    System.exit(exitCode)
  }
}
