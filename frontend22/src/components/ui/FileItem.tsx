import { ChevronDown, ChevronRight, FileMusic, Folder } from "lucide-react";

import { cn } from "../../utils";

const FileItem: React.FC<{
  name: string;
  type: "file" | "folder";
  depth?: number;
  isOpen?: boolean;
}> = ({ name, type, depth = 0, isOpen = false }) => {
  return (
    <button
      type="button"
      className={cn("ui-list-item", depth > 0 && "pl-6")}
      style={{ paddingLeft: `${(depth + 1) * 0.5}rem` }}
    >
      {type === "folder" &&
        (isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />)}
      {type === "file" && <span className="w-3.5" />}
      {type === "folder" ? (
        <Folder size={16} className="text-blue-400" />
      ) : (
        <FileMusic size={16} className="text-gray-400" />
      )}
      <span className="truncate">{name}</span>
    </button>
  );
};

export default FileItem;
