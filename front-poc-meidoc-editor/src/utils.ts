import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Tailwindのクラス名を条件付きで結合し、競合を解消するヘルパー関数
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
