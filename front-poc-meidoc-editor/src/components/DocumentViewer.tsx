import type { XHTML5MEIDocument } from "./Main";

export function DocumentViewer({
  xhtml5meiDocument,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
}) {
  return (
    <div
      className="h-full main-content"
      dangerouslySetInnerHTML={{
        __html: xhtml5meiDocument.rawContent,
      }}
    ></div>
  );
}
