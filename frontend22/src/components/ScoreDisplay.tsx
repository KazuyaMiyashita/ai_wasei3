import type React from "react";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  getBeatByXmlCalculation,
  getMeasureNumberById,
  getStaffNumberById,
} from "../lib/mei-utils";

export interface ScorePosition {
  part: number;
  measure: number;
  beat: string;
}

interface ScoreDisplayProps {
  loading: boolean;
  svgData: string | undefined;
  meiXML: Document | undefined;
  onSelectionChange: (
    mode: "none" | "note" | "staff",
    selectedIds: string[],
    position?: ScorePosition,
  ) => void;
  onContextMenu: (x: number, y: number) => void;
}

// Helper functions outside component
const collectNoteX = (svgElement: SVGSVGElement): [string, SVGRect][] => {
  const results: [string, SVGRect][] = [];
  const notes = svgElement.querySelectorAll(".note");
  notes.forEach((note) => {
    const notehead = note.querySelector(".notehead");
    if (notehead && note.id) {
      results.push([note.id, (notehead as SVGGraphicsElement).getBBox()]);
    }
  });
  const rests = svgElement.querySelectorAll(".rest");
  rests.forEach((rest) => {
    if (rest.id) {
      results.push([rest.id, (rest as SVGGraphicsElement).getBBox()]);
    }
  });
  return results;
};

const getStaffYRange = (svgElement: SVGSVGElement) => {
  const staves = svgElement.querySelectorAll(".staff");
  let minY = Infinity;
  let maxY = -Infinity;
  staves.forEach((staff) => {
    const bbox = (staff as SVGGraphicsElement).getBBox();
    minY = Math.min(minY, bbox.y);
    maxY = Math.max(maxY, bbox.y + bbox.height);
  });
  return { top: minY, bottom: maxY };
};

const ScoreDisplay: React.FC<ScoreDisplayProps> = ({
  loading,
  svgData,
  meiXML,
  onSelectionChange,
  onContextMenu,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const selectedElements = useRef<Set<Element>>(new Set());
  const selectionMode = useRef<"none" | "note" | "staff">("none");
  const noteXMap = useRef<Map<number, string>>(new Map());

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

  // --- 2. 描画・生成ロジック ---

  const injectHitboxes = useCallback((svgElement: SVGSVGElement) => {
    // 既存のhitboxをクリア（再レンダリング時など）
    svgElement.querySelectorAll(".hitbox").forEach((el) => {
      el.remove();
    });

    const staves = svgElement.querySelectorAll(".staff");
    staves.forEach((staff) => {
      const staffEl = staff as SVGGraphicsElement;
      let staffRect: SVGRect | null = null;

      // StaffのBBox計算 (Staff lines)
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
        // Staffの場合は上下に少しパディングを持たせる
        minY -= 100;
        maxY += 100; // パディング調整 (height += 200 equivalent)

        staffRect = {
          x: minX,
          y: minY,
          width: maxX - minX,
          height: maxY - minY,
        } as SVGRect;
      }

      // Note hitboxes collection
      const noteHitboxes: Element[] = [];
      const notes = staffEl.querySelectorAll(".note");

      notes.forEach((note) => {
        const notehead = note.querySelector(".notehead");
        if (!notehead) return;

        const noteheadEl = notehead as SVGGraphicsElement;
        try {
          const bbox = noteheadEl.getBBox();
          const cx = bbox.x + bbox.width / 2;
          const cy = bbox.y + bbox.height / 2;
          const r = Math.max(bbox.width, bbox.height) / 2 + 150; // パディングを含める

          if (r > 0) {
            const circle = document.createElementNS(
              "http://www.w3.org/2000/svg",
              "circle",
            );
            circle.setAttribute("cx", cx.toString());
            circle.setAttribute("cy", cy.toString());
            circle.setAttribute("r", r.toString());
            circle.setAttribute("class", "hitbox");
            // IDがなければ生成するか、既存のIDを使用
            if (note.id) {
              circle.setAttribute("corresp", `#${note.id}`); // ID参照 (#付き)
            }
            noteHitboxes.push(circle);
          }
        } catch (e) {
          console.warn("Note bbox failed", e);
        }
      });

      // Rest hitboxes collection
      const rests = staffEl.querySelectorAll(".rest");
      rests.forEach((rest) => {
        const restEl = rest as SVGGraphicsElement;
        try {
          const bbox = restEl.getBBox();
          const cx = bbox.x + bbox.width / 2;
          const cy = bbox.y + bbox.height / 2;
          const r = Math.max(bbox.width, bbox.height) / 2 + 150; // パディングを含める

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

      // Staff hitbox creation
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
        rect.setAttribute("corresp", `#${staff.id}`); // ID参照 (#付き)

        // Append Staff hitbox FIRST
        staff.appendChild(rect);
      }

      // Append Note/Rest hitboxes AFTER Staff hitbox (so they are on top)
      noteHitboxes.forEach((rect) => {
        staff.appendChild(rect);
      });
    });
  }, []);

  const drawAllPlaybackCursors = useCallback(
    (svgElement: SVGSVGElement) => {
      const targetContainer = svgElement.querySelector(".page-margin");
      if (targetContainer) {
        const { top, bottom } = getStaffYRange(svgElement);
        const noteCoords = collectNoteX(svgElement);

        // X座標をキーにして、最初に見つかったIDを保持するMapを作成
        const xToIdMap = new Map<number, string>();
        noteCoords.forEach(([id, bbox]) => {
          const x = bbox.x + bbox.width / 2;
          // 同じX座標（和音など）の場合は、最初の1つだけ保持すれば小節特定には十分
          if (!xToIdMap.has(x)) {
            xToIdMap.set(x, id);
          }
        });

        // refを更新
        noteXMap.current = xToIdMap;

        let cursorGroup = targetContainer.querySelector("#cursor-group");
        if (cursorGroup) cursorGroup.remove();

        cursorGroup = document.createElementNS(
          "http://www.w3.org/2000/svg",
          "g",
        );
        cursorGroup.setAttribute("id", "cursor-group");
        cursorGroup.setAttribute("pointer-events", "none");

        // X座標の昇順でループ（任意）
        const sortedX = Array.from(xToIdMap.keys()).sort((a, b) => a - b);

        sortedX.forEach((x) => {
          const representativeId = xToIdMap.get(x);
          if (!representativeId) return;

          const measureNum = meiXML
            ? getMeasureNumberById(meiXML, representativeId)
            : null;

          const line = document.createElementNS(
            "http://www.w3.org/2000/svg",
            "line",
          );
          line.setAttribute("x1", x.toString());
          line.setAttribute("y1", top.toString());
          line.setAttribute("x2", x.toString());
          line.setAttribute("y2", bottom.toString());
          line.setAttribute("stroke", "rgba(255, 0, 0, 0)"); // デバッグ用に表示する際は第4引数を0.4程度にする
          line.setAttribute("stroke-width", "30");
          line.setAttribute("class", "playback-cursor-line");

          // 後で小節ごとに制御したい場合、データ属性に入れておくと便利
          if (measureNum !== null) {
            line.setAttribute("data-measure", measureNum.toString());
          }

          cursorGroup?.appendChild(line);
        });

        targetContainer.appendChild(cursorGroup);
      }
    },
    [meiXML],
  );

  // --- 3. 選択・スタイル制御 ---

  const updateStyles = useCallback((svg: SVGSVGElement) => {
    // まず全てのhitboxから選択クラスを除去
    svg.querySelectorAll(".hitbox").forEach((el) => {
      el.classList.remove("is-selected");
    });

    // 選択された要素に対応するhitboxにクラスを付与
    selectedElements.current.forEach((el) => {
      if (el.id) {
        // correspが ID または #ID のhitboxを探す
        // CSS.escapeでIDのエスケープ処理（念のため）
        const hitbox =
          svg.querySelector(`.hitbox[corresp="${CSS.escape(el.id)}"]`) ||
          svg.querySelector(`.hitbox[corresp="#${CSS.escape(el.id)}"]`);
        if (hitbox) {
          hitbox.classList.add("is-selected");
        }
      }
    });
  }, []);

  // --- 4. メイン副作用 (SVG描画) ---

  useEffect(() => {
    if (containerRef.current && svgData) {
      // SVGデータが更新されたら、古いDOM要素への参照をクリアする
      selectedElements.current.clear();

      containerRef.current.innerHTML = svgData;
      const svg = containerRef.current.querySelector("svg");
      if (svg) {
        // 2. ヒットボックス生成
        injectHitboxes(svg);
        // 3. 再生カーソル描画
        drawAllPlaybackCursors(svg);

        const handleInteraction = (e: MouseEvent) => {
          e.stopPropagation();
          const targetEl = e.target as Element;
          let target: Element | null = null;

          if (targetEl.classList.contains("hitbox")) {
            const corresp = targetEl.getAttribute("corresp");
            if (corresp) {
              // IDに#がついている場合とついていない場合を考慮
              const id = corresp.startsWith("#")
                ? corresp.substring(1)
                : corresp;
              // SVG内から要素を探す (MEI IDとSVG IDが一致している前提)
              // Verovio SVG output matches xml:id to id
              target = svg.querySelector(`[id="${id}"]`);
            }
          } else {
            // Fallback for non-hitbox clicks (if any)
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

            // 右クリックで、かつ既に選択されている要素の上であれば選択状態を変更しない
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
              // 選択状態を維持する場合でも、モード変数は念のため更新（通常は変わらないはず）
              selectionMode.current = targetType;
            }

            newMode = selectionMode.current;
            newSelectedIds = Array.from(selectedElements.current)
              .map((el) => el.id)
              .filter((id) => !!id);

            // 小節(staff)クリック時、あるいは音符(note)クリック時もPositionを計算する
            if (meiXML && target.id) {
              try {
                // 1. Staff Number の特定
                const staffNum = getStaffNumberById(meiXML, target.id);

                // 2. Measure & Beat の特定
                let measureNum: number | null = null;
                let beatStr = "";

                if (targetType === "staff") {
                  // 同じシステム内の全パートの音符を対象にする
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

                    // 音符の中心座標 (Screen Coordinate)
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
                  // Note click
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
    drawAllPlaybackCursors,
    updateStyles,
    meiXML,
  ]);

  // 外側クリック解除ロジック
  useEffect(() => {
    const handleDocClick = () => {
      // リセット
      if (selectedElements.current.size > 0) {
        selectedElements.current.clear();
        selectionMode.current = "none";
        // SVG要素への参照がないとクラスを消せない...
        if (containerRef.current) {
          const svg = containerRef.current.querySelector("svg");
          if (svg) updateStyles(svg);
        }
        onSelectionChange("none", []);
      }
    };

    document.addEventListener("click", handleDocClick);
    return () => document.removeEventListener("click", handleDocClick);
  }, [onSelectionChange, updateStyles]);

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
