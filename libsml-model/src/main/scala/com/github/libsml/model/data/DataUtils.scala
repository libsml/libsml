package com.github.libsml.model.data


import java.io.{PrintWriter, File}
import java.util
import java.util.StringTokenizer

import com.github.libsml.commons.util.AvroUtils
import com.github.libsml.model.data.avro.Scheme
import org.apache.avro.file.{DataFileWriter, DataFileReader}
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericDatumReader, GenericRecord}
import org.apache.avro.io.DatumReader
import org.apache.avro.mapred.{AvroOutputFormat, AvroJob, AvroInputFormat, AvroWrapper}
import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/8/17.
 */
object DataUtils {


  def randomSplitAvro(sc: SparkContext, srcPath: String, desPaths: Array[String], numPartitions: Int, weights: Array[Double]): Unit = {

    require(desPaths.length == weights.length, "Random split exception.")
    val all = sc.hadoopFile[AvroWrapper[GenericRecord], NullWritable, AvroInputFormat[GenericRecord]](srcPath, numPartitions).randomSplit(weights)

    val jobConf = new JobConf(all(0).sparkContext.hadoopConfiguration)
    AvroJob.setOutputSchema(jobConf, Scheme.WEIGHTED_LABEL_SCHEME)

    var i = 0
    while (i < all.length) {
      all(i).saveAsHadoopFile(desPaths(i), classOf[AvroWrapper[GenericRecord]], classOf[NullWritable],
        classOf[AvroOutputFormat[GenericRecord]], jobConf)
      i += 1
    }


  }

  def loadAvroData2RDD(sc: SparkContext, bias: Double, featureNum: Int, path: String, numPartitions: Int): RDD[WeightedLabeledVector] = {

    sc.hadoopFile[AvroWrapper[GenericRecord], NullWritable, AvroInputFormat[GenericRecord]](path, numPartitions)
      .map(kv => parseAvroRecord(kv._1.datum(), bias, featureNum))
  }

  def loadSVMData2RDD(sc: SparkContext, bias: Double, featureNum: Int, path: String, numPartitions: Int): RDD[WeightedLabeledVector] = {
    sc.textFile(path, numPartitions).map(_.trim).filter(!_.isEmpty).map(parseSVMLine(_, bias, featureNum))
  }

  def loadSVMData(bias: Double, featureNum: Int, path: String): Array[WeightedLabeledVector] = {
    loadSVMData(bias, featureNum, parseStringToFiles(path): _*)
  }

  def loadAvroData(bias: Double, featureNum: Int, path: String): Array[WeightedLabeledVector] = {
    loadAvroData(bias, featureNum, parseStringToFiles(path): _*)
  }

  private[this] def parseStringToFiles(filePaths: String): Array[File] = {
    val inputFiles = new ArrayBuffer[File]()

    filePaths.split(",").foreach(p => {
      val f = new File(p)
      if (f.isDirectory) {
        inputFiles ++= (FileUtils.listFiles(f, null, true).toArray(new Array[File](0)))
      }
      else {
        inputFiles += f
      }
    })
    inputFiles.toArray
  }

  private[this] def loadSVMData(bias: Double, featureNum: Int, files: File*): Array[WeightedLabeledVector] = {
    val data = new ArrayBuffer[WeightedLabeledVector]()
    files.foreach(Source.fromFile(_, "utf-8").getLines().filter(_.trim != "").foreach(data += parseSVMLine(_, bias, featureNum)))
    data.toArray
  }


  private[this] def loadAvroData(bias: Double, featureNum: Int, files: File*): Array[WeightedLabeledVector] = {
    val data = new ArrayBuffer[WeightedLabeledVector]()
    files.foreach(file => {
      val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord]()
      val dataFileReader = new DataFileReader[GenericRecord](file, reader)
      while (dataFileReader.hasNext) {
        data += parseAvroRecord(dataFileReader.next(), bias, featureNum)
      }
    })
    data.toArray
  }


  private[this] def parseAvroRecord(record: GenericRecord, bias: Double, featureNum: Int): WeightedLabeledVector = {
    val fs: util.List[_] = record.get("Features").asInstanceOf[util.List[_]]
    val index = new Array[Int](if (bias > 0) fs.size() + 1 else fs.size)
    val value = new Array[Double](if (bias > 0) fs.size() + 1 else fs.size)
    var i = 0
    while (i < fs.size) {
      val record = fs.get(i).asInstanceOf[GenericRecord]
      index(i) = AvroUtils.getIntAvro(record, "index", false)
      value(i) = AvroUtils.getDoubleAvro(record, "value", false)
      i += 1
    }
    if (bias > 0) {
      index(fs.size) = featureNum
      value(fs.size) = bias
    }
    new WeightedLabeledVector(AvroUtils.getDoubleAvro(record, "y", false), Vector(index, value),
      AvroUtils.getDoubleAvro(record, "weight", 1.0)
    )
  }

  private[this] def parseSVMLine(line: String, bias: Double, featureNum: Int): WeightedLabeledVector = {

    val st: StringTokenizer = new StringTokenizer(line, " \t\n\r\f:")
    var token: String = null
    try {
      token = st.nextToken
    }
    catch {
      case e: NoSuchElementException => {
        throw new IllegalStateException("Data exception:libsvm format empty line ")
      }
    }
    val m: Int = st.countTokens / 2
    val y = token.toDouble
    val index = new Array[Int](if (bias > 0) m + 1 else m)
    val value = new Array[Double](if (bias > 0) m + 1 else m)

    var i = 0
    while (i < m) {
      val tmp = st.nextToken().toInt
      require(tmp <= featureNum, "Data exception!")

      if (i > 0) {
        require(tmp > index(i - 1), "Data exception!")
      }
      index(i) = tmp
      value(i) = st.nextToken().toDouble
      i += 1
    }
    if (bias > 0) {
      index(m) = featureNum
      value(m) = bias
    }
    new WeightedLabeledVector(y, Vector(index, value))
  }

  def avroVector2TextVector(avroPath: String, textPath: String) = {
    val file = new File(avroPath)
    val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord]()
    val dataFileReader = new DataFileReader[GenericRecord](file, reader)
    val textOut = new PrintWriter(textPath)
    while (dataFileReader.hasNext) {
      val record = dataFileReader.next()
      val index = AvroUtils.getIntAvro(record, "index", false)
      val value = AvroUtils.getDoubleAvro(record, "value", false)
      textOut.println("%d:%f".format(index, value))
    }
    dataFileReader.close()
    textOut.close()

  }

  def readAvro2Vector(path: String, vector: Vector): Vector = {
    val file = new File(path)
    val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord]()
    val dataFileReader = new DataFileReader[GenericRecord](file, reader)
    while (dataFileReader.hasNext) {
      val record = dataFileReader.next()
      val index = AvroUtils.getIntAvro(record, "index", false)
      val value = AvroUtils.getDoubleAvro(record, "value", false)
      if (index >= 0) {
        vector(index) = value
      }
    }
    dataFileReader.close()
    vector
  }

  def readAvro2VectorAddition(path: String, vector: Vector): Double = {
    var addition: Double = 0.0
    val file = new File(path)
    val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord]()
    val dataFileReader = new DataFileReader[GenericRecord](file, reader)
    while (dataFileReader.hasNext) {
      val record = dataFileReader.next()
      val index = AvroUtils.getIntAvro(record, "index", false)
      val value = AvroUtils.getDoubleAvro(record, "value", false)
      if (index >= 0) {
        vector(index) = value
      } else if (index == -1) {
        addition = value
      }
    }
    addition
  }

  def writeVector2Avro(path: String, vector: Vector): Unit = {
    val file = new File(path)
    val writer = new GenericDatumWriter[GenericRecord](Scheme.ENTRY)
    val dataFileWriter = new DataFileWriter[GenericRecord](writer)
    dataFileWriter.create(Scheme.ENTRY, file)
    vector.foreachNoZero((i, v) => {
      val entry = new GenericData.Record(Scheme.ENTRY)
      entry.put("index", i)
      entry.put("value", v)
      dataFileWriter.append(entry)
    })
    dataFileWriter.close()
  }

  def writeVectorAddition2Avro(path: String, vector: Vector, addition: Double): Unit = {
    val file = new File(path)
    val writer = new GenericDatumWriter[GenericRecord](Scheme.ENTRY)
    val dataFileWriter = new DataFileWriter[GenericRecord](writer)
    dataFileWriter.create(Scheme.ENTRY, file)
    val entry = new GenericData.Record(Scheme.ENTRY)
    entry.put("index", -1)
    entry.put("value", addition)
    dataFileWriter.append(entry)
    vector.foreachNoZero((i, v) => {
      val entry = new GenericData.Record(Scheme.ENTRY)
      entry.put("index", i)
      entry.put("value", v)
      dataFileWriter.append(entry)
    })
    dataFileWriter.close()
  }


}
