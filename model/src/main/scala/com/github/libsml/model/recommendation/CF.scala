package com.github.libsml.model.recommendation

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import com.github.libsml.math.linalg.Vector
import collection.mutable

/**
 * Created by yellowhuang on 2016/5/6.
 */
object CF {

  def train(data: RDD[(Int, Int, Double)],
            similarity: Similarity,
            top: Int = -1,
            threshold: Double = Double.MinValue,
            _inverseData: Option[RDD[(Int, Int, Double)]] = None): CFModel = {

    def toVector(d: Iterable[(Int, Double)]): Vector = {
      val vector = Vector()
      d.foreach(kv => vector(kv._1) = kv._2)
      vector
    }

    val inverseData = _inverseData.getOrElse(data.map(d => (d._2, d._1, d._3)))
    val _norms = data.map(d => (d._1, (d._2, d._3))).groupByKey().
      map(v => (v._1, toVector(v._2))).map(v => (v._1, similarity.norm(v._2))).collectAsMap()
    val bnorm = data.sparkContext.broadcast(_norms)
    val matrix = inverseData.map(d => (d._1, (d._2, d._3))).groupByKey().flatMap(v => {
      val co = v._2.toArray.sortBy(_._1)
      for (i <- 0 until co.length; j <- i + 1 until co.length)
        yield ((co(i)._1, co(j)._1), similarity.join(co(i)._2, co(j)._2))

    }).filter(d => d._1._1 != d._1._2).reduceByKey(similarity.aggregate).mapPartitions(ds => {
      val norms = bnorm.value
      ds.map(d => (d._1, similarity.similarity(d._2, norms(d._1._1), norms(d._1._2))))
    })
      .flatMap(v => Seq[((Int,Int),Double)](v,((v._1._2,v._1._1),v._2)))

    val model = matrix.flatMap(d => Seq[((Int),(Int,Double))]((d._1._1, (d._1._2, d._2)),(d._1._2,(d._1._1,d._2)))).groupByKey().
      map(idNbs => {
        val id = idNbs._1
        var neighbors = idNbs._2.toArray.sortBy(-_._2)
        if (top > 0) neighbors = neighbors.take(top)
        if (threshold != Double.MinValue) neighbors.filter(_._2 >= threshold)
        new Neighbors(id, neighbors)
      })
    model.cache()
    bnorm.unpersist()
    new CFModel(model)
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println("Usage:<dataPath> <top> <resultPath>")
      System.exit(1)
    }
    val dataPath = args(0)
    val top = args(1).toInt
    val sc = new SparkContext(new SparkConf().setAppName("CF Test"))
    val _data = sc.textFile(dataPath).filter(_.trim != "").map(line => {
      val ss = line.split("\t")
      (ss(0).trim, ss(1).trim, 1.0)
    })
    val index2Uid = _data.map(_._1).distinct().collect()
    val index2Oid = _data.map(_._2).distinct().collect()
    val uid2Index = new mutable.HashMap[String, Int]()
    val oid2Index = new mutable.HashMap[String, Int]()
    for (i <- 0 until index2Uid.length) uid2Index(index2Uid(i)) = i
    for (i <- 0 until index2Oid.length) oid2Index(index2Oid(i)) = i

    val bUid2Index = sc.broadcast(uid2Index)
    val bOid2Index = sc.broadcast(oid2Index)

    val data = _data.map(d => {
      val u2i = bUid2Index.value
      val o2i = bOid2Index.value
      (u2i(d._1), o2i(d._2), d._3)
    })

    val model = train(data, new JaccardSimilarity(), top)
    model.save(args(2))

    sc.stop()

  }

}

case class CFPoint(val xid: Int, val yid: Int, val score: Double)

class Neighbors(val id: Int, val neighbors: Array[(Int, Double)])

class CFModel(val model: RDD[Neighbors]) {

  def save(path: String): Unit = {
    model.map(nbs => nbs.id + "\t" + nbs.neighbors.map(nb => nb._1 + ":" + nb._2).mkString(",")).
      saveAsTextFile(path)
  }

  def load(sc: SparkContext, path: String): CFModel = {
    val model = sc.textFile(path).map(line => {
      val ss = line.split("\t")
      val id = ss(0).toInt
      val nbs = ss(1).split(",").map(kv => {
        val ss = kv.split(":")
        (ss(0).toInt, ss(1).toDouble)
      })
      new Neighbors(id, nbs)
    })
    model.cache()
    new CFModel(model)
  }


}