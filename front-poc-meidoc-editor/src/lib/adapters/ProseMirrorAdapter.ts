import type { Node as PMNode } from "prosemirror-model";
import { DOMSerializer } from "prosemirror-model";
import type { EditorState, Transaction } from "prosemirror-state";
import { ReplaceStep } from "prosemirror-transform";
import type { EditorView } from "prosemirror-view";
import type {
  ResilientNode,
  ResilientSyntaxTree,
  RSTChange,
} from "../ResilientSyntaxTree";
import { mySchema } from "../schema";

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

    let currentPMDoc = tr.before;

    // Iterate steps to find changes
    for (const step of tr.steps) {
      if (step instanceof ReplaceStep) {
        const from = step.from;
        const to = step.to;
        const slice = step.slice;

        // Helper to extract text from Slice (simplified)
        // In reality, we might need to serialize complex slices or handle structure
        let content = slice.content;
        let openStart = slice.openStart;
        while (openStart > 0 && content.childCount > 0) {
          const firstChild = content.firstChild;
          if (firstChild) {
            content = firstChild.content;
          }
          openStart--;
        }

        const firstChild = content.firstChild;
        if (
          content.childCount === 1 &&
          firstChild?.isBlock &&
          firstChild.content.size > 0
        ) {
          content = firstChild.content;
        }

        let insertText = "";
        if (content.size > 0) {
          const fragment = this._serializer.serializeFragment(content);
          const tmp = window.document.createElement("div");
          tmp.appendChild(fragment);
          insertText = tmp.innerHTML;
        }

        const rstFrom = this.mapPMToRSTPosition(currentPMDoc, from);
        const rstTo = this.mapPMToRSTPosition(currentPMDoc, to);

        if (rstFrom !== -1 && rstTo !== -1) {
          changes.push({ rstFrom, rstTo, insertText });
        }

        const result = step.apply(currentPMDoc);
        if (result.doc) currentPMDoc = result.doc;
      }
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
        // (This can happen for newly created nodes during CM edits)
        const bodyNode = this.rst.getBodyNode();
        const newDoc = this.rst.toProseMirrorDoc(
          mySchema,
          bodyNode || undefined,
        );
        tr = state.tr.replaceWith(0, state.doc.content.size, newDoc);
        break; // Stop loop and use full replacement
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

  // Re-used logic from EditorController (will be refactored)
  private mapPMToRSTPosition(doc: PMNode, pos: number): number {
    const $pos = doc.resolve(pos);
    const parent = $pos.parent;
    const rstId = parent.attrs.id;

    if (!rstId) {
      if (parent.type.name === "doc") {
        const bodyNode = this.rst.getBodyNode();
        if (bodyNode) {
          const bodyStart = bodyNode.from + bodyNode.openingContent.length;
          return this.mapChildrenOffset(bodyNode, parent, pos, bodyStart);
        }
      }
      return -1;
    }

    const rstNode = this.rst.findNodeById(rstId);
    if (!rstNode) return -1;

    const rstContentStart = rstNode.from + rstNode.openingContent.length;
    const targetOffset = $pos.parentOffset;

    return this.mapChildrenOffset(
      rstNode,
      parent,
      targetOffset,
      rstContentStart,
    );
  }

  private mapChildrenOffset(
    rstNode: ResilientNode,
    pmParent: PMNode,
    targetOffset: number,
    rstStartPos: number,
  ): number {
    // (Logic copied from EditorController, will be cleaned up in later steps)
    // This duplicates the logic to ensure Adapter is self-contained before removing from Controller
    let currentPMOffset = 0;
    let rstChildIndex = 0;
    let rstAbsolutePos = rstStartPos;

    for (let i = 0; i < pmParent.childCount; i++) {
      const pmChild = pmParent.child(i);
      const pmChildSize = pmChild.nodeSize;

      if (currentPMOffset + pmChildSize >= targetOffset) {
        const offsetInChild = targetOffset - currentPMOffset;
        if (offsetInChild === 0) return rstAbsolutePos;

        if (pmChild.attrs.id) {
          const childRST = this.rst.findNodeById(pmChild.attrs.id);
          if (childRST) {
            if (offsetInChild === pmChildSize) return childRST.to;
          }
        }

        if (pmChild.isText) {
          let remainingTextOffset = offsetInChild;
          while (rstChildIndex < rstNode.children.length) {
            const rstChild = rstNode.children[rstChildIndex];
            if (rstChild.type === "Text") {
              const len = rstChild.textContent.length;
              if (remainingTextOffset <= len) {
                return rstAbsolutePos + remainingTextOffset;
              }
              remainingTextOffset -= len;
              rstAbsolutePos += rstChild.length;
              rstChildIndex++;
            } else {
              rstAbsolutePos += rstChild.length;
              rstChildIndex++;
            }
          }
          return rstAbsolutePos;
        }
        return rstAbsolutePos;
      }

      currentPMOffset += pmChildSize;

      if (pmChild.isText) {
        let remainingLen = pmChild.text?.length ?? 0;
        while (rstChildIndex < rstNode.children.length && remainingLen > 0) {
          const rstChild = rstNode.children[rstChildIndex];
          if (rstChild.type === "Text") {
            const len = rstChild.textContent.length;
            if (len <= remainingLen) {
              remainingLen -= len;
              rstAbsolutePos += rstChild.length;
              rstChildIndex++;
            } else {
              break;
            }
          } else {
            rstAbsolutePos += rstChild.length;
            rstChildIndex++;
          }
        }
      } else {
        const targetId = pmChild.attrs.id;
        while (rstChildIndex < rstNode.children.length) {
          const rstChild = rstNode.children[rstChildIndex];
          rstAbsolutePos += rstChild.length;
          rstChildIndex++;
          if (rstChild.id === targetId) break;
        }
      }
    }
    return rstAbsolutePos;
  }
}
