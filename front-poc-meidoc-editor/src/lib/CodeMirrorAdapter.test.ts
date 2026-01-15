import { describe, expect, it, vi } from "vitest";
import { CodeMirrorAdapter } from "./CodeMirrorAdapter";

describe("CodeMirrorAdapter", () => {
  it("should extract changes from ViewUpdate", () => {
    const adapter = new CodeMirrorAdapter();
    const update = {
      docChanged: true,
      changes: {
        iterChanges: (
          cb: (
            fA: number,
            tA: number,
            fB: number,
            tB: number,
            i: { toString: () => string },
          ) => void,
        ) => {
          cb(0, 5, 0, 5, { toString: () => "World" });
        },
      },
    };
    // @ts-expect-error - Mocking ViewUpdate
    const changes = adapter.getChangesFromUpdate(update);
    expect(changes).toEqual([{ from: 0, to: 5, insert: "World" }]);
  });

  it("should apply changes to view", () => {
    const adapter = new CodeMirrorAdapter();
    const dispatchMock = vi.fn();
    const viewMock = {
      dispatch: dispatchMock,
    };
    // @ts-expect-error
    adapter.setView(viewMock);

    adapter.applyChanges([{ from: 0, to: 5, insert: "Test" }]);
    expect(dispatchMock).toHaveBeenCalledWith({
      changes: [{ from: 0, to: 5, insert: "Test" }],
    });
  });
});
