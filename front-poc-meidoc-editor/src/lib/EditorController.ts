import type { Transaction as CTransaction } from "@codemirror/state";
import type { EditorView as CEditorView, ViewUpdate } from "@codemirror/view";
import { exampleSetup } from "prosemirror-example-setup";
import { DOMSerializer, type Node as PMNode } from "prosemirror-model";
import {
  EditorState,
  type Transaction as PTransaction,
} from "prosemirror-state";
import { ReplaceStep } from "prosemirror-transform";
import type { EditorView as PEditorView } from "prosemirror-view";
import {
  defaultSyntaxDefinition,
  ResilientSyntaxTree,
} from "./ResilientSyntaxTree";
import { mySchema } from "./schema";
import type { XHTML5MEIDocument } from "./XHTML5MEIDocument";

type Listener = () => void;

export type EditorLockState = "Idle" | "ProcessingFromCM" | "ProcessingFromPM";

export interface LogEntry {
  timestamp: number;
  source: "CM" | "PM" | "System";
  type: "Receive" | "Apply" | "Map" | "Lock" | "Unlock";
  details: string;
  relatedAction?: string;
}

export class EditorController {
  document: XHTML5MEIDocument;
  rst: ResilientSyntaxTree;

  // State Management
  lockState: EditorLockState = "Idle";
  logs: LogEntry[] = [];
  private readonly maxLogs = 100;

  // Views
  codeMirrorView: CEditorView | null = null;
  proseMirrorView: PEditorView | null = null;

  // Transaction History
  codeMirrorTransactions: CTransaction[] = [];
  proseMirrorTransactions: PTransaction[] = [];

  // Serialized Content
  proseMirrorXML: string = "";

  version = 0;

  private listeners: Set<Listener> = new Set();

  constructor(document: XHTML5MEIDocument) {
    this.document = document;
    this.rst = ResilientSyntaxTree.parse(
      document.rawContent,
      defaultSyntaxDefinition,
    );
    this.log("System", "Apply", "EditorController initialized with RST");
  }

  log(source: LogEntry["source"], type: LogEntry["type"], details: string) {
    const entry: LogEntry = {
      timestamp: Date.now(),
      source,
      type,
      details,
    };
    this.logs.unshift(entry);
    if (this.logs.length > this.maxLogs) {
      this.logs.pop();
    }
    this.notify();
  }

  setLock(state: EditorLockState) {
    if (this.lockState !== state) {
      this.log(
        "System",
        state === "Idle" ? "Unlock" : "Lock",
        `State: ${state}`,
      );
      this.lockState = state;
      this.notify();
    }
  }

  canEdit(source: "CM" | "PM"): boolean {
    if (this.lockState === "Idle") return true;
    if (this.lockState === "ProcessingFromCM" && source === "CM") return true;
    if (this.lockState === "ProcessingFromPM" && source === "PM") return true;
    return false;
  }

  /**
   * Subscribe to changes in the controller state.
   * Returns a cleanup function.
   */
  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  /**
   * Notifies subscribers (React components) to re-render.
   * Call this whenever any member variables are updated.
   */
  private notify() {
    this.version++;
    for (const listener of this.listeners) {
      listener();
    }
  }

  setCodeMirrorView(view: CEditorView | null) {
    this.codeMirrorView = view;
  }

  setProseMirrorView(view: PEditorView | null) {
    this.proseMirrorView = view;
  }

  handleCodeMirrorUpdate = (update: ViewUpdate) => {
    if (update.docChanged) {
      if (!this.canEdit("CM")) {
        this.log("CM", "Receive", "Change blocked due to lock");
        return;
      }

      this.setLock("ProcessingFromCM");
      this.log("CM", "Receive", `Changes detected: ${update.changes.desc}`);

      this.codeMirrorTransactions.push(...update.transactions);

      try {
        const changes: { from: number; to: number; insert: string }[] = [];
        update.changes.iterChanges((fromA, toA, _fromB, _toB, inserted) => {
          changes.push({
            from: fromA,
            to: toA,
            insert: inserted.toString(),
          });
        });

        this.rst.edit(changes);
        this.document.rawContent = this.rst.toString();
        this.log("CM", "Apply", "RST updated incrementally");

        this.syncProseMirrorFromRST();
      } catch (e) {
        this.log("System", "Apply", `Error during RST update: ${e}`);
        console.error(e);
      } finally {
        this.setLock("Idle");
      }
    }
  };

  syncProseMirrorFromRST() {
    if (!this.proseMirrorView) return;

    this.log("PM", "Apply", "Syncing ProseMirror from RST");
    const newDoc = this.rst.toProseMirrorDoc(mySchema);

    const { state } = this.proseMirrorView;
    const tr = state.tr.replaceWith(0, state.doc.content.size, newDoc);

    // TODO: Map selection from CM to PM
    // For now, we just replace the content.

    this.proseMirrorView.dispatch(tr);
    this.updateProseMirrorXML(this.proseMirrorView.state);
  }

  handleProseMirrorTransaction = (tr: PTransaction, newState: EditorState) => {
    if (tr.docChanged) {
      if (!this.canEdit("PM")) {
        this.log("PM", "Receive", "Change blocked due to lock");
        return;
      }
      this.setLock("ProcessingFromPM");

      let currentPMDoc = tr.before;

      try {
        for (const step of tr.steps) {
          if (step instanceof ReplaceStep) {
            const from = step.from;
            const to = step.to;
            const slice = step.slice;

            let content = slice.content;
            let openStart = slice.openStart;
            while (openStart > 0 && content.childCount > 0) {
              const firstChild = content.firstChild;
              if (firstChild) {
                content = firstChild.content;
              }
              openStart--;
            }

            // Strip wrapper block if content is simple text wrapped in a block
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
              this.rst.edit([{ from: rstFrom, to: rstTo, insert: insertText }]);
              this.log(
                "PM",
                "Apply",
                `Mapped PM change ${from}-${to} to RST ${rstFrom}-${rstTo}`,
              );
            } else {
              this.log(
                "System",
                "Apply",
                `Failed to map PM position ${from}-${to} to RST`,
              );
            }

            const result = step.apply(currentPMDoc);
            if (result.doc) {
              currentPMDoc = result.doc;
            }
          }
        }

        this.document.rawContent = this.rst.toString();
        this.syncCodeMirrorFromRST();
      } catch (e) {
        this.log("System", "Apply", `Error mapping PM to RST: ${e}`);
        console.error(e);
      } finally {
        this.setLock("Idle");
      }

      this.proseMirrorTransactions.push(tr);
      this.updateProseMirrorXML(newState);
      this.notify();
    }
  };

  syncCodeMirrorFromRST() {
    if (!this.codeMirrorView) return;
    const newText = this.rst.toString();
    const currentText = this.codeMirrorView.state.doc.toString();
    if (newText === currentText) return;

    this.log("CM", "Apply", "Syncing CodeMirror from RST");

    const tr = this.codeMirrorView.state.update({
      changes: { from: 0, to: currentText.length, insert: newText },
    });

    this.codeMirrorView.dispatch(tr);
  }

  private mapPMToRSTPosition(doc: PMNode, pos: number): number {
    const $pos = doc.resolve(pos);
    const parent = $pos.parent;

    const rstId = parent.attrs.id;
    const targetOffset = $pos.parentOffset;

    // Check if parent is the doc itself or lacks ID
    if (!rstId || parent.type.name === "doc") {
      // Try to map to node boundary
      const nodeBefore = $pos.nodeBefore;
      if (nodeBefore?.attrs.id) {
        const beforeId = nodeBefore.attrs.id;
        const rstNode = this.rst.findNodeById(beforeId);
        if (rstNode) {
          // Map to inside the end of nodeBefore
          return rstNode.to - rstNode.closingContent.length;
        }
      }

      const nodeAfter = $pos.nodeAfter;
      if (nodeAfter?.attrs.id) {
        const afterId = nodeAfter.attrs.id;
        const rstNode = this.rst.findNodeById(afterId);
        if (rstNode) {
          // Map to inside the start of nodeAfter
          return rstNode.from + rstNode.openingContent.length;
        }
      }
      return -1;
    }

    const rstNode = this.rst.findNodeById(rstId);
    if (!rstNode) {
      return -1;
    }

    let rstAbsolutePos = rstNode.from + rstNode.openingContent.length;
    let currentPMOffset = 0;

    // Iterate over PM children to find the position relative to RST
    let rstChildIndex = 0;

    for (let i = 0; i < parent.childCount; i++) {
      const pmChild = parent.child(i);
      const pmChildSize = pmChild.nodeSize;

      if (currentPMOffset + pmChildSize >= targetOffset) {
        // Target is inside this child or at its end
        const offsetInChild = targetOffset - currentPMOffset;

        if (offsetInChild === pmChildSize) {
          // At the end of this child.
          if (pmChild.attrs.id) {
            const childId = pmChild.attrs.id;
            const childRST = this.rst.findNodeById(childId);
            if (childRST) {
              // Map to inside end (before closing tag)
              return childRST.to - childRST.closingContent.length;
            }
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
              // PM thinks it's text, RST thinks it's not. Skip RST node.
              rstAbsolutePos += rstChild.length;
              rstChildIndex++;
            }
          }
          // If we run out of RST nodes but still have offset (should not happen if synced)
          return rstAbsolutePos;
        } else {
          // Element Node
          // If offsetInChild is 0, it's before this element.
          if (offsetInChild === 0) return rstAbsolutePos;

          // If we need to go inside the element, we should recurse,
          // but mapPMToRSTPosition takes doc and absolute pos.
          // However, ReplaceStep usually gives positions at text level or node boundaries.
          // If we are here, it means the position is "inside" an atom or "around" a node.
          // For now, support boundaries.

          // Find matching RST node
          const targetId = pmChild.attrs.id;
          while (rstChildIndex < rstNode.children.length) {
            const rstChild = rstNode.children[rstChildIndex];
            if (rstChild.id === targetId) {
              // Found it.
              // If offset is at end of this node?
              // For simplified logic, assume we only care about "before" or "after" this block
              // if we are iterating children of `parent`.
              // But wait, $pos.parent IS `parent`. So pos is directly inside `parent`.
              // So we are looking for a simplified offset.
              return rstAbsolutePos + offsetInChild; // Approximate! RST element size != PM element size
            }
            rstAbsolutePos += rstChild.length;
            rstChildIndex++;
          }
        }
        break;
      }
      currentPMOffset += pmChildSize;

      // Advance RST index corresponding to pmChild
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
          if (rstChild.id === targetId) {
            break;
          }
        }
      }
    }

    return rstAbsolutePos;
  }

  createProseMirrorState = (): EditorState => {
    const doc = this.rst.toProseMirrorDoc(mySchema);

    const state = EditorState.create({
      doc,
      schema: mySchema,
      plugins: exampleSetup({ schema: mySchema }),
    });

    this.updateProseMirrorXML(state);
    this.notify();

    return state;
  };

  private _serializer = DOMSerializer.fromSchema(mySchema);
  updateProseMirrorXML = (state: EditorState) => {
    // Serialize to XML
    const fragment = this._serializer.serializeFragment(state.doc.content);

    const tempDiv = window.document.createElement("div");
    tempDiv.appendChild(fragment);
    this.proseMirrorXML = tempDiv.innerHTML;
  };
}
