Start with example from Spark In Action
Use Spark 1.6.2 instead of Spark 2.x

My kafka directory:
~/scala/kafka/kafka_2.10-0.8.2.1
My "Spark in Action" ch06 (the chapter that covers Kafka) directory
~/scala/spark/sparkInAction/first-edition/ch06

Start zookeeper and two kafka servers.  From kafka directory, run
bin/zookeeper-server-start.sh config/zookeeper.properties &
bin/kafka-server-start.sh config/server1.properties &
bin/kafka-server-start.sh config/server2.properties &

If you haven't created the topics yet:

bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 2 --topic orders
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 2 --topic metrics
And to verify that your topics have been created:
bin/kafka-topics.sh --zookeeper localhost:2181 --describe

From "Spark in Action" ch06 directory 
 ./streamOrders2.sh 127.0.0.1:9093,127.0.0.1:9094
 
 Create this project
 sbt clean assembly
 
 Deploy to local Spark 1.6.2 instance
 ~/scala/spark/learningSpark/spark-1.6.2-bin-hadoop2.6/bin/spark-submit --class example.OrderSummary --master local[4]  ./target/scala-2.10/kafka-in-spark-example-assembly-0.0.1.jar

Here's how to view records written to a topic:
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic <theTopic>