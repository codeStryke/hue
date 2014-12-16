package com.cloudera.hue.sparker.repl.webapp

import akka.util.Timeout
import com.cloudera.hue.sparker.repl.interpreter.SparkerInterpreter
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.scalatra.{Accepted, AsyncResult, FutureSupport, ScalatraServlet}

import scala.concurrent.{Future, ExecutionContext, ExecutionContextExecutor}

class SparkerApp(interpreter: SparkerInterpreter) extends ScalatraServlet with FutureSupport with JacksonJsonSupport {

  protected implicit def executor: ExecutionContextExecutor = ExecutionContext.global
  protected implicit def defaultTimeout: Timeout = Timeout(10)
  protected implicit val jsonFormats: Formats = DefaultFormats

  sealed trait State
  case class Starting() extends State
  case class Running() extends State
  case class ShuttingDown() extends State

  var state: State = Starting()

  before() {
    contentType = formats("json")

    state match {
      case ShuttingDown() => halt(500, "Shutting down")
      case _ => {}
    }
  }

  get("/") {
    Map("state" -> state)
  }

  get("/statements") {
    interpreter.statements
  }

  post("/statements") {
    val req = parsedBody.extract[ExecuteRequest]
    val statement = req.statement
    new AsyncResult { val is = interpreter.execute(statement) }
  }

  delete("/") {
    Future {
      state = ShuttingDown()
      interpreter.close()
      Thread.sleep(1000)
      System.exit(0)
    }
    Accepted()
  }
}

case class ExecuteRequest(statement: String)
