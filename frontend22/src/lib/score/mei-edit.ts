import type { EditorAction } from "verovio";

export type EditResult =
  | { type: "no-change" }
  | { type: "verovio-edit"; action: EditorAction }
  | { type: "full-reload"; newMei: string }
  | { type: "error"; error: string };

/**
 * Parses the edited XML snippet and determines the necessary action to update the MEI.
 * It compares the new snippet with the existing element in the MEI document.
 *
 * - If only attributes have changed, it returns a 'verovio-edit' result with an EditorAction.
 * - If the structure (tag name, children, text content) has changed, it performs a DOM replacement
 *   on a clone of the MEI document and returns a 'full-reload' result with the new MEI string.
 * - If no changes are detected, it returns 'no-change'.
 * - If parsing fails or the element is not found, it returns an 'error'.
 */
export function editFromXml(
  meiXML: Document,
  elementId: string,
  editedXml: string,
): EditResult {
  if (editedXml === "") {
    // Empty XML -> Delete element
    // Deletion is a structural change if we consider it removing a node from the tree.
    // Verovio 'delete' action exists, but let's check if it supports it well.
    // For consistency with structural changes, we might want to do a full reload or use delete action.
    // Verovio supports { action: 'delete', param: { elementId } }.
    // Let's try to use Verovio action for deletion first as it is optimized.
    return {
      type: "verovio-edit",
      action: {
        action: "chain",
        param: [
          { action: "delete", param: { elementId } },
          // { action: "commit" },
        ],
      },
    };
  }

  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(editedXml, "application/xml");
    const errorNode = doc.querySelector("parsererror");
    if (errorNode) {
      return {
        type: "error",
        error: `XML Parse Error: ${errorNode.textContent}`,
      };
    }

    const newEl = doc.documentElement;

    // Find original using ID
    let originalEl = meiXML.querySelector(`[*|id="${elementId}"]`);
    if (!originalEl) originalEl = meiXML.getElementById(elementId);

    if (!originalEl) {
      return {
        type: "error",
        error: `Original element not found: ${elementId}`,
      };
    }

    // Check for structural changes
    // 1. Tag name
    if (newEl.tagName !== originalEl.tagName) {
      return createFullReloadResult(meiXML, originalEl, newEl, elementId);
    }

    // 2. Child nodes (shallow check for existence/count is not enough, need deep comparison or assumption)
    // If the element has child elements, we assume it's a structural change for safety,
    // unless we implement a deep attribute-only diff.
    // Verovio's 'set' action only works on attributes of the target element.
    // It cannot add/remove children.
    // So if either original or new element has children (Element type), we must use full reload.
    // Exception: If children are exactly the same.
    const hasChildren = (el: Element) =>
      Array.from(el.childNodes).some((n) => n.nodeType === Node.ELEMENT_NODE);

    if (hasChildren(originalEl) || hasChildren(newEl)) {
      // For now, treat any element with children as a structural update to be safe.
      // We could optimize this by deep comparing, but 'full-reload' is reliable.
      // Optimization: Check if innerHTML is identical? Serializing might handle it.
      if (originalEl.innerHTML !== newEl.innerHTML) {
        return createFullReloadResult(meiXML, originalEl, newEl, elementId);
      }
      // If innerHTML is same, we still need to check attributes of the root element.
      // Fall through to attribute check.
    }

    // 3. Text content (for leaf nodes)
    // If text content differs, Verovio 'set' cannot handle it (it only sets attributes).
    // Note: <syl> etc might use text content.
    const originalText = Array.from(originalEl.childNodes)
      .filter((n) => n.nodeType === Node.TEXT_NODE)
      .map((n) => n.textContent)
      .join("");
    const newText = Array.from(newEl.childNodes)
      .filter((n) => n.nodeType === Node.TEXT_NODE)
      .map((n) => n.textContent)
      .join("");

    if (originalText !== newText) {
      return createFullReloadResult(meiXML, originalEl, newEl, elementId);
    }

    // If we reached here, structure is identical (or assumed handled).
    // Now check attributes for Verovio 'set' action.
    const actions: EditorAction[] = [];

    // Compare attributes
    // 1. New/Updated attributes
    for (let i = 0; i < newEl.attributes.length; i++) {
      const attr = newEl.attributes[i];
      // Skip xmlns attributes
      if (attr.name === "xmlns" || attr.name.startsWith("xmlns:")) continue;

      const oldVal = originalEl.getAttribute(attr.name);
      if (oldVal !== attr.value) {
        actions.push({
          action: "set",
          param: { elementId, attribute: attr.name, value: attr.value },
        });
      }
    }

    // 2. Removed attributes
    for (let i = 0; i < originalEl.attributes.length; i++) {
      const attr = originalEl.attributes[i];
      // Skip xmlns attributes checks
      if (attr.name === "xmlns" || attr.name.startsWith("xmlns:")) continue;

      if (!newEl.hasAttribute(attr.name)) {
        // Attribute removed
        actions.push({
          action: "set",
          param: { elementId, attribute: attr.name, value: "" },
        });
      }
    }

    if (actions.length === 0) {
      return { type: "no-change" };
    }

    // Caller (useVerovio) handles commit after checking for warnings
    // actions.push({ action: "commit" });

    return {
      type: "verovio-edit",
      action: {
        action: "chain",
        param: actions,
      },
    };
  } catch (e) {
    return { type: "error", error: `Unexpected error: ${e}` };
  }
}

function createFullReloadResult(
  meiXML: Document,
  _originalEl: Element,
  newEl: Element,
  elementId: string,
): EditResult {
  try {
    // Clone the entire document to avoid mutating the current state view directly
    // (though in this app structure we might replace the state anyway, cloning is safer)
    const cloneDoc = meiXML.cloneNode(true) as Document;
    let target = cloneDoc.querySelector(`[*|id="${elementId}"]`);
    if (!target) target = cloneDoc.getElementById(elementId);

    if (!target) {
      return {
        type: "error",
        error: "Could not find element in cloned document for replacement.",
      };
    }

    // Import the new element into the cloned document
    const importedNode = cloneDoc.importNode(newEl, true);
    target.parentNode?.replaceChild(importedNode, target);

    const serializer = new XMLSerializer();
    const newMeiString = serializer.serializeToString(cloneDoc);

    return {
      type: "full-reload",
      newMei: newMeiString,
    };
  } catch (e) {
    return { type: "error", error: `Failed to create new MEI: ${e}` };
  }
}

export function validateEditXml(
  meiXML: Document,
  elementId: string,
  editedXml: string,
): { isValid: boolean; error?: string } {
  const result = editFromXml(meiXML, elementId, editedXml);
  if (result.type === "error") {
    return { isValid: false, error: result.error };
  }
  return { isValid: true };
}
