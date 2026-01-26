import { Subscribable } from "../shared/subscribable";

export type LayoutMode = "document" | "code" | "split";

export interface ViewStateData {
  scale: number;
  layoutMode: LayoutMode;
  editMode: boolean;
}

export class ViewState extends Subscribable<ViewStateData> {
  private _scale = 100;
  private _layoutMode: LayoutMode = "document";
  private _editMode = false;

  constructor() {
    super();
    this.updateState();
  }

  get scale() {
    return this._scale;
  }

  get layoutMode() {
    return this._layoutMode;
  }

  get editMode() {
    return this._editMode;
  }

  setScale(scale: number | ((prev: number) => number)) {
    if (typeof scale === "function") {
      this._scale = scale(this._scale);
    } else {
      this._scale = scale;
    }
    this.updateState();
  }

  setLayoutMode(layoutMode: LayoutMode | ((prev: LayoutMode) => LayoutMode)) {
    if (typeof layoutMode === "function") {
      this._layoutMode = layoutMode(this._layoutMode);
    } else {
      this._layoutMode = layoutMode;
    }
    this.updateState();
  }

  setEditMode(editMode: boolean | ((prev: boolean) => boolean)) {
    if (typeof editMode === "function") {
      this._editMode = editMode(this._editMode);
    } else {
      this._editMode = editMode;
    }
    this.updateState();
  }

  private updateState() {
    this.emit({
      scale: this._scale,
      layoutMode: this._layoutMode,
      editMode: this._editMode,
    });
  }
}
