import type { EditorAction } from "verovio";
import { MEI } from "../model/music/mei";
import { editFromXml } from "../model/music/mei-edit.ts";
import {
  type IEditAction,
  MeiElementReplaceAction,
  ReplaceAction,
} from "./actions";

export type UpdateHint =
  | { type: "full" } // Full re-render required (or non-MEI content changed)
  | { type: "none" } // No visual update required
  | { type: "mei-content"; index: number } // Content of a specific MEI block changed (requires loadData)
  | { type: "mei-action"; index: number; action: EditorAction }; // Verovio Editor Action (optimized update)

/**
 * ContentAdapter Interface
 * Handles difference between MEI-only and XHTML+MEI documents.
 * Responsible for applying edits and determining the scope of change.
 */
export interface ContentAdapter {
  /**
   * Current full content string.
   */
  get content(): string;

  /**
   * Initialize adapter with content.
   */
  initialize(content: string): void;

  /**
   * Get MEI instance for DOM operations.
   */
  getMeiInstance(index: number): MEI | null;

  /**
   * Apply an edit action and return the result and an update hint.
   */
  apply(action: IEditAction): { newContent: string; hint: UpdateHint };
}

// ----------------------------------------------------------------------
// Implementation: MEI Only
// ----------------------------------------------------------------------
export class MeiContentAdapter implements ContentAdapter {
  private _content = "";
  private mei: MEI | null = null;

  get content() {
    return this._content;
  }

  initialize(content: string) {
    this._content = content;
    try {
      this.mei = new MEI(content);
    } catch (e) {
      console.warn("MeiContentAdapter: Failed to parse MEI", e);
      this.mei = null;
    }
  }

  getMeiInstance(index: number): MEI | null {
    return index === 0 ? this.mei : null;
  }

  apply(action: IEditAction): { newContent: string; hint: UpdateHint } {
    if (action.type === "ReplaceString") {
      const newContent = action.applyTo(this._content);
      this.initialize(newContent); // Re-parse
      return { newContent, hint: { type: "mei-content", index: 0 } };
    }

    if (action instanceof MeiElementReplaceAction) {
      if (!this.mei)
        return { newContent: this._content, hint: { type: "none" } };

      const result = editFromXml(
        this.mei.document,
        action.targetXmlId,
        action.newXmlFragment,
      );

      if (result.type === "verovio-edit") {
        // Optimization: We don't necessarily need to update _content string immediately if we rely on Verovio to handle it?
        // NO, ActiveDocument MUST be the source of truth.
        // If we return "mei-action", the view will call toolkit.edit().
        // BUT we also need to update our internal string state to match what Verovio will have.
        // editFromXml doesn't return the full new string in verovio-edit case.
        // It returns an action.
        //
        // Problem: If we use Verovio action, we expect Verovio to update its internal state.
        // How do we sync back to our _content?
        // Option A: Perform the same DOM manipulation here (or approximate it) and serialize.
        // Option B: Assume Verovio is the truth for a moment (bad for architecture).
        // Option C: For "verovio-edit", we perform the DOM update manually on our side too.
        //
        // editFromXml logic actually checks attributes.
        // We should apply attributes to our DOM.
        this.applyEditorActionToDom(result.action);
        const newContent = this.mei.serialize();
        this._content = newContent;
        return {
          newContent,
          hint: { type: "mei-action", index: 0, action: result.action },
        };
      }

      if (result.type === "full-reload") {
        const newContent = result.newMei;
        this.initialize(newContent);
        return { newContent, hint: { type: "mei-content", index: 0 } };
      }

      return { newContent: this._content, hint: { type: "none" } };
    }

    // Fallback
    const newContent = action.applyTo(this._content);
    this.initialize(newContent);
    return { newContent, hint: { type: "full" } };
  }

  private applyEditorActionToDom(action: EditorAction) {
    if (!this.mei) return;
    if (action.action === "chain" && Array.isArray(action.param)) {
      action.param.forEach((a) => {
        this.applyEditorActionToDom(a as EditorAction);
      });
      return;
    }
    if (
      action.action === "set" &&
      action.param &&
      "elementId" in action.param
    ) {
      const { elementId, attribute, value } = action.param;
      const el = this.mei.getNoteElementById(elementId);
      if (el && attribute) {
        if (value === "") {
          el.removeAttribute(attribute);
        } else {
          el.setAttribute(attribute, value);
        }
      }
    }
    if (
      action.action === "delete" &&
      action.param &&
      "elementId" in action.param
    ) {
      const { elementId } = action.param;
      const el = this.mei.getNoteElementById(elementId);
      el?.remove();
    }
  }
}

// ----------------------------------------------------------------------
// Implementation: XHTML + MEI
// ----------------------------------------------------------------------
export class XhtmlMeiContentAdapter implements ContentAdapter {
  private _content = "";
  private meiInstances: MEI[] = [];
  // Cache the ranges of MEI blocks in the current string
  private meiRanges: { start: number; end: number; index: number }[] = [];

  get content() {
    return this._content;
  }

  initialize(content: string) {
    this._content = content;
    this.parseRanges(content);
  }

  private parseRanges(content: string) {
    this.meiInstances = [];
    this.meiRanges = [];

    // Simple parser to find <mei>...</mei> blocks
    // Note: This is a regex-based approximation.
    // Ideally we use a tokenizer, but given the constraints, regex is faster for now.
    // Assuming <mei> tags are not nested inside other weird constructs (like comments/strings) too deeply.
    const regex = /<mei[\s>]([\s\S]*?)<\/mei>/gi;
    let match: RegExpExecArray | null;

    let index = 0;
    // biome-ignore lint/suspicious/noAssignInExpressions: loop
    while ((match = regex.exec(content)) !== null) {
      const start = match.index;
      const end = start + match[0].length;
      this.meiRanges.push({ start, end, index });

      try {
        this.meiInstances.push(new MEI(match[0]));
      } catch (e) {
        console.warn(`Failed to parse MEI block ${index}`, e);
        // Push null or dummy? Push null-like behavior handled by getMeiInstance
      }
      index++;
    }
  }

  getMeiInstance(index: number): MEI | null {
    return this.meiInstances[index] || null;
  }

  apply(action: IEditAction): { newContent: string; hint: UpdateHint } {
    if (action.type === "ReplaceString" && action instanceof ReplaceAction) {
      // Check if replacement touches any MEI block
      // Determine scope
      const editStart = action.begin;
      const editEnd = action.end;

      let affectedMeiIndex = -1;
      let isStructureBroken = false;

      for (const range of this.meiRanges) {
        // Check overlap
        if (editEnd > range.start && editStart < range.end) {
          // Edit touches this MEI block
          // If edit crosses boundaries, it might break structure
          if (editStart < range.start || editEnd > range.end) {
            isStructureBroken = true;
          }
          affectedMeiIndex = range.index;
          break; // Assume only one MEI block is touched for simplicity
        }
      }

      const newContent = action.applyTo(this._content);
      this.initialize(newContent); // Re-parse everything to be safe

      if (isStructureBroken || affectedMeiIndex === -1) {
        // If outside MEI or broke boundary
        return { newContent, hint: { type: "full" } };
      }

      return {
        newContent,
        hint: { type: "mei-content", index: affectedMeiIndex },
      };
    }

    if (action instanceof MeiElementReplaceAction) {
      // Currently structural edits on mixed content not fully supported via UI
      // But if we did:
      // We need to know WHICH MEI instance we are targeting.
      // The action doesn't specify index?
      // ActiveDocument usually knows the "currentMEIIndex".
      // But adapter doesn't know context.
      // The action should probably carry context or we assume current.
      // For now, return full reload to be safe.
      const newContent = action.applyTo(this._content);
      this.initialize(newContent);
      return { newContent, hint: { type: "full" } };
    }

    const newContent = action.applyTo(this._content);
    this.initialize(newContent);
    return { newContent, hint: { type: "full" } };
  }
}
