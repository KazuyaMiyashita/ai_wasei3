import { Rational } from "../../shared/math";

export function getMeasureNumberById(
  meiXML: Document,
  id: string,
): number | null {
  try {
    // 1. [xml:id='...'] 属性で要素を検索（getElementById よりも確実）
    // CSSセレクタではコロンをエスケープするか、属性名だけで検索します
    let targetNode = meiXML.querySelector(`[*|id="${id}"]`) as Element | null;

    // 万が一見つからない場合のフォールバック（通常の id 属性も探す）
    if (!targetNode) {
      targetNode = meiXML.querySelector(`[id="${id}"]`);
    }

    if (!targetNode) {
      // デバッグ用: ID自体が見つかっていない場合
      // console.warn(`ID not found in MEI: ${id}`);
      return null;
    }

    // 2. 親要素を順に辿って <measure> を探す（名前空間を無視して localName で判定）
    let current: Element | null = targetNode;
    while (current) {
      if (current.localName === "measure") {
        const n = current.getAttribute("n");
        return n ? parseInt(n, 10) : null;
      }
      current = current.parentElement;
    }
  } catch (e) {
    console.error("Error finding measure for ID:", id, e);
    return null;
  }

  return null;
}

export function getStaffNumberById(
  meiXML: Document,
  id: string,
): number | null {
  try {
    let targetNode = meiXML.querySelector(`[*|id="${id}"]`) as Element | null;
    if (!targetNode) {
      targetNode = meiXML.querySelector(`[id="${id}"]`);
    }
    if (!targetNode) return null;

    let current: Element | null = targetNode;
    while (current) {
      if (current.localName === "staff") {
        const n = current.getAttribute("n");
        return n ? parseInt(n, 10) : null;
      }
      current = current.parentElement;
    }
  } catch (e) {
    console.error("Error finding staff for ID:", id, e);
    return null;
  }
  return null;
}

/**
 * 単一要素の持続時間を「四分音符を1」として計算する
 */
function getElementDuration(el: Element): Rational {
  const durAttr = el.getAttribute("dur");
  if (!durAttr) return new Rational(0);

  let baseDenom = parseInt(durAttr, 10);
  let baseNum = 4; // 四分音符を1とする (全音符=4)

  // 付点の計算
  const dots = parseInt(el.getAttribute("dots") || "0", 10);
  if (dots === 1) {
    baseNum *= 3;
    baseDenom *= 2;
  } else if (dots === 2) {
    baseNum *= 7;
    baseDenom *= 4;
  }

  let duration = new Rational(baseNum, baseDenom);

  // 連符の倍率計算
  const tuplet = el.closest("tuplet");
  if (tuplet) {
    const num = parseInt(tuplet.getAttribute("num") || "3", 10);
    const numbase = parseInt(tuplet.getAttribute("numbase") || "2", 10);
    // 乗算メソッドを使用
    duration = duration.mul(new Rational(numbase, num));
  }

  return duration;
}

/**
 * XMLから拍の位置を計算する
 */
export function getBeatByXmlCalculation(
  meiXML: Document,
  id: string,
): Rational | null {
  const target = meiXML.querySelector(`[*|id="${id}"]`);
  if (!target) return null;

  const layer = target.closest("layer");
  if (!layer) return null;

  const events = Array.from(layer.querySelectorAll("note, rest, chord, mRest"));

  // 累積用の変数を 1/1 (第1拍) で初期化
  let totalDuration = new Rational(1);

  for (const event of events) {
    // chord の中の note は、chord 自体の持続時間で計算するためスキップする。
    // ただし target 自身が note の場合は、親の chord に到達した時点で break する。
    if (
      event.localName === "note" &&
      event.parentElement?.localName === "chord"
    ) {
      continue;
    }

    if (event === target || event.contains(target)) break;

    const dur = getElementDuration(event);
    // メソッドチェーンで加算
    totalDuration = totalDuration.add(dur);
  }

  return totalDuration;
}

// 2スペースでフォーマットされたXMLに変換する
export function formatXml(xml: string): string | null {
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xml, "application/xml");
    if (doc.querySelector("parsererror")) return null;

    const formatNode = (node: Node, level: number): string => {
      const indent = "  ".repeat(level);
      if (node.nodeType === Node.TEXT_NODE) {
        return (node.textContent || "").trim();
      }
      if (node.nodeType === Node.ELEMENT_NODE) {
        const el = node as Element;
        const tagName = el.tagName;
        let str = `${indent}<${tagName}`;
        const attrs = Array.from(el.attributes).filter(
          (attr) => attr.name !== "xmlns" && !attr.name.startsWith("xmlns:"),
        );
        if (attrs.length > 0) {
          str += attrs.map((attr) => ` ${attr.name}="${attr.value}"`).join("");
        }

        const children = Array.from(el.childNodes);
        const hasElementChildren = children.some(
          (n) => n.nodeType === Node.ELEMENT_NODE,
        );

        if (children.length === 0) {
          return `${str} />`;
        }

        if (!hasElementChildren) {
          const textContent = (el.textContent || "").trim();
          return `${str}>${textContent}</${tagName}>`;
        }

        str += ">\n";
        for (const child of children) {
          const formattedChild = formatNode(child, level + 1);
          if (formattedChild) {
            str += `${formattedChild}\n`;
          }
        }
        str += `${indent}</${tagName}>`;
        return str;
      }
      return "";
    };

    return formatNode(doc.documentElement, 0);
  } catch (e) {
    console.warn("Formatting failed", e);
    return null;
  }
}
