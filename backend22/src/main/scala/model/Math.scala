package model

import scala.math.abs

object Math {
  // Python style modulo (always positive if b is positive)
  def mod(a: Int, b: Int): Int = ((a % b) + b) % b

  def gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

  case class Rational(numerator: Int, denominator: Int = 1) extends Ordered[Rational] {
    require(denominator != 0)

    private val g = gcd(abs(numerator), abs(denominator))
    val n: Int    = numerator / g
    val d: Int    = abs(denominator) / g // Always keep denominator positive

    def +(that: Rational): Rational = Rational(n * that.d + that.n * d, d * that.d)
    def -(that: Rational): Rational = Rational(n * that.d - that.n * d, d * that.d)
    def *(that: Rational): Rational = Rational(n * that.n, d * that.d)
    def *(i: Int): Rational         = Rational(n * i, d)
    def /(that: Rational): Rational = Rational(n * that.d, d * that.n)

    override def compare(that: Rational): Int = (this.n * that.d) compare (that.n * this.d)

    override def equals(obj: Any): Boolean = obj match {
      case that: Rational => this.n == that.n && this.d == that.d
      case _              => false
    }

    override def hashCode(): Int = (n, d).hashCode()

    override def toString: String = if (d == 1) s"$n" else s"$n/$d"

    def toDouble: Double = n.toDouble / d
  }

}
