from collections.abc import Callable, Hashable


class ViterbiSolver[T: Hashable]:
    def __init__(
        self,
        length: int,
        get_candidates_func: Callable[[int], list[T]],  # T must not be None.
        node_score_func: Callable[[T, int], float],
        # 履歴(list[T])ではなく、直前のノード(T)だけを受け取る
        transition_score_func: Callable[[T, T, int], float],
    ):
        self.length = length
        self.get_candidates = get_candidates_func
        self.get_node_score = node_score_func
        self.get_transition_score = transition_score_func

    def solve(self) -> tuple[float, list[T]]:
        """
        全探索の中で最もスコアが高いパスを1つ返す（厳密解）
        """
        # DPテーブル: dp[step][curr_cand] = max_score_to_reach_here
        # キーをTそのものにするため、Tはハッシュ化可能(hashable)である必要があります
        # (intやtupleならOK。自作クラスなら __hash__ が必要)
        current_scores: dict[T, float] = {}

        # 経路復元用: back_pointers[step][curr_cand] = prev_cand
        back_pointers: list[dict[T, T]] = [{} for _ in range(self.length)]

        # --- Step 0 (初期化) ---
        candidates = self.get_candidates(0)
        for cand in candidates:
            # Step0は遷移スコアがないのでノードスコアのみ
            current_scores[cand] = self.get_node_score(cand, 0)

        # --- Step 1 to L-1 ---
        for i in range(1, self.length):
            next_scores: dict[T, float] = {}
            candidates = self.get_candidates(i)
            prev_candidates = list(current_scores.keys())

            # 今のステップの各候補について、「どこから来るのが最強か」を探る
            for curr_cand in candidates:
                best_score = -float("inf")
                best_prev = None

                # 事前計算: ノード自体のスコア
                node_score = self.get_node_score(curr_cand, i)

                # 前のステップの全候補からの遷移を試す
                for prev_cand in prev_candidates:
                    prev_score = current_scores[prev_cand]

                    # 遷移スコア (直前 -> 今 だけを見る)
                    trans_score = self.get_transition_score(prev_cand, curr_cand, i)

                    # 禁則チェック
                    if trans_score == -float("inf"):
                        continue

                    total_score = prev_score + trans_score + node_score

                    # 最大値を更新
                    if total_score > best_score:
                        best_score = total_score
                        best_prev = prev_cand

                # 有効な経路があった場合のみ保存
                if best_score > -float("inf") and best_prev is not None:
                    next_scores[curr_cand] = best_score
                    back_pointers[i][curr_cand] = best_prev

            # スコアテーブルを更新
            current_scores = next_scores

            # 生存ルートがなくなったら終了
            if not current_scores:
                return (-float("inf"), [])

        # --- 経路復元 (Backtracking) ---
        # 最終ステップで最もスコアが高い候補を探す。
        # current_scores は空でないことが保証されているため、maxは安全に実行できる。
        best_last_node = max(current_scores, key=lambda k: current_scores[k])
        max_total_score = current_scores[best_last_node]

        # 後ろから前に辿る
        path = [best_last_node]
        curr = best_last_node
        for i in range(self.length - 1, 0, -1):
            prev = back_pointers[i][curr]
            path.append(prev)
            curr = prev

        # 逆順になっているので反転
        return (max_total_score, path[::-1])
