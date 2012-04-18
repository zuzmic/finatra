package com.posterous.finatra 

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpMethod, HttpVersion}
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._


object FakeApp extends FinatraApp {
  get("/") { "resp" }

  get("/other") { "otherresp" }
  head("/other") { "specialresp" }
  
  get("/jsonstuff") { toJson(Map("foo" -> "bar")) }
  
  get("/redirectme") { redirect("/gohere") }
}
 
@RunWith(classOf[JUnitRunner])
class RouterSpec extends FlatSpec with ShouldMatchers {

  FakeApp

  "GET /" should "respond 200" in {

    var request = Request(HttpMethod.GET, "/")
    var response = Router.dispatch(request)

    response.getStatus should equal (OK)
    response.getContent.toString("UTF8") should equal ("resp")
  }

  "HEAD /" should "respond 200" in {
    var request = Request(HttpMethod.HEAD, "/")
    var response = Router.dispatch(request)

    response.getStatus should equal (OK)  
    response.getContent.toString("UTF8") should equal ("resp")
  }

  "HEAD /other" should "respond 200" in {

    var request = Request(HttpMethod.HEAD, "/other")
    var response = Router.dispatch(request)
    
    response.getStatus should equal (OK)
    response.getContent.toString("UTF8") should equal ("specialresp")
  }
  
  "GET /other" should "respond 200" in {
    var request = Request(HttpMethod.GET, "/other")
    var response = Router.dispatch(request)

    response.getStatus should equal (OK)
    response.getContent.toString("UTF8") should equal ("otherresp")
  }

  "GET /jsonstuff" should "respond 200" in {

    var request = Request(HttpMethod.GET, "/jsonstuff")
    var response = Router.dispatch(request)

    response.getStatus should equal (OK)
    response.getContent.toString("UTF8") should equal ("{\"foo\":\"bar\"}")
  }

  "GET /redirect" should "redirect"  in {
    var request = Request(HttpMethod.GET, "/redirectme")
    var response = Router.dispatch(request)

    response.getStatus should equal (MOVED_PERMANENTLY)
    response.getHeader("Location") should equal ("/gohere")
  }

}
