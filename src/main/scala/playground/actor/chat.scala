package playground.actor

import java.util.UUID
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.util.Timeout
import scala.concurrent.duration.*
import scala.util.Random
import ChatProtocol.*
import scala.util.Failure
import scala.util.Success
import playground.actor.ChatRoom.toUserInfo

type RoomActor = ActorRef[ChatProtocol.RoomCommand]
type UserActor = ActorRef[ChatProtocol.UserCommand]
type ManagerActor = ActorRef[ChatProtocol.ManagerCommand]

case class Message(text: String)

// === Chat Room Models ===
opaque type RoomId = UUID
object RoomId:
  def apply(uuid: UUID): RoomId = uuid

case class Room(id: RoomId, name: String)
object Room:
  def apply(name: String) = new Room(RoomId(UUID.randomUUID()), name)

// === User Models ===
opaque type UserId = UUID
object UserId:
  def apply(uuid: UUID): UserId = uuid

case class User(id: UserId, name: String)
object User:
  def apply(name: String) = new User(UserId(UUID.randomUUID()), name)

// === Chat Commands ===
object ChatProtocol:
  enum RoomCommand:
    case PostMessage(from: User, message: Message)
    case UserJoined(user: User, replyTo: UserActor)
    case UserLeft(user: User)
    case ListMembers(replyTo: ActorRef[List[User]])

  enum ManagerCommand:
    case JoinRoom(
        roomName: String,
        user: User,
        replyTo: UserActor
    )
    case LeaveRoom(roomId: RoomId, user: User)
    case CreateRoom(roomName: String, user: User, replyTo: UserActor)
    case ListRooms(replyTo: ActorRef[List[Room]])
    case ListMembers(roomName: String, replyTo: ActorRef[List[User]])

  enum UserCommand:
    case PostMessage(message: Message)
    case ReceiveMessage(from: User, message: Message)
    case CreateRoom(roomName: String)
    case JoinRoom(roomName: String)
    case RoomJoined(room: Room, roomActor: RoomActor)
    case RoomJoinFailed(cause: String)
    case LeaveRoom

// === Chat Manager Actor ===
object ChatManager:

  type State = Map[Room, RoomActor]
  private def actorName(name: String) = name.replace(" ", "-").toLowerCase()

  def apply(rooms: State = Map.empty): Behavior[ManagerCommand] =
    Behaviors.setup: ctx =>
      Behaviors.receiveMessage[ManagerCommand]:
        case ManagerCommand.JoinRoom(roomName, user, replyTo) =>
          rooms.find((room, _) => room.name == roomName) match
            case None =>
              replyTo ! UserCommand.RoomJoinFailed("Room does not exist!")
            case Some((room, roomRef)) =>
              roomRef ! RoomCommand.UserJoined(user, replyTo)

          Behaviors.same

        case ManagerCommand.LeaveRoom(roomId, user) =>
          rooms
            .find((room, _) => room.id == roomId)
            .foreach((_, roomRef) => roomRef ! RoomCommand.UserLeft(user))

          Behaviors.same

        case ManagerCommand.CreateRoom(roomName, user, replyTo) =>
          val newRoom = Room(roomName)
          val roomRef = ctx.spawn(
            ChatRoom(newRoom, Set(user.toUserInfo(replyTo))),
            actorName(roomName)
          )

          roomRef ! RoomCommand.UserJoined(user, replyTo)
          apply(rooms + (newRoom -> roomRef))

        case ManagerCommand.ListRooms(replyTo) => Behaviors.same

        case ManagerCommand.ListMembers(roomName, replyTo) =>
          rooms
            .find((room, _) => room.name == roomName)
            .foreach((_, roomRef) => roomRef ! RoomCommand.ListMembers(replyTo))

          Behaviors.same

// === Chat Room Actor ===
object ChatRoom:
  case class UserInfo(user: User, actor: UserActor):
    export user.*

  extension (user: User)
    def toUserInfo(actor: UserActor) = UserInfo(user, actor)

  def apply(room: Room, members: Set[UserInfo]): Behavior[RoomCommand] =
    Behaviors.receive[RoomCommand]: (ctx, msg) =>
      msg match
        case RoomCommand.UserJoined(user, replyTo) =>
          replyTo ! UserCommand.RoomJoined(room, ctx.self)
          apply(room, members + user.toUserInfo(replyTo))

        case RoomCommand.UserLeft(user) =>
          ctx.log.info("[ROOM] User: {} left", user.name)
          apply(room, members.filterNot(_.user.id == user.id))

        case RoomCommand.PostMessage(from, msg) =>
          members
            .filterNot(_.id == from.id)
            .foreach(_.actor ! UserCommand.ReceiveMessage(from, msg))
          Behaviors.same

        case RoomCommand.ListMembers(replyTo) =>
          replyTo ! members.map(_.user).toList
          Behaviors.same

object ChatUser:

  def apply(name: String, manager: ManagerActor): Behavior[UserCommand] =
    Behaviors.setup: ctx =>
      val user = User(name)

      Behaviors.receiveMessage[UserCommand]:
        case UserCommand.CreateRoom(roomName) =>
          manager ! ManagerCommand.CreateRoom(roomName, user, ctx.self)
          Behaviors.same

        case UserCommand.JoinRoom(roomName) =>
          manager ! ManagerCommand.JoinRoom(roomName, user, ctx.self)
          Behaviors.same

        case UserCommand.RoomJoined(room, roomRef) =>
          roomMember(user, room, roomRef, manager)

        case UserCommand.RoomJoinFailed(msg) =>
          ctx.log.info("[{}] Failed to join room, reason: {}", user.name, msg)
          Behaviors.same

        case msg @ UserCommand.LeaveRoom =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

        case msg: UserCommand.PostMessage =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

        case msg: UserCommand.ReceiveMessage =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

  private def roomMember(
      user: User,
      room: Room,
      roomActor: RoomActor,
      manager: ManagerActor
  ): Behavior[UserCommand] =
    Behaviors.setup: ctx =>
      Behaviors.receiveMessage[UserCommand]:
        case UserCommand.PostMessage(message) =>
          roomActor ! RoomCommand.PostMessage(user, message)
          Behaviors.same

        case UserCommand.ReceiveMessage(from, message) =>
          ctx.log.info("[{}] From {}: {}", user.name, from.name, message.text)
          Behaviors.same

        case _ @UserCommand.LeaveRoom =>
          manager ! ManagerCommand.LeaveRoom(room.id, user)
          Behaviors.same

        case msg: UserCommand.CreateRoom =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

        case msg: UserCommand.JoinRoom =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

        case msg: UserCommand.RoomJoined =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

        case msg: UserCommand.RoomJoinFailed =>
          ctx.log.info("[{}] Unsupported command: {}", user.name, msg)
          Behaviors.same

@main def main() =
  given Timeout = 5.seconds

  def mainActor() = Behaviors.setup: ctx =>
    val chatManager = ctx.spawn(ChatManager(), "chat-manager")
    def printMembers(roomName: String) =
      ctx.ask[ManagerCommand, List[User]](chatManager, ManagerCommand.ListMembers(roomName, _)) {
        case Failure(ex) => ex.printStackTrace()
        case Success(members) => println(members.map(_.name).mkString(", "))
      }

    val adminUser = ctx.spawn(ChatUser("Admin", chatManager), "admin")
    val members = (1 to 10).map(i =>
      ctx.spawn(ChatUser(s"User $i", chatManager), s"user-$i")
    )

    adminUser ! UserCommand.CreateRoom("Room 1")
    Thread.sleep(1000)

    members.foreach(_ ! UserCommand.JoinRoom("Room 1"))
    Thread.sleep(1000)

    adminUser ! UserCommand.PostMessage(Message("Hello, World!"))

    Thread.sleep(1000)
    printMembers("Room 1")
    Thread.sleep(1000)

    Random.shuffle(members).take(2).foreach(_ ! UserCommand.LeaveRoom)
    Thread.sleep(1000)
    printMembers("Room 1")

    Behaviors.empty

  ActorSystem(mainActor(), "main")
