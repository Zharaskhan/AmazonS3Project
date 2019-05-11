import spray.json.DefaultJsonProtocol._
import models.{ Body, Response }
import spray.json.RootJsonFormat

trait JsonSupport {
  implicit val bodyFormat: RootJsonFormat[Body] = jsonFormat1(Body)
  implicit val responseFormat = jsonFormat2(Response)
}