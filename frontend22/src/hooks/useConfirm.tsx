import { useConfirmContext } from "../context/ConfirmContext";

export type { ConfirmOptions, ConfirmResult } from "../context/ConfirmContext";

export function useConfirm() {
  return useConfirmContext();
}
