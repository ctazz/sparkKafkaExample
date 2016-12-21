package tools

import java.util.Properties

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer._

import scala.collection.JavaConverters._
import scala.io.Source

object WriteJsonArrayStoredInFileToKafka extends App {


  object Support {

    def readText(fileName: String): String = {
      val source = Source.fromFile(fileName)
      try source.getLines().mkString("\n") finally source.close
    }


    def createObjectMapper(): ObjectMapper = {
      val m = new ObjectMapper()
      m.registerModule(DefaultScalaModule)
      m.registerModule(new JodaModule())
      m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      m
    }

    def toSimpleMap(config: Config): Map[String, String] = config.entrySet.asScala.foldLeft(Map.empty[String, String]) { (acc, configEntry) =>
      configEntry.getValue.unwrapped match {
        case value: String => acc + (configEntry.getKey -> value)
        case _ => acc
      }
    }

    //Yes, this is slower than directly creating Properties from Config, but it's not going to be called very often, is it?
    def toProperties(config: Config): Properties = toSimpleMap(config).foldLeft(new Properties) { (props, pair) => props.put(pair._1, pair._2); props }

    def kafkaProducer[K,V](properties: Properties):  KafkaProducer[K,V] = new KafkaProducer(properties)
  }
  
  import Support._

  //You'll need to supply arguments for the testDadtaFile, the topicName and the config's bootstrap.servers
  //The bootstrap servers need to be in the form "host1:port,host2:port"
  val testDataFile = args(0)

  val topicName = args(1)

  val config = ConfigFactory.parseString(s"""
  bootstrap.servers = "${args(2)}"
  client.id = testclient
  key.serializer=org.apache.kafka.common.serialization.StringSerializer
  value.serializer=org.apache.kafka.common.serialization.StringSerializer
  retry.backoff.ms = 10
""")

  val pathToKafkaConfig = "shm.kafka.inbound"

  val producer = kafkaProducer[String, String](toProperties(config))

  val mapper = createObjectMapper

  val data: Seq[String] = mapper.readValue(readText(testDataFile), classOf[Seq[Map[Any, Any]]]).map(mapper.writeValueAsString(_))

  //Currently we write the same records over and over again.
  while(true) {

    data.foreach { record =>
      producer.send(new ProducerRecord(topicName, record), new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (exception != null)
            println(s"error writing data $record")
          else
            println(s"Kafka record written to topic/partition/offset ${metadata.topic}/${metadata.partition}/${metadata.offset}, data is $record")
        }
      })


    }
    Thread.sleep(1000)
  }



}
