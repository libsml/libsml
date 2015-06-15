package com.github.libsml.function.imp.spark

import com.github.libsml.commons.util.AUC
import com.github.libsml.data.liblinear.DataPoint
import com.github.libsml.function.EvaluatorFunction
import com.github.libsml.function.imp.spark.SparkLogisticRegression._
import com.github.libsml.model.Statistics
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

/**
 * Created by huangfish on 2015/6/12.
 */
class SparkBinaryClassifyEvaluator(val threshold: Float, val RddPoint: RDD[DataPoint], val bias: Float) extends EvaluatorFunction {

  val sc = RddPoint.sparkContext

  override def evaluate(w: Array[Float], k: Int): Statistics = {

    val bias = this.bias
    val threshold = this.threshold
    val wBroadCast = sc.broadcast(w)
    val statisticsTmp = RddPoint.mapPartitions(ds => {
      new Iterator[(Int, Float)] {
        val weight = wBroadCast.value


        override def hasNext: Boolean = ds.hasNext

        override def next(): (Int, Float) = {
          val d = ds.next()
          (d.y.toInt, (1 / (1 + Math.exp(-xv(d, weight, bias)))).toFloat)
        }
      }

    }).groupByKey(1).mapPartitions(ds => {
      val posProbs = new ArrayBuffer[Float]()
      val negProbs = new ArrayBuffer[Float]()
      var tp = 0
      var fp = 0
      var tn = 0
      var fn = 0
      while (ds.hasNext) {
        val d = ds.next()
        if (d._1 == 1) {
          val it = d._2.iterator
          while (it.hasNext) {
            val p = it.next()
            if (p >= threshold) {
              tp += 1
            } else {
              fp += 1
            }
            posProbs += p
          }
        } else if (d._1 == -1) {
          val it = d._2.iterator
          while (it.hasNext) {
            val p = it.next()
            if (p >= threshold) {
              fn += 1
            } else {
              tn += 1
            }
            negProbs += p
          }
        } else {
          throw new IllegalStateException("Evaluat exception:label=" + d._1 + " is not 1 or -1")
        }
      }
      val precision: Float = (tp.toFloat) / (tp + fp)
      val recall: Float = (tp.toFloat) / (tp + fn)
      val f1Score: Float = 2 * precision * recall / (precision + recall)
      val accucary: Float = (tp.toFloat + tn) / (tp + fn + tn + fp)

      Seq((precision, recall, f1Score, AUC.auc(posProbs.toArray, negProbs.toArray), accucary)).iterator
      //      Seq(new Statistics(precision, recall, f1Score, AUC.auc(posProbs.toArray, negProbs.toArray), accucary)).iterator
    }).collect()(0)

    new Statistics(statisticsTmp._1, statisticsTmp._2, statisticsTmp._3, statisticsTmp._4, statisticsTmp._5)
  }

}
