import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import scala.collection.mutable
import scala.concurrent.Future
import java.io.FileWriter
import org.apache.spark.storage.StorageLevel
import org.apache.spark.mllib.clustering.{DistributedLDAModel, LDA}
import org.apache.spark.mllib.linalg.Vectors
import java.net._
import java.io._
import scala.io._
import java.nio.charset.CodingErrorAction
import scala.concurrent._
import ExecutionContext.Implicits.global


object Analysis {
    def main(args: Array[String]) {
	  val server = new ServerSocket(45409)
      val spark = SparkSession.builder.appName("Analysis").getOrCreate()
      val sc = SparkContext.getOrCreate()
      import spark.implicits._
      
      //Stopwords - used to improve topic discovery
      val stopwordsFile = spark.read.textFile("/stopwords").rdd
      /*
      val subjects = spark.read.textFile("/testFile").rdd.map(
        x => x.toLowerCase.split(" |,")
      )
      */
      
      val decoder = Codec.UTF8.decoder.onMalformedInput(CodingErrorAction.IGNORE)
      while (true) {
        val s = server.accept()
        val t = new Thread {
			override def run {
				val in = new BufferedSource(s.getInputStream())(decoder).getLines() //returns iterator that splits on \n
				val out = new PrintStream(s.getOutputStream())
				out.println("HTTP/1.1 200 OK\r\n") //respond to HTTP nonsense
				out.flush()
				var temp = ":"
				while(in.hasNext && (temp.contains(":") || temp.contains("POST") || temp == "")){
				  temp = in.next()
				}
				val lineNumber = temp.toInt //get number of lines in file (sent by webpage)
				
				//Turn lines into distributed rdd
				val lines = in.take(lineNumber).toList
				val subjects = sc.parallelize(lines).map(
				  x => x.trim.toLowerCase.split(" |,")
				)
				
				//filter and create pure word collection with indices
				val stopwords = sc.broadcast(stopwordsFile.collect.toSet)
				val words = subjects.flatMap(x => x).distinct().filter(x => !stopwords.value.contains(x))
				val w2i = words.zipWithIndex
				val i2w = w2i.map(_.swap)

				val word2ix = sc.broadcast(w2i.collectAsMap())
				val ix2word = i2w.collectAsMap()
			  
				//creates and caches the input vectors for lda
				val inputData = subjects.zipWithIndex.map { case (k,v) =>
					val counts = new mutable.HashMap[Int,Double]() 
					for(y <- k) {
					   if (word2ix.value.contains(y)){
						 val idx = word2ix.value.get(y).get.toInt
						 counts(idx) = counts.getOrElse(idx,0.0) + 1.0
					   }
					}
					(v, Vectors.sparse(word2ix.value.size, counts.toSeq))
				}.cache()

				val ldaModel = new LDA().setK(10).run(inputData)

				val descriptions = ldaModel.describeTopics(10)

				//organize and write model output
				var topic = ""
				descriptions.foreach{ tuple =>
					tuple._1.foreach{ idx =>
						topic = topic + ix2word.get(idx.toInt).get + " "
					}
					topic = topic + "\n"
				}
				out.println(topic)
				out.flush()
				s.close()
			}
		}
		t.start
		/*
        val fw = new FileWriter("/s/bach/g/under/kevincb/TopicOutput.txt", false)
        try {
            fw.write(topic)
        }
        finally fw.close() 
        */
      }
      spark.stop()
    }
}
