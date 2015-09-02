package me.ugo.http4s.middleware

import org.http4s._
import org.http4s.server.{Service, HttpService}
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType._
import scodec.bits.ByteVector
import scalaz.stream.Process

import org.http4s.dsl._
import scalaz.concurrent.Task
import scalaz._
import Scalaz._
import scalaz.Kleisli._

/**
 * Created by Ugo Bataillard on 9/2/15.
 */
object JsonP {

  val VALID_JS_VAR    = """[a-zA-Z_$][\w$]*""".r
  val VALID_CALLBACK  = s"""^${VALID_JS_VAR}(?:\\.?${VALID_JS_VAR})*$$""".r

  def apply(service: HttpService): HttpService = Service.lift { req:Request =>

    req.params.get("callback") match {
      case Some(callback) if isJson(req.headers) =>
        if(!isValidCallback(callback)) {
          BadRequest()
        }
        else{
          service.map { response:Response =>
            pad(callback, response.withContentType(Some(`Content-Type`(`application/javascript`))))
          }.run(req)
        }
      case _ => service(req)
    }
  }


  def isJson(headers:Headers):Boolean =
  {
    headers.get(`Content-Type`) match {
      case Some(`Content-Type`(`application/json`,_)) => true
      case _ => false
    }
  }

  /** See:
  * http://stackoverflow.com/questions/1661197/valid-characters-for-javascript-variable-names
  *
  * NOTE: Supports dots (.) since callbacks are often in objects:
  */
  def isValidCallback(callback:String):Boolean =
  {
    VALID_CALLBACK.findFirstIn(callback).isDefined
  }


  /** Pads the response with the appropriate callback format according to the
  * JSON-P spec/requirements.
  *
  * The Rack response spec indicates that it should be enumerable. The
  * method of combining all of the data into a single string makes sense
  * since JSON is returned as a full string.
  */
  def pad(callback:String, response:Response) = {

    val newBody:EntityBody = response.body.prepend(Seq(ByteVector(s"/**/$callback(".getBytes))) ++ Process.emit(ByteVector(")".getBytes))

    /*#U + 2028 and U + 2029 are allowed inside strings in JSON(as all literal#Unicode characters) but JavaScript defines them as newline#seperators
      .Because no literal newlines are allowed in a string, this#causes a ParseError in the browser.We work around this issue by#replacing them with the escaped version
      .This should be safe because#according to the JSON spec, these characters are * only * valid inside#a string and should therefore not be present any other places
      .*/
    response.copy(body = newBody)
  }

}
