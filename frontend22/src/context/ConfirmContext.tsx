import React, {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useState,
} from "react";
import {
  ConfirmDialog,
  type ConfirmDialogProps,
} from "../components/layout/ConfirmDialog";

export type ConfirmResult = "confirm" | "cancel" | "discard";

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  discardLabel?: string;
  variant?: "info" | "danger" | "warning";
}

interface ConfirmContextType {
  confirm: (options: ConfirmOptions) => Promise<ConfirmResult>;
}

const ConfirmContext = createContext<ConfirmContextType | undefined>(undefined);

export const ConfirmProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [dialogProps, setDialogProps] = useState<Omit<
    ConfirmDialogProps,
    "onConfirm" | "onCancel" | "onDiscard" | "isOpen"
  > | null>(null);
  const [resolveRef, setResolveRef] = useState<
    ((result: ConfirmResult) => void) | null
  >(null);

  const confirm = useCallback(
    (options: ConfirmOptions): Promise<ConfirmResult> => {
      return new Promise((resolve) => {
        setDialogProps(options);
        setResolveRef(() => resolve);
      });
    },
    [],
  );

  const handleClose = useCallback(
    (result: ConfirmResult) => {
      setDialogProps(null);
      if (resolveRef) {
        resolveRef(result);
        setResolveRef(null);
      }
    },
    [resolveRef],
  );

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {dialogProps && (
        <ConfirmDialog
          isOpen={true}
          title={dialogProps.title}
          message={dialogProps.message}
          confirmLabel={dialogProps.confirmLabel}
          cancelLabel={dialogProps.cancelLabel}
          discardLabel={dialogProps.discardLabel}
          variant={dialogProps.variant}
          onConfirm={() => handleClose("confirm")}
          onCancel={() => handleClose("cancel")}
          onDiscard={
            dialogProps.discardLabel ? () => handleClose("discard") : undefined
          }
        />
      )}
    </ConfirmContext.Provider>
  );
};

export function useConfirmContext() {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error("useConfirm must be used within a ConfirmProvider");
  }
  return context;
}
