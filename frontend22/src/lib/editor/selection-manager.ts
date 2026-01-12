import { Subscribable } from "../shared/subscribable";

export interface SelectionState {
  selectedIds: string[];
  editorSelectedIds: string[];
}

export class SelectionManager extends Subscribable<SelectionState> {
  private _selectedIds: string[] = [];
  private _editorSelectedIds: string[] = [];

  constructor() {
    super();
    this.updateState();
  }

  get selectedIds() {
    return this._selectedIds;
  }

  get editorSelectedIds() {
    return this._editorSelectedIds;
  }

  setSelectedIds(ids: string[]) {
    this._selectedIds = ids;
    this.updateState();
  }

  setEditorSelectedIds(ids: string[]) {
    this._editorSelectedIds = ids;
    this.updateState();
  }

  clearSelection() {
    this._selectedIds = [];
    this._editorSelectedIds = [];
    this.updateState();
  }

  private updateState() {
    const newState: SelectionState = {
      selectedIds: this._selectedIds, // Use reference, assuming immutability of array passed in or acceptable risk
      editorSelectedIds: this._editorSelectedIds,
    };
    // Defensive copy for safety if getters return raw arrays
    newState.selectedIds = [...this._selectedIds];
    newState.editorSelectedIds = [...this._editorSelectedIds];

    this.emit(newState);
  }

  // getState() is handled by base class returning _state
}
