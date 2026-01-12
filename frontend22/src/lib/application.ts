import { apiClient } from "./api/client";
import { AudioEngine } from "./audio/engine";
import { generateAudioForNote } from "./audio/generator";
import { Performer } from "./audio/performer";
import { ActiveDocument } from "./editor/active-document";
import {
  SelectionManager,
  type SelectionState,
} from "./editor/selection-manager";
import { Logger } from "./infrastructure/logger";
import {
  Resources,
  Workspace as WorkspaceStorage,
} from "./infrastructure/storage";
import {
  WorkspaceManager,
  type WorkspaceState,
} from "./infrastructure/workspace-manager";
import type { Document } from "./model/documents/document";
import type {
  DocumentId,
  DocumentOrigin,
  DocumentPath,
} from "./model/documents/types";
import type { MEI } from "./model/music/mei";
import { Subscribable } from "./shared/subscribable";
import { ScoreRenderer, type ScoreState } from "./view/score-renderer";
import {
  type ViewMode,
  ViewState,
  type ViewStateData,
} from "./view/view-state";

export interface ApplicationState {
  activeDocuments: Map<DocumentId, ActiveDocument>;
  currentDocumentId: DocumentId | null;
  isPlaying: boolean;
  viewState: ViewStateData;
  selection: SelectionState;
  score: ScoreState;
  workspace: WorkspaceState;
}

export class Application extends Subscribable<ApplicationState> {
  // Sub-modules
  public readonly logger: Logger;
  public readonly workspaceManager: WorkspaceManager;
  public readonly viewState: ViewState;
  public readonly selectionManager: SelectionManager;
  public readonly scoreRenderer: ScoreRenderer;
  public readonly audioEngine: AudioEngine;
  public readonly performer: Performer;

  // Storage
  private workspaceStorage: WorkspaceStorage;
  public readonly resources: Resources;

  // Application State
  private _activeDocuments: Map<DocumentId, ActiveDocument>;
  private _currentDocumentId: DocumentId | null;

  // Cache for state stability
  private _activeDocumentsSnapshot: Map<DocumentId, ActiveDocument> | null =
    null;

  constructor() {
    super();

    // Initialize Sub-modules
    this.logger = new Logger();
    this.workspaceManager = new WorkspaceManager();
    this.viewState = new ViewState();
    this.selectionManager = new SelectionManager();
    this.scoreRenderer = new ScoreRenderer(this.logger);
    this.audioEngine = new AudioEngine();
    this.performer = new Performer(this.audioEngine);

    // Initialize Storage
    this.workspaceStorage = new WorkspaceStorage(
      this.workspaceManager.currentFs,
    );
    this.resources = new Resources();

    // Initialize State
    this._activeDocuments = new Map();
    this._currentDocumentId = null;

    // Initialize initial state cache
    this.updateState();

    // Wiring
    this.workspaceManager.subscribe((state) => {
      this.workspaceStorage.setFileSystem(state.fs);
      this.closeAllDocuments();
    });

    this.viewState.subscribe(() => {
      this.scoreRenderer.setScale(this.viewState.scale);
      this.updateState();
    });

    this.scoreRenderer.subscribe(() => this.updateState());
    this.selectionManager.subscribe(() => this.updateState());
    this.performer.subscribePlayback(() => this.updateState());
    this.workspaceManager.subscribe(() => this.updateState());
  }

  async init() {
    await this.scoreRenderer.init();
  }

  private updateState() {
    if (!this._activeDocumentsSnapshot) {
      this._activeDocumentsSnapshot = new Map(this._activeDocuments);
    }

    this.emit({
      activeDocuments: this._activeDocumentsSnapshot,
      currentDocumentId: this._currentDocumentId,
      isPlaying: this.performer.isPlaying,
      viewState: this.viewState.getState(),
      selection: this.selectionManager.getState(),
      score: this.scoreRenderer.getState(),
      workspace: this.workspaceManager.getState(),
    });
  }

  private invalidateDocumentsSnapshot() {
    this._activeDocumentsSnapshot = null;
    this.updateState();
  }

  async newProject(): Promise<void> {
    const template = `<?xml version="1.0" encoding="UTF-8"?>
<mei xmlns="http://www.music-encoding.org/ns/mei" meiversion="5.0">
  <meiHead>
    <fileDesc>
      <titleStmt>
        <title>New Project</title>
      </titleStmt>
      <pubStmt/>
    </fileDesc>
  </meiHead>
  <music>
    <body>
      <mdiv>
        <score>
          <scoreDef>
            <staffGrp>
              <staffDef n="1" lines="5" clef.shape="G" clef.line="2"/>
            </staffGrp>
          </scoreDef>
          <section>
            <measure n="1">
              <staff n="1">
                <layer n="1">
                  <mRest/>
                </layer>
              </staff>
            </measure>
          </section>
        </score>
      </mdiv>
    </body>
  </music>
</mei>`;

    const id = `Untitled-${Date.now()}.mei` as DocumentId;
    const newDoc: Document = {
      id,
      path: id as unknown as DocumentPath,
      origin: "workspace",
      type: "mei",
      content: template,
    };

    const activeDoc = new ActiveDocument(newDoc);
    // Mark as dirty initially? Or clean? Usually clean if it's a template, but maybe dirty so users know to save.
    // Let's keep it clean as it is "saved" in memory as a new file, but unsaved to disk.
    // Actually, ActiveDocument.isDirty tracks changes *since load*.
    // If we want "Untitled" files to prompt for save, they should be treated carefully.
    // For now, let's treat it as a clean new file.

    this._activeDocuments.set(id, activeDoc);
    this.activate(id);
    this.invalidateDocumentsSnapshot();
  }

  // --- Audio Methods ---
  async playAudio() {
    const mei = this.currentMEI();
    if (!mei) {
      this.logger.notify("No MEI content available for playback", "warn");
      return;
    }

    try {
      const partData = await apiClient.getPerformanceData(mei.document);
      this.performer.play(partData);
    } catch (e) {
      this.logger.notify(`Playback failed: ${e}`, "error");
    }
  }

  stopAudio() {
    this.performer.stop();
  }

  playSelectedNote() {
    if (this.performer.isPlaying) return;

    const mei = this.currentMEI();
    const selectedIds = this.selectionManager.getState().selectedIds;
    const selectedNoteId = selectedIds.length > 0 ? selectedIds[0] : null;

    if (!mei || !selectedNoteId) return;

    try {
      const partData = generateAudioForNote(mei.document, selectedNoteId);
      this.performer.play(partData);
    } catch (e) {
      this.logger.notify(`Playback failed: ${e}`, "error");
    }
  }

  // --- View Methods ---
  setVerifyScale(scale: number) {
    this.viewState.setScale(scale);
  }

  setViewMode(mode: ViewMode) {
    this.viewState.setViewMode(mode);
  }

  renderScorePage(page: number) {
    return this.scoreRenderer.renderPageToSVG(page);
  }

  // --- Document Management ---

  get activeDocuments() {
    return this._activeDocuments;
  }

  get currentDocumentId() {
    return this._currentDocumentId;
  }

  currentDocument(): ActiveDocument | null {
    if (this._currentDocumentId === null) return null;
    return this._activeDocuments.get(this._currentDocumentId) ?? null;
  }

  currentMEI(): MEI | null {
    return this.currentDocument()?.currentMEI() ?? null;
  }

  async open(documentId: DocumentId, origin: DocumentOrigin): Promise<boolean> {
    let doc: Document | null = null;
    try {
      if (origin === "workspace") {
        doc = await this.workspaceStorage.load(documentId);
      } else {
        doc = await this.resources.load(documentId);
      }
    } catch (e) {
      this.logger.notify(`Failed to load document: ${e}`, "error");
      return false;
    }

    if (!doc) {
      this.logger.notify(`Document not found: ${documentId}`, "error");
      return false;
    }

    if (!this._activeDocuments.has(doc.id)) {
      this._activeDocuments.set(doc.id, new ActiveDocument(doc));
    }

    this._currentDocumentId = doc.id;

    // Load data into renderer initially
    const activeDoc = this._activeDocuments.get(doc.id);
    if (activeDoc) {
      // For initialization, we load full content.
      // Optimization: If we had a hint system here, we could use it,
      // but opening a file is always a "full" load.
      this.scoreRenderer.loadData(activeDoc.getContent());
    }

    this.invalidateDocumentsSnapshot();
    return true;
  }

  async close(documentId: DocumentId): Promise<void> {
    if (this._activeDocuments.has(documentId)) {
      this._activeDocuments.delete(documentId);
      if (this._currentDocumentId === documentId) {
        this._currentDocumentId =
          this._activeDocuments.size > 0
            ? this._activeDocuments.keys().next().value || null
            : null;
      }
      this.invalidateDocumentsSnapshot();
    }
  }

  async closeAllDocuments() {
    this._activeDocuments.clear();
    this._currentDocumentId = null;
    this.invalidateDocumentsSnapshot();
  }

  activate(documentId: DocumentId) {
    if (this._activeDocuments.has(documentId)) {
      this._currentDocumentId = documentId;
      const doc = this._activeDocuments.get(documentId);
      if (doc) {
        this.scoreRenderer.loadData(doc.getContent());
      }
      this.updateState();
    }
  }

  async save(): Promise<void> {
    const doc = this.currentDocument();
    if (!doc) return;

    if (doc.origin() !== "workspace") {
      this.logger.notify(
        "Cannot save resource documents directly. Use Save As.",
        "warn",
      );
      return;
    }

    try {
      await this.workspaceStorage.save(doc.toDocument());
      doc.markAsSaved();
      this.logger.notify("Document saved", "info");
    } catch (e) {
      this.logger.notify(`Failed to save document: ${e}`, "error");
    }
  }

  async saveAs(path: string): Promise<void> {
    const activeDoc = this.currentDocument();
    if (!activeDoc) {
      this.logger.notify("No active document to save", "error");
      return;
    }

    try {
      const newDoc: Document = {
        ...activeDoc.toDocument(),
        id: path as DocumentId,
        path: path as DocumentPath,
        origin: "workspace",
      };

      await this.workspaceStorage.save(newDoc);

      const newActiveDoc = new ActiveDocument(newDoc);
      newActiveDoc.markAsSaved();

      this._activeDocuments.set(newDoc.id, newActiveDoc);
      this._currentDocumentId = newDoc.id;

      this.logger.notify(`Document saved as ${path}`, "info");
      this.invalidateDocumentsSnapshot();
    } catch (e) {
      this.logger.notify(`Failed to save document as: ${e}`, "error");
    }
  }
}
