package ncreep

import java.net.URI
import ncreep.WikipediaAppResponse._

/** The commands that the interactive Wiki rabbit hole app supports. */
sealed trait WikipediaAppCommand extends Product with Serializable

object WikipediaAppCommand {
  case object Quit extends WikipediaAppCommand
  final case class PerformCommand(command: WikipediaCommand) extends WikipediaAppCommand
  case object InvalidCommand extends WikipediaAppCommand
}

/** Commands that explore Wiki rabbit holes. */
sealed trait WikipediaCommand extends Product with Serializable

object WikipediaCommand {
  final case class Browse(from: String) extends WikipediaCommand
  final case class Loop(from: String) extends WikipediaCommand
  final case class Find(from: String, target: String) extends WikipediaCommand
}

/** Responses to the user after executing a [[WikipediaAppCommand]]. */
sealed trait WikipediaAppResponse extends Product with Serializable {
  def render: String = {
    val str = this match {
      case Quitting => "kthxbye"
      case PerformedCommand(command, resultURI) =>
        s"""Executed: [${command.productPrefix}]
           |Rendered result at:
           |${resultURI}""".stripMargin
      case ReceivedInvalidCommand => "Invalid command, please try again"
    }

    s"$str\n"
  }
}

object WikipediaAppResponse {
  case object Quitting extends WikipediaAppResponse
  final case class PerformedCommand(command: WikipediaCommand, resultURI: URI) extends WikipediaAppResponse
  case object ReceivedInvalidCommand extends WikipediaAppResponse
}

