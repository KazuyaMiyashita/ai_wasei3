export interface ScorePosition {
  part: number;
  measure: number;
  beat: string;
}

export interface ScoreEntry {
  path: string;
  name: string;
}

export interface AnnotationEntry {
  id: string;
  menuId: string;
  inputValue: string;
  selectedIds: string[];
  latestPosition?: ScorePosition;
  createdAt: number;
}

export interface ScoreState {
  mei: string;
  annotations: AnnotationEntry[];
}

export interface Tab {
  path: string;
  name: string;
  state: "semi-open" | "open" | "editing";
  isDirty: boolean;

  // History Management
  current: ScoreState;
  history: ScoreState[];
  future: ScoreState[];
}
