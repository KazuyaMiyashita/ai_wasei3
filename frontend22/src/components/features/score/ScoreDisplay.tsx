import type React from "react";
import { useCallback, useEffect, useRef, useState } from "react";
import type { UseScoreInteraction } from "../../../hooks/score/useScoreInteraction";
import { useScoreRenderer } from "../../../hooks/score/useScoreRenderer";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import {
  getBeatByXmlCalculation,
  getMeasureNumberById,
  getStaffNumberById,
} from "../../../lib/score/mei-utils";
import type { ScorePosition } from "../../../types";

interface ScoreDisplayProps {
  loading: UseScoreView["loading"];
  svgData: UseScoreView["svgData"];
  meiXML: UseScoreView["meiXML"];
  selectedIds: string[];
  editorSelectedIds?: string[];
  onSelectionChange: UseScoreInteraction["handleSelectionChange"];
  onContextMenu: UseScoreInteraction["handleContextMenu"];
}

const ScoreDisplay: React.FC<ScoreDisplayProps> = ({
  loading,
  svgData,
  meiXML,
  selectedIds,
  editorSelectedIds = [],
  onSelectionChange,
  onContextMenu,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const selectedElements = useRef<Set<Element>>(new Set());
  const selectionMode = useRef<"none" | "note" | "staff">("none");
  const selectedIdsRef = useRef(selectedIds);
  const { injectHitboxes } = useScoreRenderer();

  // Handle Editor Highlights (Visual only)
  // biome-ignore lint/correctness/useExhaustiveDependencies: Re-apply highlights when SVG data changes
  useEffect(() => {
    const svg = containerRef.current?.querySelector("svg");
    if (!svg) return;

    // Create or clear highlight group
    let highlightGroup = svg.querySelector("#editor-highlights");
    if (!highlightGroup) {
      highlightGroup = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "g",
      );
      highlightGroup.setAttribute("id", "editor-highlights");
      // pointer-events: none ensures it doesn't block hitboxes
      highlightGroup.setAttribute("pointer-events", "none");

      // Attempt to place it strategically?
      // If we append to root, it's on top.
      // To be "behind hitboxes" (which are in .staff), we can't easily do it globally
      // without hitboxes being global.
      // But pointer-events: none satisfies the interaction requirement.
      svg.appendChild(highlightGroup);
    } else {
      highlightGroup.innerHTML = "";
    }

    // Apply new highlights
    const svgMatrix = svg.getScreenCTM();
    if (!svgMatrix) return;
    const inverseMatrix = svgMatrix.inverse();

    for (const id of editorSelectedIds) {
      const el = svg.querySelector(
        `[id="${CSS.escape(id)}"]`,
      ) as SVGGraphicsElement;
      if (!el) continue;

      try {
        // Use getBoundingClientRect to handle all transforms automatically
        const bbox = el.getBoundingClientRect();

        // Convert top-left and bottom-right to SVG coordinates
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
        rect.setAttribute("x", (x - 4).toString()); // Add some padding
        rect.setAttribute("y", (y - 4).toString());
        rect.setAttribute("width", (width + 8).toString());
        rect.setAttribute("height", (height + 8).toString());
        rect.setAttribute("class", "editor-highlight-rect");

        highlightGroup.appendChild(rect);
      } catch (e) {
        console.warn("Failed to calculate highlight bbox", e);
      }
    }
  }, [editorSelectedIds, svgData]);

  useEffect(() => {
    selectedIdsRef.current = selectedIds;
  }, [selectedIds]);

  const [showLoadingText, setShowLoadingText] = useState(false);

  useEffect(() => {
    let timeout: ReturnType<typeof setTimeout>;
    if (loading) {
      timeout = setTimeout(() => {
        setShowLoadingText(true);
      }, 300);
    } else {
      setShowLoadingText(false);
    }
    return () => clearTimeout(timeout);
  }, [loading]);

  const updateStyles = useCallback((svg: SVGSVGElement) => {
    // Clear selection styles
    svg.querySelectorAll(".hitbox").forEach((el) => {
      el.classList.remove("is-selected");
    });

    // Apply selection styles
    selectedElements.current.forEach((el) => {
      if (el.id) {
        const hitbox =
          svg.querySelector(`.hitbox[corresp="${CSS.escape(el.id)}"]`) ||
          svg.querySelector(`.hitbox[corresp="#${CSS.escape(el.id)}"]`);
        if (hitbox) {
          hitbox.classList.add("is-selected");
        }
      }
    });
  }, []);

  // Update selection styles when selectedIds prop changes (e.g. external selection)
  useEffect(() => {
    const svg = containerRef.current?.querySelector("svg");
    if (!svg) return;

    // Sync selectedElements with selectedIds
    const newSelected = new Set<Element>();
    // Determine mode based on first selected element (heuristic)
    let newMode: "none" | "note" | "staff" = "none";

    for (const id of selectedIds) {
      const el = svg.querySelector(`[id="${CSS.escape(id)}"]`);
      if (el) {
        newSelected.add(el);
        if (newMode === "none") {
          newMode =
            el.classList.contains("note") || el.classList.contains("rest")
              ? "note"
              : "staff";
        }
      }
    }
    selectedElements.current = newSelected;
    selectionMode.current = newMode;
    updateStyles(svg);
  }, [selectedIds, updateStyles]);

  useEffect(() => {
    if (containerRef.current && svgData) {
      containerRef.current.innerHTML = svgData;
      const svg = containerRef.current.querySelector("svg");
      if (svg) {
        injectHitboxes(svg);

        // Restore selection from selectedIdsRef
        selectedElements.current.clear();
        const foundIds: string[] = [];
        let newMode: "none" | "note" | "staff" = "none";

        // Use ref here to avoid dependency loop
        const currentSelectedIds = selectedIdsRef.current;

        for (const id of currentSelectedIds) {
          const el = svg.querySelector(`[id="${CSS.escape(id)}"]`);
          if (el) {
            selectedElements.current.add(el);
            foundIds.push(id);
            if (newMode === "none") {
              newMode =
                el.classList.contains("note") || el.classList.contains("rest")
                  ? "note"
                  : "staff";
            }
          }
        }
        selectionMode.current = newMode;
        updateStyles(svg);

        // If some IDs were lost during re-render (e.g. deleted), update parent
        if (foundIds.length !== currentSelectedIds.length) {
          onSelectionChange(newMode, foundIds);
        }

        const handleInteraction = (e: MouseEvent) => {
          e.stopPropagation();
          const targetEl = e.target as Element;
          let target: Element | null = null;

          if (targetEl.classList.contains("hitbox")) {
            const corresp = targetEl.getAttribute("corresp");
            if (corresp) {
              const id = corresp.startsWith("#")
                ? corresp.substring(1)
                : corresp;
              target = svg.querySelector(`[id="${id}"]`);
            }
          } else {
            target = targetEl.closest(".staff, .note, .rest");
          }

          const isMultiSelect = e.shiftKey || e.ctrlKey || e.metaKey;
          const isContextMenu = e.type === "contextmenu";

          let newMode: "none" | "note" | "staff" = "none";
          let newSelectedIds: string[] = [];
          let position: ScorePosition | undefined;

          if (!target) {
            newMode = "none";
            selectedElements.current.clear();
          } else {
            const targetType =
              target.classList.contains("note") ||
              target.classList.contains("rest")
                ? "note"
                : "staff";

            const preserveSelection =
              isContextMenu && selectedElements.current.has(target);

            if (!preserveSelection) {
              if (!isMultiSelect) {
                selectedElements.current.clear();
                selectedElements.current.add(target);
                selectionMode.current = targetType;
              } else {
                if (
                  selectionMode.current !== "none" &&
                  selectionMode.current !== targetType
                ) {
                  selectedElements.current.clear();
                }
                selectionMode.current = targetType;

                if (selectedElements.current.has(target)) {
                  selectedElements.current.delete(target);
                  if (selectedElements.current.size === 0)
                    selectionMode.current = "none";
                } else {
                  selectedElements.current.add(target);
                }
              }
            } else {
              selectionMode.current = targetType;
            }

            newMode = selectionMode.current;
            newSelectedIds = Array.from(selectedElements.current)
              .map((el) => el.id)
              .filter((id) => !!id);

            if (meiXML && target.id) {
              try {
                const staffNum = getStaffNumberById(meiXML, target.id);
                let measureNum: number | null = null;
                let beatStr = "";

                if (targetType === "staff") {
                  const system = target.closest(".system") || svg;
                  const systemNotes = system.querySelectorAll(".note");
                  let minDist = Infinity;
                  let bestId: string | undefined;

                  systemNotes.forEach((note) => {
                    const notehead = note.querySelector(".notehead");
                    if (!notehead) return;
                    const el = notehead as SVGGraphicsElement;
                    const matrix = el.getScreenCTM();
                    if (!matrix) return;

                    const bbox = el.getBBox();
                    const cx = bbox.x + bbox.width / 2;
                    const cy = bbox.y + bbox.height / 2;
                    const screenX = matrix.a * cx + matrix.c * cy + matrix.e;

                    const dist = Math.abs(screenX - e.clientX);
                    if (dist < minDist) {
                      minDist = dist;
                      bestId = note.id;
                    }
                  });

                  if (bestId) {
                    measureNum = getMeasureNumberById(meiXML, bestId);
                    const b = getBeatByXmlCalculation(meiXML, bestId);
                    if (b) beatStr = b.toMusicalString();
                  }
                } else {
                  measureNum = getMeasureNumberById(meiXML, target.id);
                  const b = getBeatByXmlCalculation(meiXML, target.id);
                  if (b) beatStr = b.toMusicalString();
                }

                if (staffNum !== null && measureNum !== null) {
                  position = {
                    part: staffNum,
                    measure: measureNum,
                    beat: beatStr || "0",
                  };
                }
              } catch (err) {
                console.warn("Position calculation failed", err);
              }
            }
          }

          updateStyles(svg);
          onSelectionChange(newMode, newSelectedIds, position);
        };

        const handleClick = (e: MouseEvent) => {
          handleInteraction(e);
        };

        const handleContextMenu = (e: MouseEvent) => {
          e.preventDefault();
          handleInteraction(e);
          onContextMenu(e.clientX, e.clientY);
        };

        const handleMouseMove = (e: MouseEvent) => {
          const targetEl = e.target as Element;
          let target: Element | null = null;
          if (targetEl.classList.contains("hitbox")) {
            const corresp = targetEl.getAttribute("corresp");
            if (corresp) {
              const id = corresp.startsWith("#")
                ? corresp.substring(1)
                : corresp;
              target = svg.querySelector(`[id="${id}"]`);
            }
          } else {
            target = targetEl.closest(".staff, .note, .rest");
          }

          svg.querySelectorAll(".staff, .note, .rest").forEach((el) => {
            el.classList.remove("is-hovered");
          });
          if (target) {
            target.classList.add("is-hovered");
          }
        };

        svg.addEventListener("click", handleClick);
        svg.addEventListener("contextmenu", handleContextMenu);
        svg.addEventListener("mousemove", handleMouseMove);

        return () => {
          svg.removeEventListener("click", handleClick);
          svg.removeEventListener("contextmenu", handleContextMenu);
          svg.removeEventListener("mousemove", handleMouseMove);
        };
      }
    }
  }, [
    svgData,
    onSelectionChange,
    onContextMenu,
    injectHitboxes,
    updateStyles,
    meiXML,
  ]);

  return (
    <div
      style={{
        width: "100%",
        display: "flex",
        justifyContent: "flex-start",
        padding: "20px 0",
        minHeight: "500px",
        position: "relative",
      }}
    >
      <div
        ref={containerRef}
        style={{ width: "100%", backgroundColor: "var(--color-surface)" }}
      />
      {loading && showLoadingText && (
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            height: "100%",
            backgroundColor: "var(--color-surface)",
            zIndex: 10,
            display: "flex",
            alignItems: "flex-start",
            justifyContent: "center",
            paddingTop: "100px",
            color: "var(--color-text-muted)",
          }}
        >
          Loading...
        </div>
      )}
    </div>
  );
};

export default ScoreDisplay;
