import sax from "sax";

export interface IdRange {
  id: string;
  start: number; // 開始タグの "<" の位置
  end: number; // 閉じタグの ">" の位置
}

export function buildIdRangeMap(xmlString: string): IdRange[] {
  // strictモードをtrueに設定
  const parser = sax.parser(true);
  const ranges: IdRange[] = [];

  // 処理中のタグを一時保存するスタック
  // xml:id がない要素も階層構造を維持するためにスタックに入れる
  const stack: { id: string | null; start: number }[] = [];

  // 1. タグが始まった瞬間のイベント
  parser.onopentagstart = (_tag) => {
    // parser.startTagPosition は "<" の位置を指す
    // ※ sax-js の内部仕様により、1始まりの場合は調整が必要なことがありますが、
    // Monacoのオフセットと合わせるため、そのまま、あるいは -1 して保持します。
    stack.push({
      id: null, // まだ属性を読み込んでいないので一旦null
      start: parser.startTagPosition - 1,
    });
  };

  // 2. 属性を含めタグが完全に開いた時のイベント
  parser.onopentag = (node) => {
    const current = stack[stack.length - 1];
    if (current) {
      // xml:id 属性があるか確認
      current.id = (node.attributes["xml:id"] as string) || null;
    }
  };

  // 3. タグが閉じた時のイベント
  parser.onclosetag = () => {
    const entry = stack.pop();
    if (entry?.id) {
      // parser.position は ">" の直後の位置を指すため
      // その要素の末尾として記録
      ranges.push({
        id: entry.id,
        start: entry.start,
        end: parser.position,
      });
    }
  };

  // エラーハンドリング（入力中の不正なXMLでも止まらないようにする）
  parser.onerror = (_e) => {
    // エラーが発生しても、それまでに解析できた範囲を返すか、空を返す
    // parser.resume() を呼ぶと続行できる場合もあるが、
    // 構造が壊れている場合は正しい範囲が取れないため、ここでは何もしない
  };

  try {
    parser.write(xmlString).close();
  } catch (_e) {
    // saxが同期的にエラーを投げる場合の対策
    // console.error("XML parsing error:", _e);
  }
  return ranges;
}

/**
 * 範囲（またはカーソル位置）に基づいて、該当する要素のIDを検索する
 */
export function findIdsFromSelection(
  idMap: IdRange[],
  start: number,
  end: number,
): string[] {
  const isRange = start !== end;

  if (!isRange) {
    // カーソルの場合: カーソル位置を包含する最も深い要素を1つ選ぶ
    const containers = idMap.filter((r) => r.start <= start && r.end >= start);
    if (containers.length > 0) {
      const deepest = containers.reduce((prev, curr) =>
        curr.end - curr.start < prev.end - prev.start ? curr : prev,
      );
      return [deepest.id];
    }
    return [];
  }

  // 範囲選択の場合: 選択範囲内に開始位置または終了位置が含まれる要素をすべて選ぶ
  return idMap
    .filter((r) => {
      const startIsInside = r.start >= start && r.start <= end;
      const endIsInside = r.end >= start && r.end <= end;
      return startIsInside || endIsInside;
    })
    .map((r) => r.id);
}

export function findElementById(xml: Document, id: string): Element | null {
  try {
    const xpath = `//*[@xml:id="${id}"]`;
    const result = xml.evaluate(
      xpath,
      xml,
      (prefix) => {
        // Simple resolver for standard xml namespace if needed,
        // though typically xml: is strictly reserved and handled globally.
        if (prefix === "xml") return "http://www.w3.org/XML/1998/namespace";
        return null;
      },
      XPathResult.FIRST_ORDERED_NODE_TYPE,
      null,
    );
    if (result.singleNodeValue) {
      return result.singleNodeValue as Element;
    }
  } catch (_e) {
    // console.warn("XPath search failed", _e);
  }

  return null;
}

export function getAncestors(element: Element): Element[] {
  const ancestors: Element[] = [];
  let current: Element | null = element.parentElement;
  while (current) {
    ancestors.unshift(current);
    current = current.parentElement;
  }
  return ancestors;
}

export function findLowestCommonAncestor(
  xml: Document,
  ids: string[],
): Element | null {
  if (ids.length === 0) return null;

  const elements = ids
    .map((id) => findElementById(xml, id))
    .filter((el): el is Element => el !== null);

  if (elements.length === 0) return null;

  if (elements.length === 1) {
    return elements[0];
  }

  // Get ancestors for all elements (including the elements themselves)
  const paths = elements.map((el) => [...getAncestors(el), el]);

  // Find the longest common prefix
  let commonAncestor: Element | null = null;
  const shortestPathLength = Math.min(...paths.map((p) => p.length));

  for (let i = 0; i < shortestPathLength; i++) {
    const current = paths[0][i];
    const isCommon = paths.every((path) => path[i] === current);
    if (isCommon) {
      commonAncestor = current;
    } else {
      break;
    }
  }

  return commonAncestor;
}
