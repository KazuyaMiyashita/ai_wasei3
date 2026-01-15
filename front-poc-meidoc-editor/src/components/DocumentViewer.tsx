import React, { memo, useSyncExternalStore } from "react";
import type { EditorController } from "../lib/EditorController";
import type { ResilientNode } from "../lib/ResilientSyntaxTree";

const RSTNodeView = memo(({ node }: { node: ResilientNode }) => {
  if (node.type === "Text") {
    return <>{node.textContent}</>;
  }

  if (node.type === "Error") {
    return (
      <div
        className="parse-error border border-red-500 bg-red-50 p-1 text-red-700"
        data-error={node.errorMessage}
      >
        {node.textContent}
      </div>
    );
  }

  const tagName = node.tagName || "div";

  if (tagName === "mei") {
    return <pre className="mei-content">{node.toString()}</pre>;
  }

  const props: Record<string, string> = { ...node.attributes };

  // React uses 'className' instead of 'class'
  if (props.class) {
    props.className = props.class;
    delete props.class;
  }

  // Handle void tags
  if (["br", "img", "hr"].includes(tagName)) {
    return React.createElement(tagName, props);
  }

  const children = node.children.map((child) => (
    <RSTNodeView key={child.id} node={child} />
  ));

  return React.createElement(tagName, props, children);
});

export function DocumentViewer({
  controller,
}: {
  controller: EditorController;
}) {
  useSyncExternalStore(
    (callback) => controller.subscribe(callback),
    () => controller.version,
  );

  const bodyNode = controller.rst.getBodyNode();

  return (
    /* 一番外側を DocumentEditor と一致させる */
    <div className="main-content h-full overflow-y-auto bg-white p-4">
      <div className="ProseMirror-menubar-wrapper">
        {/* メニューバーのプレースホルダー (実測値の26px) */}
        <div
          className="ProseMirror-menubar mb-2 border-b border-gray-200 bg-gray-50"
          style={{ minHeight: "26px" }}
        />
        <div className="">
          {bodyNode ? (
            bodyNode.children.map((child) => (
              <RSTNodeView key={child.id} node={child} />
            ))
          ) : (
            <div className="text-red-500">No body element found</div>
          )}
        </div>
      </div>
    </div>
  );
}
