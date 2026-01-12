import { getMeasureNumberById } from "../model/music/mei-utils";

export const collectNoteX = (
  svgElement: SVGSVGElement,
): [string, SVGRect][] => {
  const results: [string, SVGRect][] = [];
  const notes = svgElement.querySelectorAll(".note");
  for (const note of notes) {
    const notehead = note.querySelector(".notehead");
    if (notehead && note.id) {
      results.push([note.id, (notehead as SVGGraphicsElement).getBBox()]);
    }
  }
  const rests = svgElement.querySelectorAll(".rest");
  for (const rest of rests) {
    if (rest.id) {
      results.push([rest.id, (rest as SVGGraphicsElement).getBBox()]);
    }
  }
  return results;
};

export const getStaffYRange = (svgElement: SVGSVGElement) => {
  const staves = svgElement.querySelectorAll(".staff");
  let minY = Number.POSITIVE_INFINITY;
  let maxY = Number.NEGATIVE_INFINITY;
  for (const staff of staves) {
    const bbox = (staff as SVGGraphicsElement).getBBox();
    minY = Math.min(minY, bbox.y);
    maxY = Math.max(maxY, bbox.y + bbox.height);
  }
  return { top: minY, bottom: maxY };
};

export const injectHitboxes = (svgElement: SVGSVGElement) => {
  // Clear existing hitboxes
  const existingHitboxes = svgElement.querySelectorAll(".hitbox");
  for (const el of existingHitboxes) {
    el.remove();
  }

  const staves = svgElement.querySelectorAll(".staff");
  for (const staff of staves) {
    const staffEl = staff as SVGGraphicsElement;
    let staffRect: SVGRect | null = null;

    // Calculate Staff BBox (based on staff lines)
    const lines = staffEl.querySelectorAll(":scope > path");
    let minX = Number.POSITIVE_INFINITY;
    let maxX = Number.NEGATIVE_INFINITY;
    let minY = Number.POSITIVE_INFINITY;
    let maxY = Number.NEGATIVE_INFINITY;

    for (const line of lines) {
      const pathEl = line as SVGGraphicsElement;
      try {
        const b = pathEl.getBBox();
        minX = Math.min(minX, b.x);
        maxX = Math.max(maxX, b.x + b.width);
        minY = Math.min(minY, b.y);
        maxY = Math.max(maxY, b.y + b.height);
      } catch (_e) {
        // ignore
      }
    }

    if (minX !== Number.POSITIVE_INFINITY) {
      minY -= 100; // Padding
      maxY += 100;

      // Manually constructing a rect-like object since SVGRect constructor might not be available or behave differently
      staffRect = {
        x: minX,
        y: minY,
        width: maxX - minX,
        height: maxY - minY,
      } as SVGRect;
    }

    const noteHitboxes: Element[] = [];

    // Notes
    const notes = staffEl.querySelectorAll(".note");
    for (const note of notes) {
      const notehead = note.querySelector(".notehead");
      if (!notehead) continue;

      const noteheadEl = notehead as SVGGraphicsElement;
      try {
        const bbox = noteheadEl.getBBox();
        const cx = bbox.x + bbox.width / 2;
        const cy = bbox.y + bbox.height / 2;
        const r = Math.max(bbox.width, bbox.height) / 2 + 150;

        if (r > 0) {
          const circle = document.createElementNS(
            "http://www.w3.org/2000/svg",
            "circle",
          );
          circle.setAttribute("cx", cx.toString());
          circle.setAttribute("cy", cy.toString());
          circle.setAttribute("r", r.toString());
          circle.setAttribute("class", "hitbox");
          if (note.id) {
            circle.setAttribute("corresp", `#${note.id}`);
          }
          noteHitboxes.push(circle);
        }
      } catch (e) {
        console.warn("Note bbox failed", e);
      }
    }

    // Rests
    const rests = staffEl.querySelectorAll(".rest");
    for (const rest of rests) {
      const restEl = rest as SVGGraphicsElement;
      try {
        const bbox = restEl.getBBox();
        const cx = bbox.x + bbox.width / 2;
        const cy = bbox.y + bbox.height / 2;
        const r = Math.max(bbox.width, bbox.height) / 2 + 150;

        if (r > 0) {
          const circle = document.createElementNS(
            "http://www.w3.org/2000/svg",
            "circle",
          );
          circle.setAttribute("cx", cx.toString());
          circle.setAttribute("cy", cy.toString());
          circle.setAttribute("r", r.toString());
          circle.setAttribute("class", "hitbox");
          if (rest.id) {
            circle.setAttribute("corresp", `#${rest.id}`);
          }
          noteHitboxes.push(circle);
        }
      } catch (e) {
        console.warn("Rest bbox failed", e);
      }
    }

    // Append Staff hitbox
    if (staffRect && staff.id) {
      const rect = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "rect",
      );
      rect.setAttribute("x", staffRect.x.toString());
      rect.setAttribute("y", staffRect.y.toString());
      rect.setAttribute("width", staffRect.width.toString());
      rect.setAttribute("height", staffRect.height.toString());
      rect.setAttribute("class", "hitbox");
      rect.setAttribute("corresp", `#${staff.id}`);
      staff.appendChild(rect);
    }

    // Append Note/Rest hitboxes
    for (const rect of noteHitboxes) {
      staff.appendChild(rect);
    }
  }
};

export const drawAllPlaybackCursors = (
  svgElement: SVGSVGElement,
  meiContent: string | null,
) => {
  const targetContainer = svgElement.querySelector(".page-margin");
  if (targetContainer) {
    const { top, bottom } = getStaffYRange(svgElement);
    const noteCoords = collectNoteX(svgElement);

    const xToIdMap = new Map<number, string>();
    for (const [id, bbox] of noteCoords) {
      const x = bbox.x + bbox.width / 2;
      if (!xToIdMap.has(x)) {
        xToIdMap.set(x, id);
      }
    }

    let cursorGroup = targetContainer.querySelector("#cursor-group");
    if (cursorGroup) cursorGroup.remove();

    cursorGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
    cursorGroup.setAttribute("id", "cursor-group");
    cursorGroup.setAttribute("pointer-events", "none");

    const sortedX = Array.from(xToIdMap.keys()).sort((a, b) => a - b);

    let meiXML: Document | null = null;
    if (meiContent) {
      try {
        meiXML = new DOMParser().parseFromString(meiContent, "application/xml");
      } catch (e) {
        console.warn("Failed to parse MEI for cursor generation", e);
      }
    }

    for (const x of sortedX) {
      const representativeId = xToIdMap.get(x);
      if (!representativeId) continue;

      let measureNum: number | null = null;
      if (meiXML) {
        measureNum = getMeasureNumberById(meiXML, representativeId);
      }

      const line = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "line",
      );
      line.setAttribute("x1", x.toString());
      line.setAttribute("y1", top.toString());
      line.setAttribute("x2", x.toString());
      line.setAttribute("y2", bottom.toString());
      line.setAttribute("stroke", "rgba(255, 0, 0, 0.0)");
      line.setAttribute("stroke-width", "30");
      line.setAttribute("class", "playback-cursor-line");

      if (measureNum !== null) {
        line.setAttribute("data-measure", measureNum.toString());
      }

      cursorGroup.appendChild(line);
    }

    targetContainer.appendChild(cursorGroup);
  }
};
