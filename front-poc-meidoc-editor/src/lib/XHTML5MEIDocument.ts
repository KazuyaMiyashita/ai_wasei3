export /** XHTML5の文章中にMEIのXMLを含むドキュメント。
 */
class XHTML5MEIDocument {
  // 正常な状態であれば内容はXMLであるためXMLDocument型として扱いたいところだが、
  // シンタックスが壊れた状態で読み込み・保存を可能にするため、string型で扱う
  rawContent: string;

  constructor(rawContent: string) {
    this.rawContent = rawContent;
  }

  isValidXML(): boolean {
    try {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(this.rawContent, "text/xml");
      const parseErrors = xmlDoc.getElementsByTagName("parsererror");
      return parseErrors.length === 0;
    } catch (_e) {
      return false;
    }
  }

  toXMLDocument(): XMLDocument | null {
    try {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(this.rawContent, "text/xml");
      const parseErrors = xmlDoc.getElementsByTagName("parsererror");
      return parseErrors.length === 0 ? xmlDoc : null;
    } catch (_e) {
      return null;
    }
  }
}
