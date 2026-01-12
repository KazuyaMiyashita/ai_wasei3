import {
  ChevronDown,
  ChevronRight,
  FileText,
  Folder,
  Loader2,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useApplication } from "../../../context/ApplicationContext";
import type { ResourceEntry } from "../../../lib/api/client";
import type {
  DocumentId,
  DocumentOrigin,
} from "../../../lib/model/documents/types";

const ResourceItem: React.FC<{ entry: ResourceEntry; level: number }> = ({
  entry,
  level,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [children, setChildren] = useState<ResourceEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const application = useApplication();

  const handleToggle = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (entry.kind === "file") {
      const url = `/resources${entry.path}`;
      try {
        await application.open(url as DocumentId, "resource" as DocumentOrigin);
      } catch (err) {
        console.error(err);
      }
      return;
    }

    // Directory toggle
    if (!isOpen && !loaded) {
      setLoading(true);
      try {
        const entries = await application.resources.browse(entry.path);
        setChildren(entries);
        setLoaded(true);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    setIsOpen(!isOpen);
  };

  return (
    <div>
      {/* biome-ignore lint/a11y/useSemanticElements: interactive div for tree structure */}
      <div
        className="hover:bg-ui-bg-hover flex cursor-pointer items-center gap-1 truncate px-2 py-1 text-sm select-none"
        style={{ paddingLeft: `${level * 12 + 8}px` }}
        onClick={handleToggle}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            handleToggle(e as unknown as React.MouseEvent);
          }
        }}
        role="button"
        tabIndex={0}
      >
        {entry.kind === "directory" && (
          <span className="text-ui-text-muted">
            {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
          </span>
        )}
        {/* Spacer for files to align with folders that have chevron */}
        {entry.kind === "file" && <span className="w-[14px]" />}

        <span className="text-ui-text-muted">
          {entry.kind === "directory" ? (
            <Folder size={14} />
          ) : (
            <FileText size={14} />
          )}
        </span>
        <span className="text-ui-text-main truncate">{entry.name}</span>
        {loading && (
          <Loader2
            size={12}
            className="text-ui-text-muted ml-auto animate-spin"
          />
        )}
      </div>

      {isOpen && entry.kind === "directory" && (
        <div>
          {children.length === 0 && loaded && (
            <div
              className="text-ui-text-muted px-2 py-1 text-xs italic"
              style={{ paddingLeft: `${(level + 1) * 12 + 8}px` }}
            >
              Empty
            </div>
          )}
          {children.map((child) => (
            <ResourceItem key={child.path} entry={child} level={level + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

const Resources: React.FC = () => {
  const [entries, setEntries] = useState<ResourceEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const application = useApplication();

  useEffect(() => {
    const loadRoot = async () => {
      setLoading(true);
      try {
        const rootEntries = await application.resources.browse("");
        setEntries(rootEntries);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    loadRoot();
  }, [application]);

  return (
    <div className="ui-panel">
      <div className="ui-panel-section">
        <div className="ui-panel-title flex items-center justify-between pr-4">
          <span>Resources</span>
          {loading && <Loader2 size={14} className="animate-spin" />}
        </div>
        <div className="flex-1 overflow-y-auto">
          {entries.length === 0 && !loading && (
            <div className="text-ui-text-muted px-4 py-2 text-sm italic">
              Empty Root
            </div>
          )}
          {entries.map((entry) => (
            <ResourceItem key={entry.path} entry={entry} level={0} />
          ))}
        </div>
      </div>
    </div>
  );
};

export default Resources;
