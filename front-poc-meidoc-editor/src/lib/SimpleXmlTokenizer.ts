/**
 * SimpleXmlTokenizer.ts
 *
 * A lossless tokenizer for XML-like structures.
 * It preserves exact whitespace, quote styles, and handles incomplete tags gracefully.
 */

export type TokenType =
  | "Text"
  | "OpenTag"
  | "CloseTag"
  | "Comment" // <!-- ... -->
  | "CDATA" // <![CDATA[ ... ]]>
  | "ProcessingInstruction"; // <? ... ?>

export interface Token {
  type: TokenType;
  content: string; // The exact raw string of the token
  tagName?: string; // Extracted for tags
  isSelfClosing?: boolean; // For OpenTag
}

export class SimpleXmlTokenizer {
  private input: string;
  private pos: number = 0;

  constructor(input: string) {
    this.input = input;
  }

  tokenize(): Token[] {
    const tokens: Token[] = [];
    while (this.pos < this.input.length) {
      const char = this.input[this.pos];

      if (char === "<") {
        // Potential tag or special instruction
        const nextChar = this.input[this.pos + 1];

        if (nextChar === "/") {
          // Closing Tag </...
          tokens.push(this.readCloseTag());
        } else if (nextChar === "!") {
          // Comment or CDATA or DOCTYPE
          if (this.input.startsWith("<!--", this.pos)) {
            tokens.push(this.readComment());
          } else if (this.input.startsWith("<![CDATA[", this.pos)) {
            tokens.push(this.readCDATA());
          } else {
            // Treat DOCTYPE or others as special or just generic tag-like
            // For now, let's treat generic <! ... > as ProcessingInstruction-ish or OpenTag
            tokens.push(this.readProcessingInstructionOrDoctype());
          }
        } else if (nextChar === "?") {
          // Processing Instruction <? ... ?>
          tokens.push(this.readProcessingInstructionOrDoctype());
        } else {
          // Opening Tag <name ...
          tokens.push(this.readOpenTag());
        }
      } else {
        // Text content
        tokens.push(this.readText());
      }
    }
    return tokens;
  }

  private readText(): Token {
    let content = "";
    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      if (char === "<") break; // Stop at start of next tag
      content += char;
      this.pos++;
    }
    return { type: "Text", content };
  }

  private readOpenTag(): Token {
    // Scan until > but ignore > inside quotes
    let content = "<";
    this.pos++; // Skip <

    let tagName = "";
    // Read tag name
    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      if (/\s/.test(char) || char === ">" || char === "/") break;
      tagName += char;
      content += char;
      this.pos++;
    }

    // Read attributes and rest until >
    let inQuote: null | "'" | '"' = null;
    let isSelfClosing = false;

    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      content += char;
      this.pos++;

      if (inQuote) {
        if (char === inQuote) inQuote = null;
      } else {
        if (char === "'" || char === '"') {
          inQuote = char;
        } else if (char === ">") {
          break; // End of tag
        } else if (char === "/" && this.input[this.pos] === ">") {
          // Self closing indicator found, wait for >
          isSelfClosing = true;
        }
      }
    }

    // Check if it was really self-closing (ends with />)
    if (content.endsWith("/>")) isSelfClosing = true;

    return { type: "OpenTag", content, tagName, isSelfClosing };
  }

  private readCloseTag(): Token {
    let content = "</";
    this.pos += 2; // Skip </

    let tagName = "";
    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      if (char === ">" || /\s/.test(char)) break;
      tagName += char;
      content += char;
      this.pos++;
    }

    // Skip garbage until >
    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      content += char;
      this.pos++;
      if (char === ">") break;
    }

    return { type: "CloseTag", content, tagName };
  }

  private readComment(): Token {
    // <!-- ... -->
    let content = "";
    while (this.pos < this.input.length) {
      if (this.input.startsWith("-->", this.pos)) {
        content += "-->";
        this.pos += 3;
        break;
      }
      content += this.input[this.pos];
      this.pos++;
    }
    return { type: "Comment", content };
  }

  private readCDATA(): Token {
    // <![CDATA[ ... ]]>
    let content = "";
    while (this.pos < this.input.length) {
      if (this.input.startsWith("]]>", this.pos)) {
        content += "]]>";
        this.pos += 3;
        break;
      }
      content += this.input[this.pos];
      this.pos++;
    }
    return { type: "CDATA", content };
  }

  private readProcessingInstructionOrDoctype(): Token {
    // <? ... ?> or <!DOCTYPE ... >
    // Simple scan until > (handling quotes)
    // For simplicity, treat same as OpenTag logic but distinct type
    // This assumes they don't contain unquoted > in weird places except within strings
    let content = "";
    let inQuote: null | "'" | '"' = null;

    while (this.pos < this.input.length) {
      const char = this.input[this.pos];
      content += char;
      this.pos++;

      if (inQuote) {
        if (char === inQuote) inQuote = null;
      } else {
        if (char === "'" || char === '"') {
          inQuote = char;
        } else if (char === ">") {
          break;
        }
      }
    }
    return { type: "ProcessingInstruction", content };
  }
}
