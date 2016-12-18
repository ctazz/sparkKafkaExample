package producer

import kafka.producer.Producer
import kafka.producer.KeyedMessage
import kafka.producer.ProducerConfig

//TODO  Needs to be in its own jar, so it can be a per-jvm singleton
//TODO  Use the actual KafkaProducer, in the Kafka client dependency, so we can do async processing
case class KafkaProducerWrapper(brokerList: String) {
  val producerProps = {
    val prop = new java.util.Properties
    prop.put("metadata.broker.list", brokerList)
    prop
  }
  val p = new Producer[Array[Byte], Array[Byte]](new ProducerConfig(producerProps))

  def send(topic: String, key: String, value: String) {
    p.send(new KeyedMessage(topic, key.toCharArray.map(_.toByte), value.toCharArray.map(_.toByte)))
  }
}
object KafkaProducerWrapper {
  var brokerList = ""
  lazy val instance = new KafkaProducerWrapper(brokerList)
}