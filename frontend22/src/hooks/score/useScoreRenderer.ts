import { useCallback } from "react";

export function useScoreRenderer() {
  const injectHitboxes = useCallback((svgElement: SVGSVGElement) => {
    // Clear existing hitboxes
    svgElement.querySelectorAll(".hitbox").forEach((el) => {
      el.remove();
    });

    const staves = svgElement.querySelectorAll(".staff");
    staves.forEach((staff) => {
      const staffEl = staff as SVGGraphicsElement;
      let staffRect: SVGRect | null = null;

      // Calculate Staff BBox (based on staff lines)
      const lines = staffEl.querySelectorAll(":scope > path");
      let minX = Number.POSITIVE_INFINITY;
      let maxX = Number.NEGATIVE_INFINITY;
      let minY = Number.POSITIVE_INFINITY;
      let maxY = Number.NEGATIVE_INFINITY;

      lines.forEach((line) => {
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
      });

      if (minX !== Number.POSITIVE_INFINITY) {
        minY -= 100; // Padding
        maxY += 100;

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
      notes.forEach((note) => {
        const notehead = note.querySelector(".notehead");
        if (!notehead) return;

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
      });

      // Rests
      const rests = staffEl.querySelectorAll(".rest");
      rests.forEach((rest) => {
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
      });

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
      noteHitboxes.forEach((rect) => {
        staff.appendChild(rect);
      });
    });
  }, []);

  return { injectHitboxes };
}
