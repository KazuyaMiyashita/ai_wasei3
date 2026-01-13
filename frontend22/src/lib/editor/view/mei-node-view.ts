import type { Node as PMNode } from "prosemirror-model";
import type { NodeView } from "prosemirror-view";
import verovio from "verovio";

export class MeiNodeView implements NodeView {
  dom: HTMLElement;
  private container: HTMLElement;
  private node: PMNode;
  // biome-ignore lint/suspicious/noExplicitAny: Verovio toolkit is complex
  private tk: any; // Verovio toolkit

  constructor(node: PMNode) {
    this.node = node;
    this.dom = document.createElement("div");
    this.dom.className = "mei-node-view";

    this.container = document.createElement("div");
    this.container.className = "mei-svg-container";
    this.dom.appendChild(this.container);

    this.render();
  }

  async render() {
    if (!this.tk) {
      // In a real app, use a singleton or async loader
      this.tk = new verovio.toolkit();
    }
    try {
      const options = {
        scale: 30,
        adjustPageHeight: true,
        pageWidth: this.dom.clientWidth || 800,
      };
      this.tk.setOptions(options);

      // Ensure content has namespace if needed
      let content = this.node.attrs.content;
      if (!content || !content.trim()) return;

      if (!content.includes("xmlns")) {
        content = `<mei xmlns="http://www.music-encoding.org/ns/mei" meiversion="5.1">${content}</mei>`;
      }

      this.tk.loadData(content);
      const svg = this.tk.renderToSVG(1);
      this.container.innerHTML = svg;
    } catch (e) {
      this.container.innerHTML = `<div class="mei-error">Failed to render MEI: ${e}</div>`;
    }
  }

  update(node: PMNode) {
    if (node.type !== this.node.type) return false;

    // If content changed, re-render
    if (node.attrs.content !== this.node.attrs.content) {
      this.node = node;
      this.render();
    }

    return true;
  }

  selectNode() {
    this.dom.classList.add("ProseMirror-selectednode");
  }

  deselectNode() {
    this.dom.classList.remove("ProseMirror-selectednode");
  }

  stopEvent() {
    return true;
  }
  ignoreMutation() {
    return true;
  }
  destroy() {
    if (this.tk) {
      // cleanup
    }
  }
}
