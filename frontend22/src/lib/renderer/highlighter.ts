export function applyHighlights(
  svg: SVGSVGElement,
  selectedIds: string[],
  prevSelectedIds: string[],
) {
  // Helper to toggle class on an ID
  const toggleClass = (id: string, add: boolean) => {
    const escapedId = CSS.escape(id);
    const hitbox =
      svg.querySelector(`.hitbox[corresp="#${escapedId}"]`) ||
      svg.querySelector(`.hitbox[corresp="${escapedId}"]`);

    if (hitbox) {
      if (add) hitbox.classList.add("is-selected");
      else hitbox.classList.remove("is-selected");
    } else {
      const el = svg.querySelector(`[id="${escapedId}"]`);
      if (el) {
        if (add) el.classList.add("is-selected");
        else el.classList.remove("is-selected");
      }
    }
  };

  // Diffing
  const toRemove = prevSelectedIds.filter((id) => !selectedIds.includes(id));
  const toAdd = selectedIds.filter((id) => !prevSelectedIds.includes(id));

  toRemove.forEach((id) => {
    toggleClass(id, false);
  });
  toAdd.forEach((id) => {
    toggleClass(id, true);
  });
}

export function forceApplyHighlights(
  svg: SVGSVGElement,
  selectedIds: string[],
) {
  selectedIds.forEach((id) => {
    const escapedId = CSS.escape(id);
    const hitbox =
      svg.querySelector(`.hitbox[corresp="#${escapedId}"]`) ||
      svg.querySelector(`.hitbox[corresp="${escapedId}"]`);

    if (hitbox) {
      hitbox.classList.add("is-selected");
    } else {
      const el = svg.querySelector(`[id="${escapedId}"]`);
      if (el) {
        el.classList.add("is-selected");
      }
    }
  });
}

export function applyEditorHighlights(
  svg: SVGSVGElement,
  editorSelectedIds: string[],
) {
  let highlightGroup = svg.querySelector("#editor-highlights");
  if (!highlightGroup) {
    highlightGroup = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "g",
    );
    highlightGroup.setAttribute("id", "editor-highlights");
    highlightGroup.setAttribute("pointer-events", "none");
    svg.appendChild(highlightGroup);
  } else {
    highlightGroup.innerHTML = "";
  }

  const svgMatrix = svg.getScreenCTM();
  if (!svgMatrix) return;
  const inverseMatrix = svgMatrix.inverse();

  for (const id of editorSelectedIds) {
    const el = svg.querySelector(
      `[id="${CSS.escape(id)}"]`,
    ) as SVGGraphicsElement;
    if (!el) continue;

    try {
      const bbox = el.getBoundingClientRect();

      const pt1 = svg.createSVGPoint();
      pt1.x = bbox.left;
      pt1.y = bbox.top;
      const svgPt1 = pt1.matrixTransform(inverseMatrix);

      const pt2 = svg.createSVGPoint();
      pt2.x = bbox.right;
      pt2.y = bbox.bottom;
      const svgPt2 = pt2.matrixTransform(inverseMatrix);

      const width = Math.abs(svgPt2.x - svgPt1.x);
      const height = Math.abs(svgPt2.y - svgPt1.y);
      const x = Math.min(svgPt1.x, svgPt2.x);
      const y = Math.min(svgPt1.y, svgPt2.y);

      const rect = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "rect",
      );
      rect.setAttribute("x", (x - 4).toString());
      rect.setAttribute("y", (y - 4).toString());
      rect.setAttribute("width", (width + 8).toString());
      rect.setAttribute("height", (height + 8).toString());
      rect.setAttribute("class", "editor-highlight-rect");

      highlightGroup.appendChild(rect);
    } catch (e) {
      console.warn("Failed to calculate highlight bbox", e);
    }
  }
}
