name := "kafka-in-spark-example"

version := "0.0.1"

scalaVersion := "2.10.4"

// additional libraries
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.6.2" % "provided"
  , "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.2"
  , "org.apache.spark" % "spark-streaming_2.10" % "1.6.2" % "provided"
  , "org.apache.kafka" % "kafka_2.10" % "0.8.2.1" % "provided"
)

//assemblyMergeStragegy?  Beats me.  But I was getting this error:
///Users/charlestassoni/.ivy2/cache/org.apache.spark/spark-streaming-kafka_2.10/jars/spark-streaming-kafka_2.10-1.6.2.jar:org/apache/spark/unused/UnusedStubClass.class /Users/charlestassoni/.ivy2/cache/org.spark-project.spark/unused/jars/unused-1.0.0.jar:org/apache/spark/unused/UnusedStubClass.class
//And found a solution here: http://stackoverflow.com/questions/37152902/sbt-assemblymergestrategy-not-working
assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class")  => MergeStrategy.first
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}