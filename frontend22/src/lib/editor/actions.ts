export interface IEditAction {
  readonly type: string;
  applyTo(content: string): string;
  createInverse(contentBeforeEdit: string): IEditAction;
}

export class ReplaceAction implements IEditAction {
  readonly type = "ReplaceString";
  constructor(
    public begin: number,
    public end: number,
    public content: string,
  ) {}

  applyTo(content: string): string {
    return (
      content.slice(0, this.begin) + this.content + content.slice(this.end)
    );
  }

  createInverse(contentBeforeEdit: string): IEditAction {
    const deletedContent = contentBeforeEdit.slice(this.begin, this.end);
    return new ReplaceAction(
      this.begin,
      this.begin + this.content.length,
      deletedContent,
    );
  }
}

export class FullReplaceAction implements IEditAction {
  readonly type = "FullReplace";
  constructor(public newContent: string) {}

  applyTo(_content: string): string {
    return this.newContent;
  }

  createInverse(contentBeforeEdit: string): IEditAction {
    return new FullReplaceAction(contentBeforeEdit);
  }
}

/**
 * Represents a semantic edit to an MEI element.
 * This action assumes the underlying XML structure is valid and the ID exists.
 */
export class MeiElementReplaceAction implements IEditAction {
  readonly type = "MeiElementReplace";

  /**
   * @param targetXmlId The xml:id of the element to replace.
   * @param newXmlFragment The new XML string for the element.
   * @param _oldXmlFragmentForUndo The previous XML string (for undo).
   */
  constructor(
    public targetXmlId: string,
    public newXmlFragment: string,
    private _oldXmlFragmentForUndo: string = "",
  ) {}

  applyTo(content: string): string {
    // Note: Applying semantic edits directly to a raw string is complex.
    // Ideally, the ContentAdapter should handle this by parsing, modifying DOM, and serializing.
    // However, to satisfy the interface, we might need a helper or just return content
    // if the system is designed to use ContentAdapter's internal logic for this action type.
    // For now, we assume the caller/adapter handles the actual string manipulation
    // or this method uses a regex/DOM parser helper.

    // A naive implementation using Regex (Use with caution, better handled in Adapter):
    // This is just a placeholder to satisfy the interface.
    // The actual logic should be in the ContentAdapter or model layer.
    return content;
  }

  createInverse(_contentBeforeEdit: string): IEditAction {
    return new MeiElementReplaceAction(
      this.targetXmlId,
      this._oldXmlFragmentForUndo,
      this.newXmlFragment,
    );
  }
}
