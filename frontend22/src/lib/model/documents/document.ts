import type {
  DocumentId,
  DocumentOrigin,
  DocumentPath,
  DocumentType,
} from "./types";

export type Document = {
  id: DocumentId;
  path: DocumentPath;
  origin: DocumentOrigin;
  type: DocumentType;
  content: string; // Persistence format (Serialized XML/HTML)
};

export type DocumentRef = {
  id: DocumentId;
  path: DocumentPath;
  type: DocumentType;
};
