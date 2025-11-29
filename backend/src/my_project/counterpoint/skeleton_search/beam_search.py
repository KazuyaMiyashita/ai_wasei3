from collections.abc import Callable


class BeamSearch[T]:
    """
    ジェネリクス対応ビームサーチクラス
    候補生成とスコア計算を分離した設計
    """

    def __init__(
        self,
        length: int,
        get_candidates_func: Callable[[int], list[T]],
        node_score_func: Callable[[T, int], float],
        # これまでの履歴をもとに遷移のスコアを計算する
        transition_score_func: Callable[[list[T], T, int], float],
    ):
        """
        Args:
            length: 生成する列の長さ (N)
            get_candidates_func: (index) -> その時点で可能な候補のリスト
            node_score_func: (candidate, index) -> 候補単体のスコア
            transition_score_func: (history, candidate, index) -> 遷移スコア
        """
        self.length = length
        self.get_candidates = get_candidates_func
        self.get_node_score = node_score_func
        self.get_transition_score = transition_score_func

    def solve(self, beam_width: int = 100, top_k: int = 1) -> list[tuple[list[T], float]]:
        """
        ビームサーチを実行する

        Args:
            beam_width: 各ステップで残す候補の数
            top_k: 最終的に返す上位解の数

        Returns:
            (選択された候補のリスト, 合計スコア) のタプルのリスト
        """
        # (path_history, current_total_score)
        # 初期状態: 履歴なし, スコア0.0
        current_beams: list[tuple[list[T], float]] = [([], 0.0)]

        for i in range(self.length):
            # 1. 候補を取得 (評価はまだしない)
            candidates = self.get_candidates(i)
            next_beams: list[tuple[list[T], float]] = []

            # 現在のビーム(生き残りルート)それぞれに対して次を展開
            for path, prev_total_score in current_beams:
                for cand in candidates:
                    # --- スコア計算フェーズ ---
                    # A. ノード単体のスコア
                    node_score = self.get_node_score(cand, i)
                    # B. 遷移スコア
                    trans_score = 0.0
                    if i > 0:
                        trans_score = self.get_transition_score(path, cand, i)
                    # 足し合わせる
                    current_step_score = node_score + trans_score
                    # 禁則(-inf)チェック
                    if current_step_score == -float("inf"):
                        continue
                    # 合計スコア更新
                    new_total_score = prev_total_score + current_step_score
                    # 新しいパスを作成
                    new_path = [*path, cand]

                    next_beams.append((new_path, new_total_score))

            # 上位 beam_width 件に絞る
            if not next_beams:
                return []  # 候補が全滅した場合

            current_beams = sorted(next_beams, key=lambda x: x[1], reverse=True)[:beam_width]

        return current_beams[:top_k]


NoteType = int


def get_candidates_sample(i: int) -> list[NoteType]:
    # 純粋に「あり得る音」だけを返す
    return [60, 62, 64, 65, 67]


def score_node_sample(note: NoteType, i: int) -> float:
    # 音そのものの良し悪し (例: 特定の音を推奨するなど)
    if note == 60:
        return 1.0
    return 0.5


def score_transition_sample(history: list[NoteType], current: NoteType, i: int) -> float:
    # 遷移の良し悪し
    prev = history[-1]
    diff = abs(current - prev)

    # 禁則: 同音連打禁止
    if diff == 0:
        return -float("inf")

    # 跳躍が小さいほうが良い
    return -float(diff) * 0.1


def main() -> None:
    solver = BeamSearch[int](
        length=8,
        get_candidates_func=get_candidates_sample,
        node_score_func=score_node_sample,
        transition_score_func=score_transition_sample,
    )

    results = solver.solve(beam_width=10, top_k=3)

    for rank, (score, seq) in enumerate(results, 1):
        print(f"Rank {rank}: Score={score:.2f}, Seq={seq}")


if __name__ == "__main__":
    main()
