# Demonstration Guide

## Setup

1. Run the application: `pnpm dev`
2. Open `http://localhost:5173`

## Verification Steps

### 1. Whitespace Handling

- **Action**: Paste the following XML into the CodeMirror (Source) editor:
  ```xml
  <div>
    <p>
      Hello
      World
    </p>
  </div>
  ```
- **Check**: In the ProseMirror (WYSIWYG) editor, verifying that it displays as "Hello World" (or similar normalized text) without excessive indentation or newlines before "Hello".

### 2. Mark Synchronization

- **Action**: In the WYSIWYG editor, select the word "Hello".
- **Action**: Press `Ctrl+B` (or `Cmd+B`) or use the menu to apply **Bold**.
- **Check**: Look at the Source editor. It should update to `<p><strong>Hello</strong> World</p>` (or `<b>`).

### 3. Attribute Synchronization

- **Action**: In the Source editor, create a heading: `<h1>Title</h1>`.
- **Action**: In the WYSIWYG editor, verify it appears as a Heading 1.
- **Action**: Change it to Heading 2 using the menu (if available) or by editing text (if configured). Alternatively, use the browser console:
  ```js
  // Assuming view is accessible via global debug var or similar
  // view.dispatch(view.state.tr.setNodeMarkup(pos, null, { level: 2 }))
  ```
- **Check**: Source editor updates to `<h2>Title</h2>`.

### 4. Structural Edits

- **Action**: In the WYSIWYG editor, place cursor in the middle of a paragraph and press `Enter`.
- **Check**: Source editor shows the paragraph split into two `<p>` tags correctly.
