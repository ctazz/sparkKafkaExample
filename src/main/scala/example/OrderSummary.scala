package example

import org.apache.spark._
import kafka.serializer.StringDecoder
import kafka.producer.Producer
import kafka.producer.KeyedMessage
import kafka.producer.ProducerConfig
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import org.apache.spark.streaming._

import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.InputDStream



import producer.KafkaProducerWrapper

object OrderSummary {


  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("ordersToSummaryMetrics")

    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(5))

    val kafkaReceiverParams = Map[String, String](
        "metadata.broker.list" -> "127.0.0.1:9093,127.0.0.1:9094")
    val kafkaStream: InputDStream[(String, String)] = KafkaUtils.
  createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaReceiverParams, Set("orders"))


    import java.sql.Timestamp
    case class Order(time: java.sql.Timestamp, orderId: Long, clientId: Long, symbol: String, amount: Int, price: Double, buy: Boolean)

    //TODO  JodaTime  SimpleDateFormat is not thread safe!
    import java.text.SimpleDateFormat

    val orders: DStream[Order] = kafkaStream.flatMap { case (kafkaKey, line) => {
      println(s"kafkaKey is $kafkaKey  line is $line")
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
      val s = line.split(",")
      try {
        assert(s(6) == "B" || s(6) == "S")
        List(Order(new Timestamp(dateFormat.parse(s(0)).getTime()), s(1).toLong, s(2).toLong, s(3), s(4).toInt, s(5).toDouble, s(6) == "B"))
      }
      catch {
        case e: Throwable => println("Wrong line format (" + e + "): " + line)
          List()
      }
    }
    }


    val numPerType = orders.map(o => (o.buy, 1L)).reduceByKey((c1, c2) => c1 + c2)
    val amountPerClient = orders.map(o => (o.clientId, o.amount * o.price))
    val amountState = amountPerClient.updateStateByKey((vals, totalOpt: Option[Double]) => {
      totalOpt match {
        case Some(total) => Some(vals.sum + total)
        case None => Some(vals.sum)
      }
    })
    val top5clients = amountState.transform(_.sortBy(_._2, false).map(_._1).
      zipWithIndex.filter(x => x._2 < 5))

    val buySellList = numPerType.map(t =>
      if (t._1) ("BUYS", List(t._2.toString))
      else ("SELLS", List(t._2.toString)))
    val top5clList = top5clients.repartition(1).
      map(x => x._1.toString).
      glom().
      map(arr => ("TOP5CLIENTS", arr.toList))

    val stocksPerWindow = orders.map(x => (x.symbol, x.amount)).reduceByKeyAndWindow((a1: Int, a2: Int) => a1 + a2, Minutes(60))
    val topStocks = stocksPerWindow.transform(_.sortBy(_._2, false).map(_._1).
      zipWithIndex.filter(x => x._2 < 5)).repartition(1).
      map(x => x._1.toString).glom().
      map(arr => ("TOP5STOCKS", arr.toList))

    val finalStream = buySellList.union(top5clList).union(topStocks)

    //import org.sia.KafkaProducerWrapper
    finalStream.foreachRDD((rdd) => {
      rdd.foreachPartition((iter) => {
        KafkaProducerWrapper.brokerList = "127.0.0.1:9093,127.0.0.1:9094"
        val producer = KafkaProducerWrapper.instance
        iter.foreach({ case (metric, list) => producer.send("metrics", metric, metric + ", " + list.toString) })
      })
    })

    sc.setCheckpointDir("target/checkpoint/")
    ssc.start()

    //Why does the process stop when I don't sleep here? I don't always need to sleep in a Scala App.
    Thread.sleep(600000)

  }


}
