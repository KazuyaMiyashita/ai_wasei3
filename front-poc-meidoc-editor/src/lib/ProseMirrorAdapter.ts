import type { Node as PMNode } from "prosemirror-model";
import { DOMSerializer } from "prosemirror-model";
import type { EditorState, Transaction } from "prosemirror-state";
import { ReplaceStep } from "prosemirror-transform";
import type { EditorView } from "prosemirror-view";
import type {
  // ResilientNode,
  ResilientSyntaxTree,
  RSTChange,
} from "./ResilientSyntaxTree";
import { mySchema } from "./schema";

export interface PMChange {
  rstFrom: number;
  rstTo: number;
  insertText: string;
}

export class ProseMirrorAdapter {
  view: EditorView | null = null;
  private _serializer = DOMSerializer.fromSchema(mySchema);

  constructor(private rst: ResilientSyntaxTree) {}

  setView(view: EditorView | null) {
    this.view = view;
  }

  getChangesFromTransaction(
    tr: Transaction,
    _oldState: EditorState,
  ): PMChange[] {
    const changes: PMChange[] = [];
    if (!tr.docChanged) return changes;
    const serializer = this._serializer;
    let currentDoc = tr.before;
    const affectedNodes = new Map<string, PMNode>();
    // Iterate through steps and find all parent nodes with IDs that were affected
    for (const step of tr.steps) {
      if (step instanceof ReplaceStep) {
        const docBefore = currentDoc;
        const result = step.apply(docBefore);
        currentDoc = result.doc || docBefore;
        const from = step.from;
        const to = step.to;

        docBefore.nodesBetween(from, to, (node, pos) => {
          if (node.attrs.id) {
            // Find the corresponding node in the *final* doc state
            const mappedPos = tr.mapping.map(pos);
            const nodeAfter = tr.doc.nodeAt(mappedPos);
            if (nodeAfter && !affectedNodes.has(node.attrs.id)) {
              affectedNodes.set(node.attrs.id, nodeAfter);
            }
          }
        });
      } else {
        throw new Error(`not implemented for ${typeof step}`);
      }
    }

    // Generate changes for each unique affected node
    for (const [id, nodeAfter] of affectedNodes.entries()) {
      const rstNode = this.rst.findNodeById(id);
      if (rstNode) {
        const domFragment = serializer.serializeNode(nodeAfter);
        const tmp = window.document.createElement("div");
        tmp.appendChild(domFragment);
        const insertText = tmp.innerHTML;

        changes.push({
          rstFrom: rstNode.from,
          rstTo: rstNode.to,
          insertText,
        });
      }
    }

    // Handle cases where no existing node was affected (e.g. appending to doc)
    if (changes.length === 0 && tr.docChanged) {
      // Fallback to replacing the whole body/root
      const bodyNode = this.rst.getBodyNode() || this.rst.root;
      // const newDoc = this.rst.toProseMirrorDoc(mySchema, bodyNode);
      // FIXME
      // const tr_new = state.tr.replaceWith(0, state.doc.content.size, tr.doc);
      // const domFragment = serializer.serializeFragment(tr_new.doc.content);
      const tmp = window.document.createElement("div");
      // tmp.appendChild(domFragment);
      const insertText = tmp.innerHTML;
      changes.push({
        rstFrom: bodyNode.from + bodyNode.openingContent.length,
        rstTo: bodyNode.to - bodyNode.closingContent.length,
        insertText,
      });
    }

    return changes;
  }

  applyChanges(changes: RSTChange[]) {
    if (!this.view) return;
    const { state } = this.view;
    let tr = state.tr;

    const idToPos = this.getIDToPosMap(tr.doc);

    for (const change of changes) {
      const nodeId = change.affectedNodes[0];
      const rstNode = this.rst.findNodeById(nodeId);
      if (!rstNode) continue;

      const pos = idToPos.get(nodeId);
      if (pos !== undefined) {
        const mappedPos = tr.mapping.map(pos);
        const nodeInDoc = tr.doc.nodeAt(mappedPos);
        if (nodeInDoc && nodeInDoc.attrs.id === nodeId) {
          const newPM = this.rst.toProseMirrorNode(mySchema, rstNode);
          if (newPM) {
            const nodes = Array.isArray(newPM) ? newPM : [newPM];
            tr.replaceWith(mappedPos, mappedPos + nodeInDoc.nodeSize, nodes);
          }
        }
      } else {
        // Fallback: full sync if node not found
        const bodyNode = this.rst.getBodyNode();
        const newDoc = this.rst.toProseMirrorDoc(
          mySchema,
          bodyNode || undefined,
        );
        tr = state.tr.replaceWith(0, state.doc.content.size, newDoc);
        break;
      }
    }

    if (tr.docChanged) {
      this.view.dispatch(tr);
    }
  }

  private getIDToPosMap(doc: PMNode): Map<string, number> {
    const map = new Map<string, number>();
    doc.descendants((node, pos) => {
      if (node.attrs.id) {
        map.set(node.attrs.id, pos);
      }
      return true;
    });
    return map;
  }

  // private mapPMToRSTPosition(doc: PMNode, pos: number): number {
  //   const $pos = doc.resolve(pos);
  //   const parent = $pos.parent;
  //   const rstId = parent.attrs.id;

  //   if (!rstId) {
  //     // Fallback: search for ID in ancestors
  //     for (let d = $pos.depth; d >= 0; d--) {
  //       const ancestor = $pos.node(d);
  //       if (ancestor.attrs.id) {
  //         const rstNode = this.rst.findNodeById(ancestor.attrs.id);
  //         if (rstNode) {
  //           const rstContentStart =
  //             rstNode.from + rstNode.openingContent.length;
  //           const offset = $pos.before(d + 1); // Position before this depth
  //           const relOffset = pos - offset;
  //           return rstContentStart + relOffset - 1; // -1 to account for node opening
  //         }
  //       }
  //     }

  //     if (parent.type.name === "doc") {
  //       const bodyNode = this.rst.getBodyNode() || this.rst.root;
  //       const bodyStart = bodyNode.from + bodyNode.openingContent.length;
  //       return this.mapChildrenOffset(bodyNode, parent, pos, bodyStart);
  //     }
  //     return -1;
  //   }

  //   const rstNode = this.rst.findNodeById(rstId);
  //   if (!rstNode) return -1;

  //   const rstContentStart = rstNode.from + rstNode.openingContent.length;
  //   const targetOffset = $pos.parentOffset;

  //   return this.mapChildrenOffset(
  //     rstNode,
  //     parent,
  //     targetOffset,
  //     rstContentStart,
  //   );
  // }

  // private mapChildrenOffset(
  //   rstNode: ResilientNode,
  //   pmParent: PMNode,
  //   targetOffset: number,
  //   rstStartPos: number,
  // ): number {
  //   let currentPMOffset = 0;
  //   let rstChildIndex = 0;
  //   let rstAbsolutePos = rstStartPos;

  //   for (let i = 0; i < pmParent.childCount; i++) {
  //     const pmChild = pmParent.child(i);
  //     const pmChildSize = pmChild.nodeSize;

  //     if (currentPMOffset + pmChildSize >= targetOffset) {
  //       const offsetInChild = targetOffset - currentPMOffset;
  //       if (offsetInChild === 0) return rstAbsolutePos;

  //       if (pmChild.attrs.id) {
  //         const childRST = this.rst.findNodeById(pmChild.attrs.id);
  //         if (childRST) {
  //           if (offsetInChild === pmChildSize) return childRST.to;
  //         }
  //       }

  //       if (pmChild.isText) {
  //         let remainingTextOffset = offsetInChild;
  //         while (rstChildIndex < rstNode.children.length) {
  //           const rstChild = rstNode.children[rstChildIndex];
  //           if (rstChild.type === "Text") {
  //             const len = rstChild.textContent.length;
  //             if (remainingTextOffset <= len) {
  //               return rstAbsolutePos + remainingTextOffset;
  //             }
  //             remainingTextOffset -= len;
  //             rstAbsolutePos += rstChild.length;
  //             rstChildIndex++;
  //           } else {
  //             rstAbsolutePos += rstChild.length;
  //             rstChildIndex++;
  //           }
  //         }
  //         return rstAbsolutePos;
  //       }
  //       return rstAbsolutePos;
  //     }

  //     currentPMOffset += pmChildSize;

  //     if (pmChild.isText) {
  //       let remainingLen = pmChild.text?.length ?? 0;
  //       while (rstChildIndex < rstNode.children.length && remainingLen > 0) {
  //         const rstChild = rstNode.children[rstChildIndex];
  //         if (rstChild.type === "Text") {
  //           const len = rstChild.textContent.length;
  //           if (len <= remainingLen) {
  //             remainingLen -= len;
  //             rstAbsolutePos += rstChild.length;
  //             rstChildIndex++;
  //           } else {
  //             break;
  //           }
  //         } else {
  //           rstAbsolutePos += rstChild.length;
  //           rstChildIndex++;
  //         }
  //       }
  //     } else {
  //       const targetId = pmChild.attrs.id;
  //       while (rstChildIndex < rstNode.children.length) {
  //         const rstChild = rstNode.children[rstChildIndex];
  //         rstAbsolutePos += rstChild.length;
  //         rstChildIndex++;
  //         if (rstChild.id === targetId) break;
  //       }
  //     }
  //   }
  //   return rstAbsolutePos;
  // }
}
