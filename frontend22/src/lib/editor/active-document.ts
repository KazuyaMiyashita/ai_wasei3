import type { Document } from "../model/documents/document";
import type { DocumentId, DocumentOrigin } from "../model/documents/types";
import type { MEI } from "../model/music/mei";
import type { ISubscribable } from "../shared/subscribable";
import type { IEditAction } from "./actions";
import {
  type ContentAdapter,
  MeiContentAdapter,
  type UpdateHint,
  XhtmlMeiContentAdapter,
} from "./content-adapters";

type HistoryEntry = {
  forward: IEditAction;
  inverse: IEditAction;
};

export type ActiveDocumentState = {
  // content: string; // Removed to prevent payload bloat
  isDirty: boolean;
  canUndo: boolean;
  canRedo: boolean;
  currentMEIIndex: number | null;
  lastUpdateHint: UpdateHint;
};

type ActiveDocumentListener = (state: ActiveDocumentState) => void;

export class ActiveDocument implements ISubscribable<ActiveDocumentState> {
  originalDocument: Document;
  public adapter: ContentAdapter;
  currentMEIIndex: number | null = 0;

  private undoStack: HistoryEntry[] = [];
  private redoStack: HistoryEntry[] = [];
  private listeners: Set<ActiveDocumentListener> = new Set();
  private isDirty = false;
  private lastUpdateHint: UpdateHint = { type: "none" };

  constructor(document: Document) {
    this.originalDocument = document;

    if (document.type === "mei") {
      this.adapter = new MeiContentAdapter();
    } else {
      this.adapter = new XhtmlMeiContentAdapter();
    }
    this.adapter.initialize(document.content);
  }

  // --- ISubscribable ---

  subscribe(listener: ActiveDocumentListener): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => this.listeners.delete(listener);
  }

  private notifyObservers(): void {
    const state = this.getState();
    for (const listener of this.listeners) {
      listener(state);
    }
  }

  getState(): ActiveDocumentState {
    return {
      isDirty: this.isDirty,
      canUndo: this.undoStack.length > 0,
      canRedo: this.redoStack.length > 0,
      currentMEIIndex: this.currentMEIIndex,
      lastUpdateHint: this.lastUpdateHint,
    };
  }

  // --- Accessors ---

  get id(): DocumentId {
    return this.originalDocument.id;
  }

  origin(): DocumentOrigin {
    return this.originalDocument.origin;
  }

  markAsSaved() {
    this.isDirty = false;
    this.notifyObservers();
  }

  toDocument(): Document {
    return {
      ...this.originalDocument,
      content: this.adapter.content,
    };
  }

  getContent(): string {
    return this.adapter.content;
  }

  currentMEI(): MEI | null {
    if (this.currentMEIIndex === null) return null;
    return this.adapter.getMeiInstance(this.currentMEIIndex);
  }

  // --- Edit Operations ---

  edit(action: IEditAction): void {
    const contentBefore = this.adapter.content;
    const inverseAction = action.createInverse(contentBefore);

    const result = this.adapter.apply(action);
    this.lastUpdateHint = result.hint;

    this.undoStack.push({ forward: action, inverse: inverseAction });
    this.redoStack = [];
    this.isDirty = true;

    this.notifyObservers();
  }

  undo(): void {
    const entry = this.undoStack.pop();
    if (!entry) return;

    const result = this.adapter.apply(entry.inverse);
    this.lastUpdateHint = result.hint;

    this.redoStack.push(entry);
    this.isDirty = true; // Simplified dirty check

    this.notifyObservers();
  }

  redo(): void {
    const entry = this.redoStack.pop();
    if (!entry) return;

    const result = this.adapter.apply(entry.forward);
    this.lastUpdateHint = result.hint;

    this.undoStack.push(entry);
    this.isDirty = true;

    this.notifyObservers();
  }

  // Semantic Edit Helper
  async richEdit(
    operationName: string,
    // biome-ignore lint/suspicious/noExplicitAny: Params depend on op
    params: any,
  ): Promise<void> {
    const mei = this.currentMEI();
    if (!mei) return;

    let action: IEditAction | null = null;

    if (operationName === "noteUp" && params?.id) {
      action = mei.updateNoteUp(params.id);
    } else if (operationName === "noteDown" && params?.id) {
      action = mei.updateNoteDown(params.id);
    }

    if (action) {
      this.edit(action);
    }
  }
}
