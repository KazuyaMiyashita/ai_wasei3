export const CONFIG = {
  measureCount: 10,
  baseWidth: 300,
  partHeight: 110,
  startX: 130,
  startY: 20,
};

export type PartId = "SOPRANO" | "ALTO" | "TENOR" | "BASS";
export const PART_KEYS: PartId[] = ["SOPRANO", "ALTO", "TENOR", "BASS"];
export const PART_NAMES = ["Soprano", "Alto", "Tenor", "Bass"];
export const PART_NAME_MAP: Record<PartId, string> = {
  SOPRANO: "Soprano",
  ALTO: "Alto",
  TENOR: "Tenor",
  BASS: "Bass",
};
export const CLEF_MAP: Record<PartId, string> = {
  SOPRANO: "soprano",
  ALTO: "alto",
  TENOR: "tenor",
  BASS: "bass",
};
