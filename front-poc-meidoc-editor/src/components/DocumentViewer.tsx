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
    <div
      className="main-content h-full"
      // biome-ignore lint/security/noDangerouslySetInnerHtml: This is a preview component
      dangerouslySetInnerHTML={{
        __html: controller.document.rawContent,
      }}
    />
  );
}
