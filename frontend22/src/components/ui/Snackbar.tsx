import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { cn } from "../../utils";

interface SnackbarProps {
  message: string;
  isOpen: boolean;
  onClose: () => void;
}

export function Snackbar({ message, isOpen, onClose }: SnackbarProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setVisible(true);
      const timer = setTimeout(() => {
        onClose();
      }, 5000);
      return () => clearTimeout(timer);
    }
    const timer = setTimeout(() => setVisible(false), 300); // Wait for fade out
    return () => clearTimeout(timer);
  }, [isOpen, onClose]);

  if (!isOpen && !visible) return null;

  return (
    <div
      className={cn(
        "fixed top-4 left-1/2 z-50 flex w-full max-w-md -translate-x-1/2 items-center justify-between rounded-lg px-4 py-3 shadow-2xl transition-all duration-300",
        "bg-gray-900 text-white", // Black background as requested
        isOpen ? "translate-y-0 opacity-100" : "-translate-y-4 opacity-0",
      )}
    >
      <span className="text-sm font-medium">{message}</span>
      <button
        type="button"
        onClick={onClose}
        className="ml-4 rounded p-1 hover:bg-white/20"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
