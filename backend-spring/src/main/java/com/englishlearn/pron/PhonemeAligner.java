package com.englishlearn.pron;

import java.util.ArrayList;
import java.util.List;

/**
 * 音素序列对齐：目标音素（参考句）与实际音素（录音识别结果）做编辑距离对齐，
 * 逐个音素判定 读对 / 读错 / 漏读 / 多读。
 */
public final class PhonemeAligner {

    /** 对齐操作：ops ∈ {match, sub, del, ins} */
    public record Op(String op, int targetIndex, int hypIndex) {
    }

    private PhonemeAligner() {
    }

    public static List<Op> align(List<String> target, List<String> hyp) {
        int n = target.size();
        int m = hyp.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = target.get(i - 1).equals(hyp.get(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        List<Op> ops = new ArrayList<>();
        int i = n;
        int j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                int cost = target.get(i - 1).equals(hyp.get(j - 1)) ? 0 : 1;
                if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                    ops.add(new Op(cost == 0 ? "match" : "sub", i - 1, j - 1));
                    i--;
                    j--;
                    continue;
                }
            }
            if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                ops.add(new Op("del", i - 1, -1));
                i--;
            } else {
                ops.add(new Op("ins", -1, j - 1));
                j--;
            }
        }
        java.util.Collections.reverse(ops);
        return ops;
    }
}
