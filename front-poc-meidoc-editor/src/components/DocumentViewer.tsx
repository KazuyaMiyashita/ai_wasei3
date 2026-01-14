import { useSyncExternalStore } from "react";
import type { EditorController } from "../lib/EditorController";

export function DocumentViewer({
  controller,
}: {
  controller: EditorController;
}) {
  useSyncExternalStore(
    (callback) => controller.subscribe(callback),
    () => controller.version,
  );

  return (
    /* 一番外側を DocumentEditor と一致させる */
    <div className="main-content h-full overflow-y-auto bg-white p-4">
      <div className="ProseMirror-menubar-wrapper">
        {/* メニューバーのプレースホルダー (実測値の26px) */}
        <div
          className="ProseMirror-menubar mb-2 border-b border-gray-200 bg-gray-50"
          style={{ minHeight: "26px" }}
        />

        {/* ProseMirror 側には class="ProseMirror ProseMirror-example-setup-style" というスタイルが付与されている。
         * しかしこの箇所に ProseMirror クラスを付与すると、空白のテキストノードの扱いか何かが異なり、大きくスタイルが変わってしまう。
         * そのためclassNameは付与しないのが一番レンダリング結果が近い。ただしMEIの楽譜があった箇所の下に ProseMirror 側では以下のような
         * 存在するため途中の幅が異なる
         *
         * <section>
         *   <p>
         *     <br class="ProseMirror-trailingBreak">
         *   </p>
         * </section>
         *
         * また ProseMirror 側では hrが <hr contenteditable="false" class=""> となっていて、見た目が異なる。
         *
         *  */}
        <div className="">
          <div
            // biome-ignore lint/security/noDangerouslySetInnerHtml: This is a preview component
            dangerouslySetInnerHTML={{
              __html: controller.document.rawContent,
            }}
          />
        </div>
      </div>
    </div>
  );
}
