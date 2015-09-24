package com.github.libsml.feature.engineering.smooth

import scala.collection.mutable

/**
 * Created by huangyu on 15/8/24.
 */
class BayesianSmoothArguments(args: Array[String]) {

  var input: String = null
  var output: String = null
  var optimizerClass: String = "com.github.libsml.optimization.lbfgs.LBFGS"
  var keyIndex: Int = 0
  var clickIndex: Int = 2
  var impressionIndex: Int = 3
  var reduceNum: Int = 100

  //argument:(--conf key=value)*
  var confMap: mutable.Map[String, String] = new mutable.HashMap[String, String]()

  parse(args.toList)

  if (input == null || output == null) {
    printUsageAndExit(1)
  }

  def parse(args: List[String]): Unit = args match {
    case ("--input" | "-i") :: value :: tail =>
      input = value
      parse(tail)

    case ("--output" | "-o") :: value :: tail =>
      output = value
      parse(tail)

    case ("-op") :: value :: tail =>
      optimizerClass = value
      parse(tail)

    case "-k" :: value :: tail =>
      keyIndex = value.toInt
      parse(tail)

    case ("-c") :: value :: tail =>
      clickIndex = value.toInt
      parse(tail)

    case ("-im") :: value :: tail =>
      impressionIndex = value.toInt
      parse(tail)

    case ("-r") :: value :: tail =>
      reduceNum = value.toInt
      parse(tail)

    case ("--help") :: tail =>
      printUsageAndExit(0)

    case ("--conf") :: value :: tail =>
      val ss = value.split("=")
      confMap(ss(0)) = ss(1)
      parse(tail)

    case Nil => {}

    case _ =>
      printUsageAndExit(1)
  }

  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: BayesianSmooth [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i INPUT,  --input INPUT     Input path \n" +
        "  -o OUTPUT, --output OUTPUT   Output path\n" +
        "  -op OPTIMIZER CLASS          Optimizer class(default:fixedPoint)\n" +
        "  -k KEY INDEX                 Key index (default: 0)\n" +
        "  -c CLICK INDEX               Click index (default: 1)\n" +
        "  -im IMPRESSION INDEX         Impression index (default: 2)\n" +
        "  --conf key=value             configuration\n" +
        "  -r REDUCE NUMBER             Reduce number (default: 100)\n"
    )
    System.exit(exitCode)
  }
}
