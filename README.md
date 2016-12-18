Sorry, no time to mess with markdown format

Start with example from Spark In Action
Use Spark 1.6.2 instead of Spark 2.x. And create a project rather than run from Spark console.

Setup kafka
http://kafka.apache.org/downloads.html
choose version 0.8.2.1
Copy to directory you want, and unpack
tar -xvfz kafka_2.10-0.8.2.1.tgz

I like to run two kafka servers, and I like to keep kafka and zookeeper data under the kafka directory. I also like to retain kafka logs for less than the 7 day default.
I've put my zookeeper.properties and server1.properties, server2.properties into src/main/resources, in case you want to do some or all of those things

Troubleshooting Kafka:
If you run into weird problems with your Kafka installation, which has happened to me in the past:
What's worked for me is to delete all of your Kafka data.
If you're using my setup, just delete your <KafkaRoot>/theData directory
And you'll also need to go to /var/folders (on a Mac, anyway), and delete all the directories under there. They are temporary operating system folders.
You'll need to sudo, as in: sudo rm -rf <whatever> 


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

Clone https://github.com/spark-in-action/first-edition
Go to the ch06 directory
unpack orders.tar.gz
tar -xvfz orders.tar.gz
Look at streamOrders.sh.  You'll need to modify it so that it points to your <kafkaRoot>\bin\kafka-console-producer.sh file
(A streamOrders file that works for me is in src/main/resources)
You want to execute that file, so you'll need to run
chmod +x streamOrders.sh
Now, from that ch06 directory (and assuming that you configured your kafka properties as I did, so that you're running Kafka locally on 9093 and 9094:
./streamOrders.sh 127.0.0.1:9093,127.0.0.1:9094

From "Spark in Action" ch06 directory 
 
 
 Package our scala project
 sbt clean assembly
 
 Deploy to local Spark 1.6.2 instance (the first part of this one depends on the Spark path on my computer)
 ~/scala/spark/learningSpark/spark-1.6.2-bin-hadoop2.6/bin/spark-submit --class example.OrderSummary --master local[4]  ./target/scala-2.10/kafka-in-spark-example-assembly-0.0.1.jar

Here's how to view records written to a topic:
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic <theTopic>

You can see this project's output like this (be patient, it takes a few seconds for this command to start reading)
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic metrics
and if you're running into problems, check 
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic orders