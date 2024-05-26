package playground

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.derived.*

import scala.deriving.Mirror
import scala.compiletime.*

case class Person(name: String, age: Int) derives Show, MyShow
enum Animal derives MyShow:
  case Dog(name: String)
  case Cat(name: String)

object Animal:
  given MyShow[Dog] = MyShow.derived
  given MyShow[Cat] = MyShow.derived

val showProgram =
  val person = Person("Jammy", 31)
  val dog: Animal = Animal.Dog("Tommy")
  for
    _ <- IO.println("-" * 80)
    _ <- IO.println("Custom  -> " + MyShow[Person].show(person))
    _ <- IO.println("Custom  -> " + MyShow[Animal].show(dog))
    _ <- IO.println("Kittens -> " + Show[Person].show(person))
    _ <- IO.println("-" * 80)
  yield ()

trait MyShow[T]:
  def show(a: T): String

object MyShow:
  def apply[T](using s: MyShow[T]): MyShow[T] = s

  given MyShow[String] with
    def show(t: String): String = t

  given MyShow[Int] with
    def show(t: Int): String = t.toString

  given MyShow[Boolean] with
    def show(t: Boolean): String = t.toString

  inline def derived[T](using m: Mirror.Of[T]): MyShow[T] =
    inline m match
      case p: Mirror.ProductOf[T] => productOf(p)
      case s: Mirror.SumOf[T]     => sumOf(s)

  private inline def productOf[T](p: Mirror.ProductOf[T]): MyShow[T] =
    new MyShow[T]:
      def show(t: T): String =
        val elems = t.asInstanceOf[Product].productIterator
        val labels = constValueTuple[p.MirroredElemLabels].productIterator
        val instances = summonAll[p.MirroredElemTypes].productIterator
        val fields = (labels zip (elems zip instances)).map {
          case (label, (value, instance)) =>
            s"${label.asInstanceOf[String]} = ${instance.asInstanceOf[MyShow[Any]].show(value)}"
        }
        s"${p.fromProduct(t.asInstanceOf[Product]).getClass.getSimpleName}(${fields.mkString(", ")})"

  private inline def sumOf[T](s: Mirror.SumOf[T]): MyShow[T] = new MyShow[T]:
    def show(t: T): String =
      val ord = s.ordinal(t)
      val instance = summonFromOrdinal[s.MirroredElemTypes](ord)
      instance.asInstanceOf[MyShow[Any]].show(t.asInstanceOf[Any])

  private inline def summonFromOrdinal[T <: Tuple](ordinal: Int): MyShow[?] =
    inline erasedValue[T] match
      case _: (head *: tail) =>
        if ordinal == 0 then summonInline[MyShow[head]]
        else summonFromOrdinal[tail](ordinal - 1)
      case _: EmptyTuple => throw new MatchError("Invalid ordinal")

  private inline def summonAll[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts)  => summonInline[MyShow[t]] *: summonAll[ts]

  private inline def constValueTuple[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts)  => constValue[t] *: constValueTuple[ts]
