import type React from "react";
import { useEffect, useRef } from "react";

export interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  discardLabel?: string; // Optional third option
  onConfirm: () => void;
  onCancel: () => void;
  onDiscard?: () => void;
  variant?: "info" | "danger" | "warning";
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = "OK",
  cancelLabel = "Cancel",
  discardLabel,
  onConfirm,
  onCancel,
  onDiscard,
  variant = "info",
}) => {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (isOpen) {
      dialogRef.current?.showModal();
    } else {
      dialogRef.current?.close();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div
        className="bg-surface-main border-border-main animate-in fade-in zoom-in-95 w-full max-w-sm rounded-lg border bg-white p-6 shadow-lg duration-200"
        role="dialog"
        aria-modal="true"
      >
        <h3 className="text-text-main mb-2 text-lg font-semibold">{title}</h3>
        <p className="text-text-sub mb-6 text-sm">{message}</p>

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancel}
            className="hover:bg-surface-hover text-text-main rounded px-4 py-2 text-sm font-medium transition-colors"
          >
            {cancelLabel}
          </button>

          {discardLabel && onDiscard && (
            <button
              type="button"
              onClick={onDiscard}
              className="text-error hover:bg-surface-hover rounded px-4 py-2 text-sm font-medium transition-colors"
            >
              {discardLabel}
            </button>
          )}

          <button
            type="button"
            onClick={onConfirm}
            className={`rounded px-4 py-2 text-sm font-bold text-white shadow-sm transition-colors ${
              variant === "danger"
                ? "bg-error hover:bg-red-600"
                : "bg-brand hover:bg-brand-hover"
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
