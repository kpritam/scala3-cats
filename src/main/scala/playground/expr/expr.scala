package playground.expr

sealed trait Expr[T]
case class IntLit(value: Int) extends Expr[Int]
case class BoolLit(value: Boolean) extends Expr[Boolean]
case class StringLit(value: String) extends Expr[String]
case class Or(left: Expr[Boolean], right: Expr[Boolean]) extends Expr[Boolean]
case class Sum(left: Expr[Int], right: Expr[Int]) extends Expr[Int]

def eval[T](expr: Expr[T]): T =
  expr match
    case IntLit(value) => value
    case BoolLit(value) => value
    case StringLit(value) => value
    case Or(left, right) => eval(left) || eval(right)
    case Sum(left, right) => eval(left) + eval(right)

@main def main() =
  println(eval(Sum(IntLit(2), IntLit(5))))
  println(eval(Or(BoolLit(false), BoolLit(true))))