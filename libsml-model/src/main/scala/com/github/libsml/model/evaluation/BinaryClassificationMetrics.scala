package com.github.libsml.model.evaluation

import com.github.libsml.model.evaluation.binary._
import org.apache.spark.rdd.{UnionRDD, RDD}

import scala.collection.mutable.ArrayBuffer


/**
 * Created by huangyu on 15/8/22.
 */
trait BinaryClassificationMetrics {


  def thresholds(): Array[Double]

  def roc(): Array[(Double, Double)]

  def areaUnderROC(): Double

  def pr(): Array[(Double, Double)]

  def areaUnderPR(): Double

  def fMeasureByThreshold(beta: Double): Array[(Double, Double)]

  def fMeasureByThreshold(): Array[(Double, Double)]

  def precisionByThreshold(): Array[(Double, Double)]

  def recallByThreshold(): Array[(Double, Double)]
}

/**
 * Evaluator for binary classification.
 *
 * @param scoreAndLabels an Array of (score, label) pairs.
 * @param numBins if greater than 0, then the curves (ROC curve, PR curve) computed internally
 *                will be down-sampled to this many "bins". If 0, no down-sampling will occur.
 *                This is useful because the curve contains a point for each distinct score
 *                in the input, and this could be as large as the input itself -- millions of
 *                points or more, when thousands may be entirely sufficient to summarize
 *                the curve. After down-sampling, the curves will instead be made of approximately
 *                `numBins` points instead. Points are made from bins of equal numbers of
 *                consecutive points. The size of each bin is
 *                `floor(scoreAndLabels.count() / numBins)`, which means the resulting number
 *                of bins may not exactly equal numBins. The last bin in each partition may
 *                be smaller as a result, meaning there may be an extra sample at
 *                partition boundaries.
 */

class SingleBinaryClassificationMetrics(val scoreAndLabels: Array[(Double, Double)],
                                        val numBins: Int) extends BinaryClassificationMetrics {

  def this(scoreAndLabels: Array[(Double, Double)]) = this(scoreAndLabels, 0)

  override def areaUnderROC(): Double =
    AreaUnderCurve.of(roc())

  override def roc(): Array[(Double, Double)] = createCurve(FalsePositiveRate, Recall, Some((0.0, 0.0)), Some((1.0, 1.0)))

  override def precisionByThreshold(): Array[(Double, Double)] = createCurve(Precision)

  override def recallByThreshold(): Array[(Double, Double)] = createCurve(Recall)

  override def pr(): Array[(Double, Double)] = createCurve(Recall, Precision, Some((0.0, 1.0)))

  override def fMeasureByThreshold(beta: Double): Array[(Double, Double)] = createCurve(FMeasure(beta))

  override def fMeasureByThreshold(): Array[(Double, Double)] = fMeasureByThreshold(1.0)

  override def thresholds(): Array[Double] = cumulativeCounts.map(_._1)

  override def areaUnderPR(): Double = AreaUnderCurve.of(pr())

  private lazy val (cumulativeCounts: Array[(Double, BinaryLabelCounter)],
  confusions: Array[(Double, BinaryConfusionMatrix)]) = {
    val counts = new ArrayBuffer[(Double, BinaryLabelCounter)]()
    var kk: Double = -1
    var value: BinaryLabelCounter = new BinaryLabelCounter()
    scoreAndLabels.sortBy(_._1).reverse.foreach(kv => {
      if (kv._1 != kk) {
        kk = kv._1
        value = new BinaryLabelCounter()
        counts += ((kk, value))
      }
      value.+=(kv._2)
    })

    val binnedCounts =
    // Only down-sample if bins is > 0
      if (numBins == 0) {
        // Use original directly
        counts
      } else {
        val countsSize = counts.size
        // Group the iterator into chunks of about countsSize / numBins points,
        // so that the resulting number of bins is about numBins
        val grouping = countsSize / numBins
        if (grouping < 2) {
          // numBins was more than half of the size; no real point in down-sampling to bins
          println(s"Curve is too small ($countsSize) for $numBins bins to be useful")
          counts
        } else {
          counts.grouped(grouping).map(pairs => {
            // The score of the combined point will be just the first one's score
            val firstScore = pairs.head._1
            // The point will contain all counts in this chunk
            val agg = new BinaryLabelCounter()
            pairs.foreach(pair => agg += pair._2)
            (firstScore, agg)
          })
        }
      }

    val totalCount = new BinaryLabelCounter()
    val cumulativeCounts = binnedCounts.map(pairs => {
      totalCount += pairs._2
      (pairs._1, totalCount.clone)
    }
    ).toArray

    val confusions = cumulativeCounts.map { case (score, cumCount) =>
      (score, BinaryConfusionMatrixImpl(cumCount, totalCount).asInstanceOf[BinaryConfusionMatrix])
    }
    (cumulativeCounts, confusions)
  }

  /** Creates a curve of (threshold, metric). */
  private def createCurve(y: BinaryClassificationMetricComputer): Array[(Double, Double)] = {
    confusions.map { case (s, c) =>
      (s, y(c))
    }
  }

  //  /** Creates a curve of (metricX, metricY). */
  //  private def createCurve(
  //                           x: BinaryClassificationMetricComputer,
  //                           y: BinaryClassificationMetricComputer): Array[(Double, Double)] = {
  //    confusions.map { case (_, c) =>
  //      (x(c), y(c))
  //    }
  //  }

  /** Creates a curve of (metricX, metricY). */
  private def createCurve(
                           x: BinaryClassificationMetricComputer,
                           y: BinaryClassificationMetricComputer,
                           startPoint: Option[(Double, Double)] = None,
                           endPoint: Option[(Double, Double)] = None): Array[(Double, Double)] = {

    var length = confusions.length
    var index = 0
    if (startPoint.isDefined) {
      length += 1
      index = 1
    }
    if (endPoint.isDefined) {
      length += 1
    }

    val curve: Array[(Double, Double)] = new Array[(Double, Double)](length)
    confusions.foreach(pairs => {
      curve(index) = (x(pairs._2), y(pairs._2))
      index += 1
    })
    startPoint.foreach(curve(0) = _)
    endPoint.foreach(curve(length - 1) = _)
    curve
  }
}

class SparkBinaryClassificationMetrics(val scoreAndLabels: RDD[(Double, Double)],
                                       val numBins: Int) {
  require(numBins >= 0, "numBins must be nonnegative")

  /**
   * Defaults `numBins` to 0.
   */
  def this(scoreAndLabels: RDD[(Double, Double)]) = this(scoreAndLabels, 0)

  /** Unpersist intermediate RDDs used in the computation. */
  def unpersist() {
    cumulativeCounts.unpersist()
  }

  /** Returns thresholds in descending order. */
  def thresholds(): RDD[Double] = cumulativeCounts.map(_._1)

  /**
   * Returns the receiver operating characteristic (ROC) curve,
   * which is an RDD of (false positive rate, true positive rate)
   * with (0.0, 0.0) prepended and (1.0, 1.0) appended to it.
   * @see http://en.wikipedia.org/wiki/Receiver_operating_characteristic
   */
  def roc(): RDD[(Double, Double)] = {
    val rocCurve = createCurve(FalsePositiveRate, Recall)
    val sc = confusions.context
    val first = sc.makeRDD(Seq((0.0, 0.0)), 1)
    val last = sc.makeRDD(Seq((1.0, 1.0)), 1)
    new UnionRDD[(Double, Double)](sc, Seq(first, rocCurve, last))
  }

  /**
   * Computes the area under the receiver operating characteristic (ROC) curve.
   */
  def areaUnderROC(): Double = AreaUnderCurve.of(roc())

  /**
   * Returns the precision-recall curve, which is an RDD of (recall, precision),
   * NOT (precision, recall), with (0.0, 1.0) prepended to it.
   * @see http://en.wikipedia.org/wiki/Precision_and_recall
   */
  def pr(): RDD[(Double, Double)] = {
    val prCurve = createCurve(Recall, Precision)
    val sc = confusions.context
    val first = sc.makeRDD(Seq((0.0, 1.0)), 1)
    first.union(prCurve)
  }

  /**
   * Computes the area under the precision-recall curve.
   */
  def areaUnderPR(): Double = AreaUnderCurve.of(pr())

  /**
   * Returns the (threshold, F-Measure) curve.
   * @param beta the beta factor in F-Measure computation.
   * @return an RDD of (threshold, F-Measure) pairs.
   * @see http://en.wikipedia.org/wiki/F1_score
   */
  def fMeasureByThreshold(beta: Double): RDD[(Double, Double)] = createCurve(FMeasure(beta))

  /** Returns the (threshold, F-Measure) curve with beta = 1.0. */
  def fMeasureByThreshold(): RDD[(Double, Double)] = fMeasureByThreshold(1.0)

  /** Returns the (threshold, precision) curve. */
  def precisionByThreshold(): RDD[(Double, Double)] = createCurve(Precision)

  /** Returns the (threshold, recall) curve. */
  def recallByThreshold(): RDD[(Double, Double)] = createCurve(Recall)

  private lazy val (
    cumulativeCounts: RDD[(Double, BinaryLabelCounter)],
    confusions: RDD[(Double, BinaryConfusionMatrix)]) = {
    // Create a bin for each distinct score value, count positives and negatives within each bin,
    // and then sort by score values in descending order.
    val counts = scoreAndLabels.combineByKey(
      createCombiner = (label: Double) => new BinaryLabelCounter(0L, 0L) += label,
      mergeValue = (c: BinaryLabelCounter, label: Double) => c += label,
      mergeCombiners = (c1: BinaryLabelCounter, c2: BinaryLabelCounter) => c1 += c2
    ).sortByKey(ascending = false)

    val binnedCounts =
    // Only down-sample if bins is > 0
      if (numBins == 0) {
        // Use original directly
        counts
      } else {
        val countsSize = counts.count()
        // Group the iterator into chunks of about countsSize / numBins points,
        // so that the resulting number of bins is about numBins
        var grouping = countsSize / numBins
        if (grouping < 2) {
          // numBins was more than half of the size; no real point in down-sampling to bins
          println(s"Curve is too small ($countsSize) for $numBins bins to be useful")
          counts
        } else {
          if (grouping >= Int.MaxValue) {
            println(
              s"Curve too large ($countsSize) for $numBins bins; capping at ${Int.MaxValue}")
            grouping = Int.MaxValue
          }
          counts.mapPartitions(_.grouped(grouping.toInt).map { pairs =>
            // The score of the combined point will be just the first one's score
            val firstScore = pairs.head._1
            // The point will contain all counts in this chunk
            val agg = new BinaryLabelCounter()
            pairs.foreach(pair => agg += pair._2)
            (firstScore, agg)
          })
        }
      }

    val agg = binnedCounts.values.mapPartitions { iter =>
      val agg = new BinaryLabelCounter()
      iter.foreach(agg += _)
      Iterator(agg)
    }.collect()
    val partitionwiseCumulativeCounts =
      agg.scanLeft(new BinaryLabelCounter())(
        (agg: BinaryLabelCounter, c: BinaryLabelCounter) => agg.clone() += c)
    val totalCount = partitionwiseCumulativeCounts.last
    println(s"Total counts: $totalCount")
    val cumulativeCounts = binnedCounts.mapPartitionsWithIndex(
      (index: Int, iter: Iterator[(Double, BinaryLabelCounter)]) => {
        val cumCount = partitionwiseCumulativeCounts(index)
        iter.map { case (score, c) =>
          cumCount += c
          (score, cumCount.clone())
        }
      }, preservesPartitioning = true)
    cumulativeCounts.persist()
    val confusions = cumulativeCounts.map { case (score, cumCount) =>
      (score, BinaryConfusionMatrixImpl(cumCount, totalCount).asInstanceOf[BinaryConfusionMatrix])
    }
    (cumulativeCounts, confusions)
  }

  /** Creates a curve of (threshold, metric). */
  private def createCurve(y: BinaryClassificationMetricComputer): RDD[(Double, Double)] = {
    confusions.map { case (s, c) =>
      (s, y(c))
    }
  }

  /** Creates a curve of (metricX, metricY). */
  private def createCurve(
                           x: BinaryClassificationMetricComputer,
                           y: BinaryClassificationMetricComputer): RDD[(Double, Double)] = {
    confusions.map { case (_, c) =>
      (x(c), y(c))
    }
  }

}

object BinaryClassificationMetrics {

  def apply(scoreAndLabels: Array[(Double, Double)], numBins: Int): BinaryClassificationMetrics = {
    new SingleBinaryClassificationMetrics(scoreAndLabels, numBins)
  }

  def apply(scoreAndLabels: Array[(Double, Double)]): BinaryClassificationMetrics = {
    new SingleBinaryClassificationMetrics(scoreAndLabels)
  }

  def apply(scoreAndLabels: RDD[(Double, Double)], numBins: Int) = {
    new SparkBinaryClassificationMetrics(scoreAndLabels, numBins)
  }

  def apply(scoreAndLabels: RDD[(Double, Double)]) = {
    new SparkBinaryClassificationMetrics(scoreAndLabels)
  }


  //  def apply(scoreAndLabels: RDD[(Double, Double)], numBins: Int): BinaryClassificationMetrics = {
  //    new SparkBinaryClassificationMetrics(scoreAndLabels, numBins)
  //  }

}