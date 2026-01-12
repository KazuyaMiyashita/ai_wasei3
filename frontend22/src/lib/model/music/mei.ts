import type { IEditAction } from "../../editor/actions";
import { MeiElementReplaceAction } from "../../editor/actions";
import { type Key, Pitch } from "./elements";

/**
 * Logic for handling MEI XML Structure
 *
 * Review Comment:
 * DOM操作を隠蔽するドメインモデルです。
 * パフォーマンスのためにMutableに操作を行いますが、Undo/Redoのために
 * 「操作結果として逆操作(IEditAction)を返す」あるいは「状態のスナップショットを取る」
 * 等の連携が ActiveDocument との間で必要になります。
 */
export class MEI {
  document: XMLDocument;

  constructor(source: string) {
    this.document = MEI.parseXML(source);
  }

  // --- Static Helpers ---

  private static parseXML(xmlString: string): XMLDocument {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlString, "text/xml");
    const errorNode = doc.querySelector("parsererror");
    if (errorNode) {
      throw new Error(`XML Parsing Error: ${errorNode.textContent}`);
    }
    return doc;
  }

  private nsResolver(prefix: string | null): string | null {
    if (prefix === "mei") return "http://www.music-encoding.org/ns/mei";
    if (prefix === "xml") return "http://www.w3.org/XML/1998/namespace";
    return null;
  }

  // --- Queries ---

  serialize(): string {
    return new XMLSerializer().serializeToString(this.document);
  }

  getNoteElementById(targetId: string): Element | null {
    // Review Comment: XPathは強力ですが、頻繁に呼ぶ場合はパフォーマンスに注意が必要です。
    // 必要に応じてIDマップをキャッシュする戦略も検討してください。
    const xpath = `//*[@xml:id='${targetId}']`; // noteに限らず汎用的に検索しても良い
    const result = this.document.evaluate(
      xpath,
      this.document,
      this.nsResolver.bind(this), // bindが必要
      XPathResult.FIRST_ORDERED_NODE_TYPE,
      null,
    );
    return result.singleNodeValue as Element | null;
  }

  /**
   * 指定されたnoteのidに適用されている調号を取得する。
   */
  getKeyAt(_targetId: string): Key | null {
    // Simplified implementation: Look for the first scoreDef with key information
    // In a full implementation, we should traverse up to find the staff,
    // then find the corresponding staffDef, or look for changes (keySig) in the layer.

    const scoreDef = this.document.querySelector("scoreDef");
    if (!scoreDef) return null;

    // Try to find global key info
    const keyPname = scoreDef.getAttribute("key.pname");
    const keyMode = scoreDef.getAttribute("key.mode");

    if (keyPname) {
      // Map pname to tonic number (0=C, 1=D... ?) No, our Key definition uses 'tonic: number'.
      // Step 1 defines NoteNameEnum C=0, D=1...
      // Let's use that.
      const map: Record<string, number> = {
        c: 0,
        d: 1,
        e: 2,
        f: 3,
        g: 4,
        a: 5,
        b: 6,
      };
      const tonic = map[keyPname.toLowerCase()] ?? 0;
      const mode = keyMode === "minor" ? "minor" : "major";
      return { tonic, mode };
    }

    return { tonic: 0, mode: "major" }; // Default C Major
  }

  // --- Mutations ---

  // Review Comment:
  // これらのメソッドは DOM を直接書き換えますが、
  // Undo/Redo を実現するために「変更前の状態」や「逆操作のための情報」を
  // 戻り値として返すと ActiveDocument 側で履歴管理がしやすくなります。

  updateNoteUp(targetId: string): IEditAction | null {
    const element = this.getNoteElementById(targetId);
    if (!element || element.tagName !== "note") return null;

    const pname = element.getAttribute("pname");
    const oct = element.getAttribute("oct");

    if (!pname || !oct) return null;

    const currentPitch = new Pitch(parseInt(oct, 10), pname);
    const nextPitch = currentPitch.stepUp();

    const oldXml = element.outerHTML;

    // Create a clone to manipulate so we don't change the main doc yet (or do we? Action applies it)
    // Actually, MeiElementReplaceAction expects the new XML fragment.
    // We can clone the element, update attributes, and serialize.
    const newElement = element.cloneNode(true) as Element;
    newElement.setAttribute("pname", nextPitch.noteName.toString());
    newElement.setAttribute("oct", nextPitch.octave.value.toString());

    return new MeiElementReplaceAction(targetId, newElement.outerHTML, oldXml);
  }

  updateNoteDown(targetId: string): IEditAction | null {
    const element = this.getNoteElementById(targetId);
    if (!element || element.tagName !== "note") return null;

    const pname = element.getAttribute("pname");
    const oct = element.getAttribute("oct");

    if (!pname || !oct) return null;

    const currentPitch = new Pitch(parseInt(oct, 10), pname);
    const prevPitch = currentPitch.stepDown();

    const oldXml = element.outerHTML;

    const newElement = element.cloneNode(true) as Element;
    newElement.setAttribute("pname", prevPitch.noteName.toString());
    newElement.setAttribute("oct", prevPitch.octave.value.toString());

    return new MeiElementReplaceAction(targetId, newElement.outerHTML, oldXml);
  }
}
