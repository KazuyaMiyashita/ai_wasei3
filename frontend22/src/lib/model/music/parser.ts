export function parseXml(xmlString: string): Document | undefined {
  if (!xmlString) return undefined;
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlString, "application/xml");
    if (doc.querySelector("parsererror")) {
      console.error("XML Parse Error");
      return undefined;
    }
    return doc;
  } catch (e) {
    console.error("Failed to parse XML", e);
    return undefined;
  }
}
