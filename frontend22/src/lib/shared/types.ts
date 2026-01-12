/**
 * 汎用的な型ユーティリティ。
 */
export type Branded<T, BrandName> = T & { readonly _brand: BrandName };
