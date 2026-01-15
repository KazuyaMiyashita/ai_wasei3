import type { EditorView, ViewUpdate } from "@codemirror/view";

export interface CMChange {
  from: number;
  to: number;
  insert: string;
}

export class CodeMirrorAdapter {
  view: EditorView | null = null;

  setView(view: EditorView | null) {
    this.view = view;
  }

  getChangesFromUpdate(update: ViewUpdate): CMChange[] {
    const changes: CMChange[] = [];
    if (update.docChanged) {
      update.changes.iterChanges((fromA, toA, _fromB, _toB, inserted) => {
        changes.push({
          from: fromA,
          to: toA,
          insert: inserted.toString(),
        });
      });
    }
    return changes;
  }

  applyChanges(changes: { from: number; to: number; insert: string }[]) {
    if (!this.view) return;

    const cmChanges = changes.map((c) => ({
      from: c.from,
      to: c.to,
      insert: c.insert,
    }));

    this.view.dispatch({
      changes: cmChanges,
      // filter: false
    });
  }
}
