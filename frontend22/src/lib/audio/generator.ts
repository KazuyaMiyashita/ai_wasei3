import type { PartData } from "./performer";
import { parsedSampleData } from "./sample-data";

/**
 * MEI楽譜データと選択された音符のIDから、再生用のPartData[]を生成します。
 * @param meiXML MEI形式のXMLドキュメント
 * @param noteId 選択された音符のxml:id
 * @returns 再生用データ
 */
export function generateAudioForNote(
  _meiXML: Document,
  _noteId: string,
): PartData[] {
  // 現時点ではダミー実装としてサンプルデータを返します。
  // 将来的にmeiDataをパースし、noteIdに対応する音高やアーティキュレーションを
  // 考慮したデータを生成するロジックをここに実装します。
  // console.log(`Generating audio for note: ${noteId}`);
  return parsedSampleData;
}
