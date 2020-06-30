package com.hcl.dataops

import org.apache.spark.sql.{ SaveMode, SparkSession }
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.lang.Thread
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

object ShuffleTuner {

  case class Employee(ssn: String, creditCardNumber: String, creditCardProvider: String,
                      creditCardExpire: String, name: String,
                      city: String, phoneNumber: String, country: String,
                      dateOfPurchase: String, amountSpend: String)
  def writeappidtofile(appid:String){

	val path = "/dataops/sparkappid"
	var conf = new Configuration();
	conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
	conf.set("fs.defaultFS", "hdfs://nameservice1:8020/")
	val fs = FileSystem.get(conf)
	val os = fs.create(new Path(path))
	os.write(appid.getBytes)
	fs.close()
  }

  def main(args: Array[String]): Unit = {

   if((args.length < 4) && (args(3).equalsIgnoreCase("count") || args(3).equalsIgnoreCase("write"))){
        System.out.println("Stopping the application. Please provide the necessary input parameters needed.");
        System.exit(0);
    }
    
    var calendar = Calendar.getInstance(); 
    println("ShuffleTuner - Application Started at : ",calendar.getTime())
    
    val inputPath1 = args(0)
    val inputPath2 = args(1)
    val outputPath = args(2)
    val executionType = args(3)
    
    print("ShuffleTuner - Reading the input folders : ",inputPath1, "\t", inputPath2 )
    val spark: SparkSession = SparkSession.builder()
      .appName("ShuffleTuner")
      .getOrCreate()
    
    import spark.implicits._
    
    val appid = spark.sparkContext.applicationId
    
    writeappidtofile(appid)

    val schema = new StructType()
      .add("ssn", StringType, true)
      .add("creditCardNumber", StringType, true)
      .add("creditCardProvider", StringType, true)
      .add("creditCardExpire", StringType, true)
      .add("name", StringType, true)
      .add("city", StringType, true)
      .add("phoneNumber", StringType, true)
      .add("country", StringType, true)
      .add("dateOfPurchase", StringType, true)
      .add("amountSpend", DoubleType, true)

      val df1 = spark.read.format("csv").option("header", "true").schema(schema).load(inputPath1)
      val df2 = spark.read.format("csv").option("header", "true").schema(schema).load(inputPath2)
      //df1.cache()
      println("ShuffleTuner - Number of partitions available in the input dataframe 1 : ", df1.rdd.partitions.length)
      println("ShuffleTuner - Number of partitions available in the input dataframe 2 : ", df2.rdd.partitions.length)

     // val outputDF = df.groupBy("ssn", "creditCardNumber", "name").agg(sum("amountSpend").as("TotalAmountSpent"))
      //joining the datafrrames on ssn column
      val outputDF = df1.as("d1").join(df2.as("d2"), $"d1.creditCardNumber" === $"d2.creditCardNumber").select($"d1.creditCardNumber", $"d1.name")
      println("ShuffleTuner - Number of partitions available in the output dataframe : ", outputDF.rdd.partitions.length)
      
      if(executionType.equalsIgnoreCase("count")){
        print("ShuffleTuner - Number of output rows : ", outputDF.count())
      }
      else if(executionType.equalsIgnoreCase("write")){
        println("ShuffleTuner - Saving the output to the path : ", outputPath)
        //saving the output dataframe to hdfs location
        //outputDF.write.mode(SaveMode.Overwrite).csv(outputPath)
	//outputDF.write.mode(SaveMode.Overwrite).format("csv").save(outputPath)
	outputDF.write.mode(SaveMode.Overwrite).parquet(outputPath)
      }else{
        println("ShuffleTuner - No action found")
      }
      //Thread.sleep(9000000)    
      calendar = Calendar.getInstance(); 
      println("ShuffleTuner - Application Completed at : ", calendar.getTime())
      
  }
}
