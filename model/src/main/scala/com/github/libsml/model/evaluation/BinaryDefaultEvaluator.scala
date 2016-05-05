package com.github.libsml.model.evaluation

import java.io.PrintStream

import com.github.libsml.commons.util.Logging
import com.github.libsml.model.Model
import com.github.libsml.model.data.WeightedLabeledVector
import com.github.libsml.math.linalg.Vector
import org.apache.spark.rdd.RDD

/**
 * Created by huangyu on 15/9/6.
 */
class BinaryDefaultEvaluator(val testData: Either[RDD[WeightedLabeledVector], Array[WeightedLabeledVector]],
                             val methods: Array[String],
                             val numBins: Int = 0,
                             val testOutput: Option[PrintStream] = None) extends Evaluator[Vector, Vector] with Logging {

//  private[this] val out: PrintStream = {
//    testOutput match {
//      case Some(path) =>
//        new PrintStream(path)
//      case None =>
//        System.out
//    }
//  }

  override def evaluator(model: Model[Vector, Vector]): Unit = {

    val metrics = testData match {
      case Left(td) =>
        val bc = td.sparkContext.broadcast(model)
        val scoreAndLabel = td.map(e => {
          (bc.value.score(e.features, 1), e.label)
        })
        BinaryClassificationMetrics(scoreAndLabel, numBins)

      case Right(td) =>
        val scoreAndLabel = td.map(e => {
          (model.score(e.features, 1), e.label)
        })
        BinaryClassificationMetrics(scoreAndLabel, numBins)
    }

    methods.foreach(m => {
      try {
        val testResult = metrics.getClass.getMethod(m).invoke(metrics)
        testOutput.foreach(_.println(m+"="+testResult))
//        out.println(m + "=" + testResult)
      } catch {
        case e: NoSuchMethodException =>
          logError("Evaluator exception:method:%s not found in class:%s".format(m, metrics.getClass.toString), e)
      }
    })
  }
}
