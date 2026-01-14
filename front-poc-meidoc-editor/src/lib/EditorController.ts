import type { Transaction as CTransaction } from "@codemirror/state";
import type { EditorView as CEditorView, ViewUpdate } from "@codemirror/view";
import { exampleSetup } from "prosemirror-example-setup";
import { DOMParser, DOMSerializer } from "prosemirror-model";
import {
  EditorState,
  type Transaction as PTransaction,
} from "prosemirror-state";
import type { EditorView as PEditorView } from "prosemirror-view";
import { mySchema } from "./schema";
import type { XHTML5MEIDocument } from "./XHTML5MEIDocument";

type Listener = () => void;

export class EditorController {
  document: XHTML5MEIDocument;

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
      this.codeMirrorTransactions.push(...update.transactions);

      const newContent = update.state.doc.toString();
      this.document.rawContent = newContent;

      this.notify();
    }
  };

  handleProseMirrorTransaction = (tr: PTransaction, newState: EditorState) => {
    if (tr.docChanged) {
      this.proseMirrorTransactions.push(tr);

      // Serialize to XML
      const serializer = DOMSerializer.fromSchema(mySchema);
      const fragment = serializer.serializeFragment(newState.doc.content);

      const tempDiv = window.document.createElement("div");
      tempDiv.appendChild(fragment);
      this.proseMirrorXML = tempDiv.innerHTML;

      this.notify();
    }
  };

  createProseMirrorState = (): EditorState => {
    const domParser = new window.DOMParser();
    const xmlDoc = domParser.parseFromString(
      this.document.rawContent,
      "text/html",
    );
    const bodyContent = xmlDoc.body || xmlDoc.documentElement;

    const docNode = DOMParser.fromSchema(mySchema).parse(bodyContent);

    return EditorState.create({
      doc: docNode,
      schema: mySchema,
      plugins: exampleSetup({ schema: mySchema }),
    });
  };
}
