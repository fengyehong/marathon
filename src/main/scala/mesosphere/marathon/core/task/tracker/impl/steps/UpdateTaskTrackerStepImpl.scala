package mesosphere.marathon.core.task.tracker.impl.steps

import com.google.inject.Inject
import mesosphere.marathon.Protos.MarathonTask
import mesosphere.marathon.core.task.tracker.TaskStatusUpdateStep
import mesosphere.marathon.state.{ PathId, Timestamp }
import mesosphere.marathon.tasks.TaskTracker
import org.apache.mesos.Protos.TaskStatus

import scala.concurrent.Future

/**
  * Update task tracker corresponding to the event.
  */
class UpdateTaskTrackerStepImpl @Inject() (
    taskTracker: TaskTracker) extends TaskStatusUpdateStep {

  def name: String = "updateTaskTracker"

  def processUpdate(
    timestamp: Timestamp, appId: PathId, task: MarathonTask, status: TaskStatus): Future[_] = {

    taskTracker.statusUpdate(appId, status)
  }
}
