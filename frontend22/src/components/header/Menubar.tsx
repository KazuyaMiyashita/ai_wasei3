import { ArrowBigUp, ChevronRight, Command } from "lucide-react";
import { Menubar as RadixMenubar } from "radix-ui";
import type * as React from "react";
import { useCallback, useRef } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";

const Trigger = ({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) => (
  <RadixMenubar.Trigger
    className={`text-ui-text-muted hover:text-brand-primary focus:text-brand-primary cursor-pointer rounded px-1 py-0.5 text-sm font-medium transition-colors outline-none select-none ${className}`}
  >
    {children}
  </RadixMenubar.Trigger>
);

const Content = ({ children }: { children: React.ReactNode }) => (
  <RadixMenubar.Portal>
    <RadixMenubar.Content
      className="bg-ui-bg-base border-ui-border animate-in fade-in zoom-in-95 min-w-48 rounded-md border p-1 shadow-md"
      align="start"
      sideOffset={8}
    >
      {children}
    </RadixMenubar.Content>
  </RadixMenubar.Portal>
);

const Item = ({
  children,
  onSelect,
  shortcut,
}: {
  children: React.ReactNode;
  onSelect?: () => void;
  shortcut?: React.ReactNode;
}) => (
  <RadixMenubar.Item
    onSelect={onSelect}
    className="hover:bg-ui-bg-hover text-ui-text-main data-highlighted:bg-ui-bg-hover flex cursor-pointer items-center justify-between rounded-sm px-2 py-1.5 text-xs outline-none"
  >
    <span>{children}</span>
    {shortcut && <span className="text-ui-text-muted ml-4">{shortcut}</span>}
  </RadixMenubar.Item>
);

const SubTrigger = ({ children }: { children: React.ReactNode }) => (
  <RadixMenubar.SubTrigger className="hover:bg-ui-bg-hover text-ui-text-main data-[state=open]:bg-ui-bg-hover data-highlighted:bg-ui-bg-hover flex cursor-pointer items-center justify-between rounded-sm px-2 py-1.5 text-xs outline-none">
    {children}
    <ChevronRight size={14} className="text-ui-text-muted ml-auto" />
  </RadixMenubar.SubTrigger>
);

const SubContent = ({ children }: { children: React.ReactNode }) => (
  <RadixMenubar.Portal>
    <RadixMenubar.SubContent
      className="bg-ui-bg-base border-ui-border animate-in fade-in zoom-in-95 min-w-40 rounded-md border p-1 shadow-md"
      sideOffset={2}
      alignOffset={-5}
    >
      {children}
    </RadixMenubar.SubContent>
  </RadixMenubar.Portal>
);

type MenuItemType =
  | "---"
  | string
  | {
      label: string;
      onSelect?: () => void;
      shortcut?: React.ReactNode;
      children?: MenuItemType[];
    };

const RenderMenuItems = ({ items }: { items: MenuItemType[] }) => (
  <>
    {items.map((item, i) => {
      if (item === "---")
        return (
          // biome-ignore lint/suspicious/noArrayIndexKey: separator has no unique id
          <RadixMenubar.Separator key={i} className="bg-ui-border my-1 h-px" />
        );

      if (typeof item === "object" && item.children) {
        return (
          <RadixMenubar.Sub key={item.label}>
            <SubTrigger>{item.label}</SubTrigger>
            <SubContent>
              <RenderMenuItems items={item.children} />
            </SubContent>
          </RadixMenubar.Sub>
        );
      }

      const label = typeof item === "string" ? item : item.label;
      const onSelect = typeof item === "object" ? item.onSelect : undefined;
      const shortcut = typeof item === "object" ? item.shortcut : undefined;

      return (
        <Item key={label} onSelect={onSelect} shortcut={shortcut}>
          {label}
        </Item>
      );
    })}
  </>
);

export default function Menubar() {
  const application = useApplication();
  const currentDocumentId = useApplicationState(
    (state) => state.currentDocumentId,
  );
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleNewProject = useCallback(() => {
    application.newProject();
  }, [application]);

  const handleSave = useCallback(async () => {
    if (currentDocumentId) {
      await application.save();
    } else {
      alert("No document to save.");
    }
  }, [currentDocumentId, application]);

  const handleSaveAs = useCallback(async () => {
    if (currentDocumentId) {
      const name = prompt("Save as (filename):");
      if (name) {
        const path = name.startsWith("/") ? name : `/${name}`;
        await application.saveAs(path);
      }
    } else {
      alert("No document to save.");
    }
  }, [currentDocumentId, application]);

  const handleExportWorkspace = useCallback(() => {
    application.workspaceManager.exportWorkspace();
  }, [application]);

  const handleImportClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleFileChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) {
        application.workspaceManager.importWorkspace(file);
      }
    },
    [application],
  );

  const handleDebug = useCallback((level: string) => {
    console.log(`Debug [${level}]: Notification triggered`);
  }, []);

  const menuData: { label: string; items: MenuItemType[] }[] = [
    {
      label: "File",
      items: [
        { label: "New Project", onSelect: handleNewProject },
        "Open File...",
        "---",
        {
          label: "Save",
          onSelect: handleSave,
          shortcut: (
            <div className="flex items-center gap-0.5">
              <Command size={12} />
              <span>S</span>
            </div>
          ),
        },
        {
          label: "Save As...",
          onSelect: handleSaveAs,
          shortcut: (
            <div className="flex items-center gap-0.5">
              <ArrowBigUp size={12} />
              <Command size={12} />
              <span>S</span>
            </div>
          ),
        },
        "---",
        { label: "Save Workspace (Export)", onSelect: handleExportWorkspace },
        { label: "Import Workspace", onSelect: handleImportClick },
      ],
    },
    { label: "Edit", items: ["Undo", "Redo"] },
    { label: "View", items: ["Zoom In", "Zoom Out"] },
    {
      label: "Tools",
      items: [
        "Generate Counterpoint",
        {
          label: "Debug Notification",
          children: [
            { label: "Info", onSelect: () => handleDebug("Info") },
            { label: "Warn", onSelect: () => handleDebug("Warn") },
            { label: "Error", onSelect: () => handleDebug("Error") },
          ],
        },
      ],
    },
  ];

  return (
    <RadixMenubar.Root className="flex items-center p-2">
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        style={{ display: "none" }}
        accept=".json"
      />
      <div className="flex gap-4">
        {menuData.map((menu) => (
          <RadixMenubar.Menu key={menu.label}>
            <Trigger>{menu.label}</Trigger>
            <Content>
              <RenderMenuItems items={menu.items} />
            </Content>
          </RadixMenubar.Menu>
        ))}
      </div>
    </RadixMenubar.Root>
  );
}
