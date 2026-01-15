import type { Transaction as CTransaction } from "@codemirror/state";
import type { EditorView as CEditorView, ViewUpdate } from "@codemirror/view";
import { baseKeymap } from "prosemirror-commands";
import { dropCursor } from "prosemirror-dropcursor";
import { buildMenuItems } from "prosemirror-example-setup";
import { gapCursor } from "prosemirror-gapcursor";
import { history, redo, undo } from "prosemirror-history";
import { keymap } from "prosemirror-keymap";
import { menuBar } from "prosemirror-menu";
import { DOMSerializer } from "prosemirror-model";
import {
  EditorState,
  type Transaction as PTransaction,
} from "prosemirror-state";
import type { EditorView as PEditorView } from "prosemirror-view";
import { CodeMirrorAdapter } from "./adapters/CodeMirrorAdapter";
import { ProseMirrorAdapter } from "./adapters/ProseMirrorAdapter";
import {
  defaultSyntaxDefinition,
  ResilientSyntaxTree,
  type RSTChange,
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

  // Adapters
  cmAdapter: CodeMirrorAdapter;
  pmAdapter: ProseMirrorAdapter;

  // State Management
  lockState: EditorLockState = "Idle";
  logs: LogEntry[] = [];
  private readonly maxLogs = 100;

  // Transaction History (Restored for DebugView compatibility)
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
    this.cmAdapter = new CodeMirrorAdapter();
    this.pmAdapter = new ProseMirrorAdapter(this.rst);

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

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify() {
    this.version++;
    for (const listener of this.listeners) {
      listener();
    }
  }

  setCodeMirrorView(view: CEditorView | null) {
    this.cmAdapter.setView(view);
  }

  setProseMirrorView(view: PEditorView | null) {
    this.pmAdapter.setView(view);
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
        const changes = this.cmAdapter.getChangesFromUpdate(update);

        // Apply to RST
        const rstChanges = this.rst.edit(changes);
        this.document.rawContent = this.rst.toString();
        this.log("CM", "Apply", "RST updated incrementally");

        // Sync to PM
        this.syncProseMirrorFromRST(rstChanges);
      } catch (e) {
        this.log("System", "Apply", `Error during RST update: ${e}`);
        console.error(e);
      } finally {
        this.setLock("Idle");
      }
    }
  };

  syncProseMirrorFromRST(changes?: RSTChange[]) {
    this.log(
      "PM",
      "Apply",
      `Syncing ProseMirror from RST (${changes ? "Incremental" : "Full"})`,
    );

    if (!this.pmAdapter.view) return;

    if (changes) {
      this.pmAdapter.applyChanges(changes);
    } else {
      const bodyNode = this.rst.getBodyNode();
      const newDoc = this.rst.toProseMirrorDoc(mySchema, bodyNode || undefined);
      const { state } = this.pmAdapter.view;
      const tr = state.tr.replaceWith(0, state.doc.content.size, newDoc);
      this.pmAdapter.view.dispatch(tr);
    }

    this.updateProseMirrorXML(this.pmAdapter.view.state);
  }
  handleProseMirrorTransaction = (tr: PTransaction, newState: EditorState) => {
    if (tr.docChanged) {
      if (!this.canEdit("PM")) {
        this.log("PM", "Receive", "Change blocked due to lock");
        return;
      }
      this.setLock("ProcessingFromPM");

      const allRstChanges: RSTChange[] = [];

      try {
        const pmChanges = this.pmAdapter.getChangesFromTransaction(
          tr,
          newState,
        );

        // Map PM changes to RST edits
        for (const change of pmChanges) {
          const rstChanges = this.rst.edit([
            {
              from: change.rstFrom,
              to: change.rstTo,
              insert: change.insertText,
            },
          ]);
          allRstChanges.push(...rstChanges);
          this.log(
            "PM",
            "Apply",
            `Mapped PM change to RST ${change.rstFrom}-${change.rstTo}`,
          );
        }

        this.document.rawContent = this.rst.toString();

        // Sync to CM
        this.cmAdapter.applyChanges(
          allRstChanges.map((c) => ({
            from: c.from,
            to: c.to,
            insert: c.insert,
          })),
        );
      } catch (e) {
        this.log("System", "Apply", `Error mapping PM to RST: ${e}`);
        console.error(e);
      } finally {
        this.setLock("Idle");
      }
    }

    // Always track transactions even if not docChanged (e.g. selection)
    this.proseMirrorTransactions.push(tr);
    this.updateProseMirrorXML(newState);
    this.notify();
  };

  createProseMirrorState = (): EditorState => {
    const bodyNode = this.rst.getBodyNode();
    const doc = this.rst.toProseMirrorDoc(mySchema, bodyNode || undefined);

    const state = EditorState.create({
      doc,
      schema: mySchema,
      plugins: [
        history(),
        keymap({ "Mod-z": undo, "Mod-y": redo }),
        keymap(baseKeymap),
        dropCursor(),
        gapCursor(),
        menuBar({
          content: buildMenuItems(mySchema).fullMenu,
        }),
      ],
    });

    this.updateProseMirrorXML(state);
    this.notify();

    return state;
  };

  private _serializer = DOMSerializer.fromSchema(mySchema);
  updateProseMirrorXML = (state: EditorState) => {
    const fragment = this._serializer.serializeFragment(state.doc.content);
    const tempDiv = window.document.createElement("div");
    tempDiv.appendChild(fragment);
    this.proseMirrorXML = tempDiv.innerHTML;
  };
}
