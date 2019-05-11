package actors

import java.io.File

import akka.actor.{ Actor, Props }

import models.Response

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest

object Task1 {

  def props(s3Client: AmazonS3, mainPath: String, bucketName: String) = Props(new Task1Actor(s3Client, mainPath, bucketName))

  case class Download(path: String)

  case class Upload(path: String)
}

class Task1Actor(s3Client: AmazonS3, mainPath: String, bucketName: String) extends Actor {
  import Task1._

  override def preStart() = println("Task1Actor actor created")

  override def receive: Receive = {
    case Download(path) =>

      println(s"Download request with path: $path")
      if (s3Client.doesObjectExist(bucketName, path)) {
        downloadFromS3(path)
        sender() ! Response("OK", 200)
      } else {
        sender() ! Response(s"File $path not Found", 404)
      }
    case Upload(path) =>
      println(s"Upload request with path: $path")
      if (!fileExistsLocally(path)) {
        sender() ! Response("File not found locally", 404)
      }
      if (uploadToS3(path)) {
        sender() ! Response("OK", 200)
      } else {
        sender() ! Response("Error", 404)
      }
  }

  def fileExistsLocally(path: String): Boolean = {
    val file = new File(mainPath + path)
    return file.exists()
  }

  def uploadToS3(path: String): Boolean = {
    val file = new File(mainPath + path)
    s3Client.putObject(bucketName, path, file)
    println(s"uploaded to s3 with path: $path")
    return true;
  }

  def fileIsUploadedToS3(uploadPath: String): Boolean = {
    return s3Client.doesObjectExist(bucketName, uploadPath)
  }

  def downloadFromS3(filePath: String) {
    val savePath: String = mainPath + filePath

    var dirPath: String = savePath.substring(0, savePath.lastIndexOf('/'))
    var newDir = new File(dirPath)
    newDir.mkdir()

    s3Client.getObject(
      new GetObjectRequest(bucketName, filePath),
      new File(savePath))
    println(s"downloaded from s3 to path: $filePath")
  }

}