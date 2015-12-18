package mesosphere.marathon.health

import java.net.{ InetAddress, ServerSocket }

import akka.actor.Props
import akka.testkit.{ ImplicitSender, TestActorRef }
import mesosphere.marathon.Protos.HealthCheckDefinition.Protocol
import mesosphere.marathon.test.MarathonActorSupport
import mesosphere.marathon.{ MarathonSpec, Protos }
import org.scalatest.{ BeforeAndAfterAll, Matchers }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class HealthCheckWorkerActorTest
    extends MarathonActorSupport
    with ImplicitSender
    with MarathonSpec
    with Matchers
    with BeforeAndAfterAll {

  import HealthCheckWorker._
  import mesosphere.util.ThreadPoolContext.context

  test("A TCP health check should correctly resolve the hostname") {
    val socket = new ServerSocket(0)
    val socketPort: Int = socket.getLocalPort

    val res = Future {
      socket.accept().close()
    }

    val task = Protos.MarathonTask
      .newBuilder
      .setHost(InetAddress.getLocalHost.getCanonicalHostName)
      .setId("test_id")
      .addPorts(socketPort)
      .build()

    val ref = TestActorRef[HealthCheckWorkerActor](Props(classOf[HealthCheckWorkerActor]))

    ref ! HealthCheckJob(task, HealthCheck(protocol = Protocol.TCP, portIndex = Some(0)))

    try { Await.result(res, 1.seconds) }
    finally { socket.close() }

    expectMsgPF(1.seconds) {
      case Healthy(taskId, _, _) => ()
    }
  }

  test("A health check worker should shut itself down") {
    val socket = new ServerSocket(0)
    val socketPort: Int = socket.getLocalPort

    val res = Future {
      socket.accept().close()
    }

    val task = Protos.MarathonTask
      .newBuilder
      .setHost(InetAddress.getLocalHost.getCanonicalHostName)
      .setId("test_id")
      .addPorts(socketPort)
      .build()

    val ref = TestActorRef[HealthCheckWorkerActor](Props(classOf[HealthCheckWorkerActor]))

    ref ! HealthCheckJob(task, HealthCheck(protocol = Protocol.TCP, portIndex = Some(0)))

    try { Await.result(res, 1.seconds) }
    finally { socket.close() }

    expectMsgPF(1.seconds) {
      case _: HealthResult => ()
    }

    watch(ref)
    expectTerminated(ref)
  }

  test("getPort") {
    val ref = TestActorRef[HealthCheckWorkerActor](Props(classOf[HealthCheckWorkerActor]))

    val check = new HealthCheck(port = Some(1234))
    val task = Protos.MarathonTask
      .newBuilder
      .setHost("fakehostname")
      .setId("test_id")
      .addPorts(4321)
      .build()
    assert(ref.underlyingActor.getPort(task, check) == Some(1234))
  }
}
