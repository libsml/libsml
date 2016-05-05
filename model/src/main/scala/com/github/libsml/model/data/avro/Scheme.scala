package com.github.libsml.model.data.avro

import org.apache.avro.Schema

/**
 * Created by huangyu on 15/9/1.
 */
object Scheme {
  val ENTRY: Schema = new Schema.Parser().parse("{\"name\":\"Entry\",\"type\":\"record\",\"fields\":[{\"name\":\"index\",\"type\":\"int\"},{\"name\":\"value\",\"type\":\"double\"}]}")
  val WEIGHTED_LABEL_SCHEME: Schema = new org.apache.avro.Schema.Parser().parse("{\"name\":\"CRData\",\"type\":\"record\",\"fields\":[{\"name\":\"y\",\"type\":\"double\"},{\"name\":\"Features\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Entry\",\"fields\":[{\"name\":\"index\",\"type\":\"int\"},{\"name\":\"value\",\"type\":\"double\"}]}}},{\"name\":\"weight\",\"type\":\"double\",\"default\":1.0}]}")

  def main(args: Array[String]) {
    println(WEIGHTED_LABEL_SCHEME)
    println(ENTRY)
  }
}
