import java.io.File

import actors.{ Task1, Task2 }
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import org.slf4j.LoggerFactory
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.amazonaws.AmazonServiceException
import org.slf4j.LoggerFactory
import models.{ Body, Response }

import scala.concurrent.duration._

object Boot extends App with JsonSupport {
  var bucketName: String = "zaman-lab"
  var resourcePath: String = "src/main/resources/"
  implicit val timeout = Timeout(30.seconds)

  // needed to run the route
  implicit val system = ActorSystem()

  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  val log = LoggerFactory.getLogger("Boot")

  val awsCreds = new BasicAWSCredentials(
    "","")

  // Frankfurt client
  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard
    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    .withRegion(Regions.EU_CENTRAL_1)
    .build
  // check if bucket exists

  if (s3Client.doesBucketExistV2(bucketName)) {
    log.info("Bucket exists")
  } else {
    s3Client.createBucket(bucketName)
    log.info("Bucket created")
  }

  val task1helper = system.actorOf(Task1.props(s3Client, resourcePath + "s3/", bucketName), "task1helper")

  val outFile = new File(resourcePath + "/out")
  val task2helper = system.actorOf(Task2.props(s3Client, bucketName, outFile, resourcePath), "task2helper")

  val route = path("file") {
    get {
      parameters('filename.as[String]) { path =>
        complete {
          log.info("Path")
          (task1helper ? Task1.Download(path)).mapTo[Response]
        }
      }
    } ~
      post {
        entity(as[Body]) { body =>
          complete {
            (task1helper ? Task1.Upload(body.path)).mapTo[Response]
          }
        }
      }
  } ~ pathPrefix("task2") {
    path("out") {
      get {
        complete {
          (task2helper ? Task2.OUT).mapTo[Response]
        }
      }
    } ~ path("in") {
      get {
        complete {
          (task2helper ? Task2.IN).mapTo[Response]
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
  log.info("Listening on port 8080...")
}
