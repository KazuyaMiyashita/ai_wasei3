/**
 * AST Node Types for XHTML5+MEI
 */
export type AstNodeType = "element" | "text" | "mei" | "error" | "root";

export interface AstNode {
  /**
   * Internal UUID for reconciliation and stable rendering.
   */
  id: string;

  /**
   * Node type.
   */
  type: AstNodeType;

  /**
   * Tag name (for elements and mei).
   */
  tagName?: string;

  /**
   * XML attributes.
   */
  attributes?: Record<string, string>;

  /**
   * Child nodes.
   */
  children: AstNode[];

  /**
   * Text content or raw MEI content.
   */
  content?: string;

  /**
   * Source offset range in the XML string.
   */
  range: {
    start: number;
    end: number;
    contentStart?: number;
    contentEnd?: number;
  };

  /**
   * Parsing error information (only for type: 'error').
   */
  errorInfo?: string;
}

export interface ParseResult {
  root: AstNode;
  errors: Array<{
    message: string;
    line: number;
    column: number;
  }>;
}
