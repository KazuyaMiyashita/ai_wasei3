import type verovio from "verovio";

export function transformVerovioSVG(svg: string): string {
  const parser = new DOMParser();
  const svgDoc = parser.parseFromString(svg, "image/svg+xml");
  const svgEl = svgDoc.documentElement;

  if (svgEl.nodeName !== "parsererror") {
    const viewBox = svgEl.getAttribute("viewBox");
    if (viewBox) {
      const [, , w, h] = viewBox.split(/\s+|,/).map(Number);
      if (!Number.isNaN(w) && !Number.isNaN(h)) {
        svgEl.style.width = `${w}px`;
        svgEl.style.height = `${h}px`;
        svgEl.style.maxWidth = "none";
      }
    }
    const serializer = new XMLSerializer();
    return serializer.serializeToString(svgDoc);
  }
  return svg;
}

export function transformXhtmlMei(
  content: string,
  toolkit: verovio.toolkit,
): string {
  const parser = new DOMParser();
  const doc = parser.parseFromString(content, "application/xml");

  if (doc.querySelector("parsererror")) {
    console.warn("XHTML+MEI parse error");
    return content; // Return original if parsing failed
  }

  let meis = doc.getElementsByTagNameNS(
    "http://www.music-encoding.org/ns/mei",
    "mei",
  );

  // Fallback: try finding by tag name without namespace if none found
  if (meis.length === 0) {
    meis = doc.getElementsByTagName("mei");
  }

  const meiArray = Array.from(meis);

  for (let i = 0; i < meiArray.length; i++) {
    const meiNode = meiArray[i];
    const serializer = new XMLSerializer();
    const meiString = serializer.serializeToString(meiNode);

    try {
      toolkit.loadData(meiString);
      toolkit.redoLayout();
      const svg = toolkit.renderToSVG(1);

      const placeholder = doc.createElement("div");
      placeholder.setAttribute("data-mei-index", i.toString());
      placeholder.setAttribute("class", "score-rendering");
      meiNode.parentNode?.replaceChild(placeholder, meiNode);

      const svgDoc = parser.parseFromString(svg, "image/svg+xml");
      if (svgDoc.documentElement.nodeName !== "parsererror") {
        const importedSvg = doc.importNode(svgDoc.documentElement, true);
        placeholder.parentNode?.replaceChild(importedSvg, placeholder);
      }
    } catch (e) {
      console.error(e);
    }
  }

  const serializer = new XMLSerializer();
  return serializer.serializeToString(doc);
}
