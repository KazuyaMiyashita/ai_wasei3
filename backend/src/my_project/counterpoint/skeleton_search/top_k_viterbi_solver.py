from collections.abc import Callable, Hashable


class TopKViterbiSolver[T: Hashable]:
    def __init__(
        self,
        length: int,
        get_candidates: Callable[[int], list[T]],
        get_node_score: Callable[[T, int], float],
        # 履歴(list[T])ではなく、直前のノード(T)だけを受け取る
        get_transition_score: Callable[[T, T, int], float],
    ):
        self.length = length
        self.get_candidates = get_candidates
        self.get_node_score = get_node_score
        self.get_transition_score = get_transition_score

    def solve(self, k: int) -> list[tuple[list[T], float]]:
        """
        厳密な上位k個のパスを返す
        """
        # 構造: list[ ステップごとのdict[ 現在のノードT, ランクごとのlist[ (前のノード, 前のランク) ] ] ]
        back_pointers: list[dict[T, list[tuple[T | None, int]]]] = [{} for _ in range(self.length)]
        current_k_scores: dict[T, list[float]] = {}

        # --- Step 0 ---
        candidates = self.get_candidates(0)
        for cand in candidates:
            score = self.get_node_score(cand, 0)
            current_k_scores[cand] = [score]
            # Backpointerの初期化 (Step0は親なし)
            back_pointers[0][cand] = [(None, -1)]

        # --- Step 1 to L-1 ---
        for i in range(1, self.length):
            next_k_scores: dict[T, list[float]] = {}
            candidates = self.get_candidates(i)
            prev_candidates = list(current_k_scores.keys())

            for curr_cand in candidates:
                node_score = self.get_node_score(curr_cand, i)
                incoming_paths = []

                for prev_cand in prev_candidates:
                    # 前のノードが持っているスコアリスト(最大k個)
                    for prev_rank_idx, prev_score in enumerate(current_k_scores[prev_cand]):
                        trans_score = self.get_transition_score(prev_cand, curr_cand, i)
                        if trans_score == -float("inf"):
                            continue

                        total_score = prev_score + trans_score + node_score
                        incoming_paths.append((total_score, prev_cand, prev_rank_idx))

                if incoming_paths:
                    # 上位k個選定
                    best_k = sorted(incoming_paths, key=lambda x: x[0], reverse=True)[:k]

                    # スコアとバックポインタを分離して保存
                    next_k_scores[curr_cand] = [x[0] for x in best_k]

                    # このノードのランク0はどこから来たか、ランク1はどこから来たか...を保存
                    back_pointers[i][curr_cand] = [(x[1], x[2]) for x in best_k]

            current_k_scores = next_k_scores
            if not current_k_scores:
                return []

        # --- 復元 ---
        final_candidates = []
        for node, scores in current_k_scores.items():
            for rank_idx, score in enumerate(scores):
                final_candidates.append((score, node, rank_idx))

        top_k_endings = sorted(final_candidates, key=lambda x: x[0], reverse=True)[:k]

        results = []
        for score, end_node, end_rank_idx in top_k_endings:
            path = [end_node]
            curr_node = end_node
            curr_rank = end_rank_idx

            # 後ろから前へ
            for i in range(self.length - 1, 0, -1):
                prev_node_uncast, prev_rank = back_pointers[i][curr_node][curr_rank]
                # back_pointers[0] には (None, -1) が入っているが、
                # このループは i=1 までしか回らないため back_pointers[0] の値は参照されない。
                # したがって、ここでの prev_node_uncast は常に T (Noneではない) である。
                assert prev_node_uncast is not None
                prev_node = prev_node_uncast
                path.append(prev_node)
                curr_node = prev_node
                curr_rank = prev_rank

            results.append((path[::-1], score))

        return results
