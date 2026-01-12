import { cn } from "../../../utils";

/**
 * MusicIcon 共通プロパティ
 */
export interface MusicIconProps {
  /** サイズ (数値ならpx, 文字列ならそのまま適用) */
  size?: number | string;
  /** 拡大率 (デフォルトの調整値を基準にさらに拡大縮小したい場合) */
  scale?: number;
  /** 色 */
  color?: string;
  /** 手動の垂直オフセット (内蔵値をさらに微調整したい場合) */
  yOffset?: number;
  /** 追加クラス */
  className?: string;
}

/**
 * 内部用のベースコンポーネント
 */
interface MusicIconBaseProps extends MusicIconProps {
  glyph: string;
  defaultOffset: number;
  defaultScale: number;
}

const MusicIconBase: React.FC<MusicIconBaseProps> = ({
  glyph,
  size = 24,
  scale = 1.0,
  color = "currentColor",
  yOffset,
  defaultOffset,
  defaultScale,
  className = "",
}) => {
  const char = String.fromCodePoint(parseInt(glyph, 16));

  const finalScale = defaultScale * scale;
  const finalOffset = yOffset ?? defaultOffset;

  return (
    <span
      aria-hidden="true"
      className={cn(
        "inline-flex items-center justify-center leading-none select-none",
        className,
      )}
      style={{
        fontFamily: "'Bravura', serif",
        fontSize: size,
        color,
        transform: `translateY(${finalOffset}px) scale(${finalScale})`,
      }}
    >
      {char}
    </span>
  );
};

// --- 個別コンポーネントの定義 ---

// --- 1. 音符 (Notes) ---
export const WholeNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1D2" defaultOffset={4} defaultScale={1.1} {...p} />
);
export const HalfNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1D3" defaultOffset={6} defaultScale={0.8} {...p} />
);
export const QuarterNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1D5" defaultOffset={6} defaultScale={0.8} {...p} />
);
export const EighthNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1D7" defaultOffset={6} defaultScale={0.8} {...p} />
);
export const SixteenthNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1D9" defaultOffset={6} defaultScale={0.75} {...p} />
);
export const ThirtySecondNote = (p: MusicIconProps) => (
  <MusicIconBase glyph="E1DB" defaultOffset={6} defaultScale={0.7} {...p} />
);

// --- 2. 休符 (Rests) ---
export const WholeRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4F4" defaultOffset={-2} defaultScale={1.0} {...p} />
);
export const HalfRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4F5" defaultOffset={2} defaultScale={1.0} {...p} />
);
export const QuarterRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4E5" defaultOffset={0} defaultScale={1.0} {...p} />
);
export const EighthRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4E6" defaultOffset={0} defaultScale={1.0} {...p} />
);
export const SixteenthRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4E7" defaultOffset={0} defaultScale={1.0} {...p} />
);
export const ThirtySecondRest = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4E8" defaultOffset={0} defaultScale={1.0} {...p} />
);

// --- 3. 変化記号 (Accidentals) ---
export const Sharp = (p: MusicIconProps) => (
  <MusicIconBase glyph="E262" defaultOffset={0} defaultScale={0.9} {...p} />
);
export const Flat = (p: MusicIconProps) => (
  <MusicIconBase glyph="E260" defaultOffset={3} defaultScale={0.9} {...p} />
);
export const Natural = (p: MusicIconProps) => (
  <MusicIconBase glyph="E261" defaultOffset={0} defaultScale={0.9} {...p} />
);
export const DoubleSharp = (p: MusicIconProps) => (
  <MusicIconBase glyph="E263" defaultOffset={0} defaultScale={0.9} {...p} />
);
export const DoubleFlat = (p: MusicIconProps) => (
  <MusicIconBase glyph="E264" defaultOffset={3} defaultScale={0.9} {...p} />
);

// --- 4. 音部記号 (Clefs) ---
export const GClef = (p: MusicIconProps) => (
  <MusicIconBase glyph="E050" defaultOffset={2} defaultScale={0.6} {...p} />
);
export const FClef = (p: MusicIconProps) => (
  <MusicIconBase glyph="E062" defaultOffset={-3} defaultScale={0.8} {...p} />
);
export const CClef = (p: MusicIconProps) => (
  <MusicIconBase glyph="E05C" defaultOffset={0} defaultScale={0.8} {...p} />
);

// --- 5. 演奏記号・その他 (Articulations / Misc) ---
export const Staccato = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4A0" defaultOffset={4} defaultScale={1.0} {...p} />
);
export const Accent = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4A2" defaultOffset={1} defaultScale={1.0} {...p} />
);
export const Tenuto = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4A4" defaultOffset={1} defaultScale={1.0} {...p} />
);
export const Fermata = (p: MusicIconProps) => (
  <MusicIconBase glyph="E4C0" defaultOffset={4} defaultScale={1.0} {...p} />
);
