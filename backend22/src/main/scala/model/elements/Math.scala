package model.elements

import scala.math.abs

object Math {
  // Python style modulo (always positive if b is positive)
  def mod(a: Long, b: Long): Long = ((a % b) + b) % b
  def mod(a: Int, b: Int): Int    = ((a % b) + b) % b

  def gcd(a: Long, b: Long): Long = if (b == 0) a else gcd(b, a % b)

  case class Rational(private val numerator: Long, private val denominator: Long = 1) extends Ordered[Rational] {
    require(denominator != 0)

    private val g = gcd(abs(numerator), abs(denominator))
    val n: Long   = numerator / g
    val d: Long   = abs(denominator) / g // Always keep denominator positive

    def +(that: Rational): Rational = Rational(n * that.d + that.n * d, d * that.d)
    def -(that: Rational): Rational = Rational(n * that.d - that.n * d, d * that.d)
    def *(that: Rational): Rational = Rational(n * that.n, d * that.d)
    def *(i: Long): Rational        = Rational(n * i, d)
    def /(that: Rational): Rational = Rational(n * that.d, d * that.n)

    override def compare(that: Rational): Int = (this.n * that.d) compare (that.n * this.d)

    override def equals(obj: Any): Boolean = obj match {
      case that: Rational => this.n == that.n && this.d == that.d
      case _              => false
    }

    override def hashCode(): Int = (n, d).hashCode()

    override def toString: String = if (d == 1) s"$n" else s"$n/$d"

    def toMixedNumberString: String = {
      val (whole, frac) = toMixedNumber
      if (whole == 0 && frac.n == 0) "0"
      else if (whole == 0) s"${frac.n}/${frac.d}"
      else if (frac.n == 0) s"$whole"
      else s"$whole+${frac.n}/${frac.d}"
    }

    def toDouble: Double = n.toDouble / d

    /** Rationalを帯分数に変換する。
      * 整数部分 (Long) と分数部分 (Rational) のタプルを返す。
      * 分数部分は常に正の値になる (例: -2 1/2 は (-2, 1/2))。
      */
    def toMixedNumber: (Long, Rational) = {
      val whole     = n / d
      val remainder = abs(n) % d // 分子を絶対値にして余りを計算
      if (remainder == 0) {
        (whole, Rational(0, 1))
      } else {
        (whole, Rational(remainder, d))
      }
    }
  }

  object Rational {

    /** Pational.parse("1+1/2") == Rational.of(3, 2) */
    def parse(s: String): Rational = {
      val trimmed   = s.trim
      val plusIndex = trimmed.indexOf('+')

      if (plusIndex != -1) {
        val wholePartStr    = trimmed.substring(0, plusIndex)
        val fractionPartStr = trimmed.substring(plusIndex + 1)
        parse(wholePartStr) + parse(fractionPartStr)
      } else {
        val slashIndex = trimmed.indexOf('/')
        if (slashIndex != -1) {
          val numStr = trimmed.substring(0, slashIndex)
          val denStr = trimmed.substring(slashIndex + 1)
          Rational(numStr.toLong, denStr.toLong)
        } else {
          Rational(trimmed.toLong)
        }
      }
    }
  }

}
