import type { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";

export function DocumentViewer({
  xhtml5meiDocument,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
}) {
  return (
    <div
      className="main-content h-full"
      // biome-ignore lint/security/noDangerouslySetInnerHtml: This is a preview component
      dangerouslySetInnerHTML={{
        __html: xhtml5meiDocument.rawContent,
      }}
    ></div>
  );
}
