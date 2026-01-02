export class Rational {
  public readonly n: number; // 分子 (Numerator)
  public readonly d: number; // 分母 (Denominator)

  constructor(n: number, d: number = 1) {
    if (d === 0) {
      throw new Error("Denominator cannot be zero.");
    }

    // 負の数の符号を分子に集約させる
    const sign = d < 0 ? -1 : 1;
    const common = Rational.gcd(Math.abs(n), Math.abs(d));

    this.n = (n / common) * sign;
    this.d = Math.abs(d) / common;
  }

  /**
   * 最大公約数 (Greatest Common Divisor) を求める
   */
  private static gcd(a: number, b: number): number {
    return b === 0 ? a : Rational.gcd(b, a % b);
  }

  /**
   * 加算: 新しい Rational インスタンスを返す
   */
  add(other: Rational): Rational {
    return new Rational(this.n * other.d + other.n * this.d, this.d * other.d);
  }

  /**
   * 減算
   */
  sub(other: Rational): Rational {
    return new Rational(this.n * other.d - other.n * this.d, this.d * other.d);
  }

  /**
   * 乗算
   */
  mul(other: Rational): Rational {
    return new Rational(this.n * other.n, this.d * other.d);
  }

  /**
   * 数値（小数）に変換
   */
  toNumber(): number {
    return this.n / this.d;
  }

  /**
   * 文字列表示 (例: "3/4")
   */
  toString(): string {
    return `${this.n}/${this.d}`;
  }

  /**
   * 小節内の拍表示用 (例: "1 + 1/2")
   */
  toMusicalString(): string {
    if (this.n === 0) return "0";
    if (this.d === 1) return `${this.n}`;
    if (this.n > this.d) {
      const whole = Math.floor(this.n / this.d);
      const rem = this.n % this.d;
      return rem === 0 ? `${whole}` : `${whole} + ${rem}/${this.d}`;
    }
    return `${this.n}/${this.d}`;
  }
}
