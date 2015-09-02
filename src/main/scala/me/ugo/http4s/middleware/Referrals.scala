package me.ugo.http4s.middleware

import org.http4s.server._
import org.http4s._
import org.http4s.Http4s._

import scala.util.matching.Regex

/**
 * Created by Ugo Bataillard on 9/2/15.
 */

object Referrals {

  val referringSearchEngine = AttributeKey[String]("referring.search_engine")
  val referringSearchTerms = AttributeKey[Seq[String]]("referring.search_terms")


  val DEFAULT_ENGINES = Map[String, (Regex,String)](
     // Borrowed from https://github.com/deviantech/rack-referrals
    "google"     -> ("""^https?:\/\/(www\.)?google.*/""".r -> "q"),
    "yahoo"      -> ("""^https?:\/\/([^\.]+.)?search\.yahoo.*/""".r -> "p"),
    "bing"       -> ("""^https?:\/\/search\.bing.*/""".r -> "q"),
    "biadu"      -> ("""^https?:\/\/(www\.)?baidu.*/""".r -> "wd"),
    "rambler"    -> ("""^https?:\/\/([^\.]+.)?rambler.ru/""".r -> "query"),
    "yandex"     -> ("""^https?:\/\/(www\.)?yandex.ru/""".r -> "text"),

     // Borrowed from https://github.com/squeejee/search_sniffer
    "msn"        -> ("""^https?:\/\/search\.msn.*/""".r -> "q"),
    "aol"        -> ("""^https?:\/\/(www\.)?\.aol.*/""".r -> "query"),
    "altavista"  -> ("""^https?:\/\/(www\.)?altavista.*/""".r -> "q"),
    "feedster"   -> ("""^https?:\/\/(www\.)?feedster.*/""".r -> "q"),
    "lycos"      -> ("""^https?:\/\/search\.lycos.*/""".r -> "query"),
    "alltheweb"  -> ("""^https?:\/\/(www\.)?alltheweb.*/""".r ->  "q")
  )

  def apply(service: HttpService): HttpService = Service.lift { request: Request =>

    request.headers.get("HTTP_REFERER".ci) match {
      case Some(Header(_,value)) if !value.isEmpty =>
        val finalRequest = (DEFAULT_ENGINES.find {case (name, data) => data._1.findFirstIn (value).isDefined} map { engine =>
          val rse = request.withAttribute(referringSearchEngine, engine._1)

          (for {
            uri <- Uri.fromString(value).toOption
            searchTerms <- uri.multiParams.get(engine._2._2)
          } yield {
            rse.withAttribute(referringSearchTerms,searchTerms)
          }) getOrElse rse
        }) getOrElse request

        service(finalRequest)
      case _ => service(request)
    }

  }
}
