import type { Branded } from "../../shared/types";

export type DocumentId = Branded<string, "DocumentId">;
export type DocumentPath = Branded<string, "DocumentPath">;
export type DocumentOrigin = "workspace" | "resource";
export type DocumentType = "mei" | "xhtml5+mei";
