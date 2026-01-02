import type React from "react";

const NotePalette: React.FC = () => {
  return (
    <div className="bg-sidebar text-text-main flex h-full w-full flex-col overflow-hidden">
      <div className="border-border-main bg-surface-header/50 text-text-sub flex h-11 shrink-0 items-center border-b px-3 text-[10px] font-bold tracking-wider uppercase">
        Note Palette
      </div>
      <div className="text-text-muted p-4 text-center text-xs italic">
        (Coming soon: Note editing tools)
      </div>
    </div>
  );
};

export default NotePalette;
