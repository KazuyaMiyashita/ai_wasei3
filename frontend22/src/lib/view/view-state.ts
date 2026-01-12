import { Subscribable } from "../shared/subscribable";

export type ViewMode = "document" | "code" | "split";

export interface ViewStateData {
  scale: number;
  viewMode: ViewMode;
}

export class ViewState extends Subscribable<ViewStateData> {
  private _scale = 100;
  private _viewMode: ViewMode = "document";

  constructor() {
    super();
    this.updateState();
  }

  get scale() {
    return this._scale;
  }

  get viewMode() {
    return this._viewMode;
  }

  setScale(scale: number | ((prev: number) => number)) {
    if (typeof scale === "function") {
      this._scale = scale(this._scale);
    } else {
      this._scale = scale;
    }
    this.updateState();
  }

  setViewMode(viewMode: ViewMode | ((prev: ViewMode) => ViewMode)) {
    if (typeof viewMode === "function") {
      this._viewMode = viewMode(this._viewMode);
    } else {
      this._viewMode = viewMode;
    }
    this.updateState();
  }

  private updateState() {
    this.emit({
      scale: this._scale,
      viewMode: this._viewMode,
    });
  }
}
