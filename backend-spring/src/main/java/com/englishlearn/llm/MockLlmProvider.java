package com.englishlearn.llm;

import com.englishlearn.entity.ConversationMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 规则式占位 LLM Provider（对齐原 FastAPI MockProvider）。
 * 未接入真实大模型前按关键词返回预设回复与启发式评分。
 */
public class MockLlmProvider implements LlmProvider {

    private static final Random RANDOM = new Random();

    private static final String[][] GENERIC_REPLIES = {
            {"I see. That is really interesting. Could you tell me a bit more?", "我明白了，这很有意思。你能再多说一点吗？"},
            {"Good point! What would you do in that situation?", "说得好！那种情况下你会怎么做呢？"},
            {"Thanks for sharing. What happened next?", "谢谢分享。接下来发生了什么？"},
            {"That sounds great. How often do you practice English?", "听起来不错。你多久练一次英语呢？"},
            {"I totally get it. Could you give me an example?", "完全理解。你能举个例子吗？"},
    };

    private record KeywordReply(List<String> keywords, String en, String zh) {}

    private static final List<KeywordReply> KEYWORD_REPLIES = List.of(
            new KeywordReply(List.of("restaurant", "menu", "order", "food", "eat", "dish", "delicious"),
                    "Sure! I would recommend our grilled salmon — it is today's special. Would you like anything to drink?",
                    "好的！我推荐我们的香煎三文鱼，是今天的特色菜。你想喝点什么吗？"),
            new KeywordReply(List.of("meeting", "project", "report", "deadline", "schedule", "plan"),
                    "Absolutely, let's keep the meeting focused. Shall we review the project timeline first?",
                    "当然，我们让会议聚焦一些。先回顾一下项目时间表好吗？"),
            new KeywordReply(List.of("check-in", "boarding", "luggage", "flight", "airport", "gate"),
                    "Of course. May I see your passport and booking reference, please? Your luggage will be checked to the final destination.",
                    "好的。请出示您的护照和订票号。您的行李将直接托运到最终目的地。"),
            new KeywordReply(List.of("hotel", "room", "reservation", "check in", "key", "breakfast"),
                    "Welcome! You have a confirmed reservation. Here is your room key, and breakfast is served from 7 to 10.",
                    "欢迎光临！您有确认的预订。这是您的房卡，早餐供应时间是 7 点到 10 点。"),
            new KeywordReply(List.of("interview", "experience", "skill", "job", "work"),
                    "Great answer. Could you describe a challenge you overcame at work, and what you learned from it?",
                    "回答得很好。能描述一个你在工作中克服的挑战，以及你从中学会了什么吗？"),
            new KeywordReply(List.of("class", "homework", "teacher", "question", "study", "lesson"),
                    "That's a good question for today's discussion. Let's open it up — what do the rest of you think?",
                    "这是今天讨论中的好问题。我们开放一下——大家觉得呢？"),
            new KeywordReply(List.of("hello", "hi ", "hey", "good morning", "good afternoon", "nice to meet"),
                    "Hello! It's great to practice with you today. How are you doing?",
                    "你好！今天和你一起练习很开心。你最近怎么样？"),
            new KeywordReply(List.of("thank", "thanks"),
                    "You are very welcome! Is there anything else I can help you with?",
                    "不客气！还有什么我可以帮你的吗？"),
            new KeywordReply(List.of("bye", "goodbye", "see you", "that's all", "that is all"),
                    "It was a pleasure talking with you. See you next time!",
                    "和你聊天很愉快。下次见！")
    );

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public Reply opening(String sceneName, String sceneDesc, String role) {
        Map<String, String[]> openers = Map.of(
                "餐厅点餐", new String[]{"Good evening! Welcome to our restaurant. How many people are in your party?", "晚上好！欢迎光临本餐厅，请问一共几位？"},
                "商务会议", new String[]{"Good morning, everyone. Let's get started. The first item is our Q3 project review. Who would like to share the update?", "大家早上好，我们开始吧。第一项是第三季度项目回顾，谁来分享一下进展？"},
                "机场值机", new String[]{"Hello, welcome to check-in. May I see your passport, please?", "您好，欢迎办理值机手续，请出示您的护照。"},
                "课堂讨论", new String[]{"Morning, class! Today we will talk about how technology changes our life. Does anyone have a thought?", "同学们早！今天我们聊聊科技如何改变生活。有谁想说说的吗？"},
                "酒店入住", new String[]{"Welcome to our hotel. Do you have a reservation with us today?", "欢迎光临我们酒店，请问您今天有预订吗？"},
                "面试问答", new String[]{"Thank you for coming. To begin, could you briefly introduce yourself?", "感谢你来参加面试。首先，请简单介绍一下自己。"}
        );
        String[] hit = openers.get(sceneName);
        if (hit != null) {
            return new Reply(hit[0], hit[1]);
        }
        String en = "Hello! I am " + role + ". Let's start our conversation about " + sceneName + ". How are you today?";
        String zh = "你好！我是" + role + "。让我们开始关于「" + sceneName + "」的对话吧，今天过得怎么样？";
        return new Reply(en, zh);
    }

    @Override
    public Reply reply(String sceneName, String sceneDesc, String role, List<ConversationMessage> history, String userInput) {
        String text = (userInput == null ? "" : userInput).strip().toLowerCase();
        for (KeywordReply kr : KEYWORD_REPLIES) {
            if (kr.keywords().stream().anyMatch(text::contains)) {
                return new Reply(kr.en(), kr.zh());
            }
        }
        String[] pair = GENERIC_REPLIES[RANDOM.nextInt(GENERIC_REPLIES.length)];
        return new Reply(pair[0], pair[1]);
    }

    @Override
    public EvalResult evaluate(String userInput) {
        String[] words = (userInput == null ? "" : userInput).split("[^A-Za-z']+");
        int wc = 0;
        for (String w : words) {
            if (!w.isBlank()) wc++;
        }
        double base = Math.min(88, 62 + wc * 2.0);
        double noise = uniform(-4, 4);
        double pron = clamp(base + noise, 55, 96);
        double fluency = clamp(base + (wc >= 6 ? 0 : -6) + uniform(-3, 3), 55, 96);
        double reaction = clamp(base + 4 + uniform(-3, 3), 55, 96);
        double natural = clamp(base + uniform(-5, 5), 55, 96);
        pron = round1(pron);
        fluency = round1(fluency);
        reaction = round1(reaction);
        natural = round1(natural);

        List<Map<String, Object>> issues = new ArrayList<>();
        String lowered = (userInput == null ? "" : userInput).toLowerCase();
        if (lowered.contains("would")) {
            issues.add(Map.of("word", "would", "phoneme", "/wʊd/", "note", "注意 /wʊd/ 的短元音与连读"));
        }
        if (lowered.contains("the")) {
            issues.add(Map.of("word", "the", "phoneme", "/ðə/", "note", "th 需舌尖轻触上齿"));
        }
        String grammar = "I noticed a small issue, but your meaning was clear. Keep speaking naturally.";
        if (wc >= 3) {
            grammar = "Good sentence structure overall. Try to keep your sentences connected.";
        }
        String better = null;
        if (lowered.contains("want to") && wc >= 2) {
            better = "You can say: \"I'd like to...\" to sound more polite.";
        }

        EvalResult r = new EvalResult();
        r.pron = pron;
        r.fluency = fluency;
        r.reaction = reaction;
        r.natural = natural;
        r.grammarFeedback = grammar;
        r.phonemeIssues = issues;
        r.betterExpression = better;
        return r;
    }

    @Override
    public SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations) {
        SummaryResult result = new SummaryResult();
        double pron, fluency, reaction, natural;
        if (evaluations != null && !evaluations.isEmpty()) {
            pron = round1(evaluations.stream().mapToDouble(e -> e.pron).average().orElse(0));
            fluency = round1(evaluations.stream().mapToDouble(e -> e.fluency).average().orElse(0));
            reaction = round1(evaluations.stream().mapToDouble(e -> e.reaction).average().orElse(0));
            natural = round1(evaluations.stream().mapToDouble(e -> e.natural).average().orElse(0));
        } else {
            pron = fluency = reaction = natural = 70.0;
        }
        double total = round1(pron * 0.35 + fluency * 0.25 + reaction * 0.2 + natural * 0.2);

        long userMsgs = messages.stream().filter(m -> "user".equals(m.speaker)).count();
        List<String> highlights = new ArrayList<>(List.of(
                "表达自然，能围绕场景主动开口并回应对方。",
                "话题保持连贯，敢于完整表达自己的想法。"));
        if (userMsgs >= 4) {
            highlights.add(1, "本场完成了 " + userMsgs + " 次有效开口，练习量充足。");
        }
        List<String> improvements = new ArrayList<>();
        if (pron < 80) {
            improvements.add("部分单词发音不够清晰，建议放慢语速逐词练习。");
        }
        if (fluency < 78) {
            improvements.add("注意句间停顿与连读，保持稳定语流。");
        }
        if (improvements.isEmpty()) {
            improvements.add("可进一步丰富句型，尝试使用更地道的表达。");
        }
        List<String> suggestions = List.of(
                "回听本场录音，标记卡壳与犹豫的句子并重复练习 3 次。",
                "跟随「场景」对应素材做一遍慢速跟读，强化发音。");

        List<Map<String, Object>> corrections = new ArrayList<>();
        for (EvalResult e : evaluations) {
            for (Map<String, Object> issue : e.phonemeIssues) {
                corrections.add(Map.of(
                        "type", "发音",
                        "word", issue.getOrDefault("word", ""),
                        "correct", issue.getOrDefault("phoneme", ""),
                        "note", issue.getOrDefault("note", "")));
            }
            if (e.betterExpression != null) {
                corrections.add(Map.of("type", "地道表达", "word", "", "correct", e.betterExpression, "note", ""));
            }
        }
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        List<Map<String, Object>> dedup = new ArrayList<>();
        for (Map<String, Object> c : corrections) {
            String key = c.get("type") + "-" + c.get("word") + "-" + c.get("correct");
            if (seen.add(key)) {
                dedup.add(c);
            }
            if (dedup.size() >= 4) break;
        }

        String feedbackText = "综合表现不错，最终得分 " + total + "。继续保持每天开口练习，重点突破"
                + String.join("、", improvements.subList(0, Math.min(2, improvements.size())));

        result.total = total;
        result.pron = pron;
        result.fluency = fluency;
        result.reaction = reaction;
        result.natural = natural;
        result.highlights = highlights;
        result.improvements = improvements;
        result.suggestions = suggestions;
        result.corrections = dedup;
        result.feedbackText = feedbackText;
        return result;
    }

    private static double uniform(double from, double to) {
        return from + RANDOM.nextDouble() * (to - from);
    }

    // ------------------------------------------------------------
    // 翻译与跟读评测（内置规则实现，Ollama 不可达时兜底）
    // ------------------------------------------------------------
    private static final String[][] COMMON_WORDS = {
            {"practice", "练习"}, {"progress", "进步"}, {"brave", "勇敢的"}, {"dream", "梦想"},
            {"fall", "跌倒"}, {"rise", "起来"}, {"yesterday", "昨天"}, {"history", "历史"},
            {"tomorrow", "明天"}, {"mystery", "谜团"}, {"choices", "选择"}, {"abilities", "能力"},
            {"fate", "命运"}, {"stars", "星辰"}, {"heart", "心"}, {"voice", "声音"},
            {"panda", "熊猫"}, {"ocean", "海洋"}, {"swimming", "游泳"}, {"dreamer", "追梦者"},
            {"bloom", "绽放"}, {"adversity", "逆境"}, {"flower", "花"}, {"greatness", "伟大"},
            {"grown", "成长"}, {"keep", "保持"}, {"moving", "前进"}, {"forward", "向前"},
            {"hope", "希望"}, {"good", "好的"}, {"thing", "事物"}, {"never", "永远不"},
            {"somebody", "某人"}, {"tell", "告诉"}, {"afraid", "害怕"}, {"master", "主宰"},
            {"soul", "灵魂"}, {"captain", "船长"}, {"adventure", "冒险"}, {"beyond", "超越"},
            {"infinity", "无限"}, {"limits", "极限"}, {"great", "伟大的"}, {"power", "力量"},
            {"responsibility", "责任"}, {"wish", "愿望"}, {"always", "总是"}, {"choose", "选择"},
            {"face", "面对"}, {"courage", "勇气"}, {"believe", "相信"}, {"stronger", "更强壮"},
            {"seem", "看起来"}, {"decide", "决定"}, {"time", "时间"}, {"given", "给予"},
            {"worth", "值得"}, {"learn", "学习"}, {"mistakes", "错误"}, {"trying", "尝试"},
            {"daily", "每天"}, {"small", "小"}, {"steps", "步"}, {"life", "生活"},
            {"live", "生活"}, {"like", "像"}, {"box", "盒子"}, {"chocolates", "巧克力"},
            {"past", "过去"}, {"hurt", "伤害"}, {"run", "跑"}, {"world", "世界"},
            {"people", "人们"}, {"friend", "朋友"}, {"love", "爱"}, {"light", "光"},
            {"dark", "黑暗"}, {"night", "夜晚"}, {"day", "白天"}, {"morning", "早晨"},
            {"start", "开始"}, {"stop", "停止"}, {"try", "尝试"}, {"way", "路"},
            {"find", "找到"}, {"give", "给予"}, {"take", "拿"}, {"make", "做"},
            {"know", "知道"}, {"think", "想"}, {"feel", "感觉"}, {"see", "看见"},
            {"speak", "说"}, {"word", "单词"}, {"sentence", "句子"}, {"story", "故事"},
            {"begin", "开始"}, {"end", "结束"}, {"first", "第一"}, {"last", "最后"},
            {"more", "更多"}, {"most", "最多"}, {"best", "最好"}, {"better", "更好"},
    };

    /** 命中内置词表则替换为中文，未命中保留原词，用于 Ollama 不可达时的离线直译 */
    static String gloss(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Za-z']+|[^A-Za-z']+").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group();
            if (token.isEmpty()) {
                continue;
            }
            if (Character.isLetter(token.charAt(0))) {
                String zh = null;
                for (String[] pair : COMMON_WORDS) {
                    if (pair[0].equalsIgnoreCase(token)) {
                        zh = pair[1];
                        break;
                    }
                }
                sb.append(zh != null ? zh : token);
            } else {
                sb.append(token);
            }
        }
        return sb.toString();
    }

    @Override
    public List<String> translate(List<String> texts) {
        List<String> out = new ArrayList<>();
        if (texts == null) {
            return out;
        }
        for (String t : texts) {
            out.add(t == null || t.isBlank() ? null : gloss(t));
        }
        return out;
    }

    @Override
    public EvalResult evaluateReading(String target, String spoken) {
        EvalResult r = new EvalResult();
        int tc = (target == null ? "" : target).trim().length();
        int sc = (spoken == null ? "" : spoken).trim().length();
        // 覆盖度 = 用户所说字符占参照的比例（粗粒度的完整度估计）
        int covered = Math.min(tc, sc);
        double coverage = tc == 0 ? 0 : (double) covered / tc;
        double natural = clamp(58 + coverage * 30 + uniform(-3, 3), 50, 97);
        double pron = clamp(natural + uniform(-5, 5), 50, 97);
        double fluency = clamp(natural - (sc < 8 ? 6 : 0) + uniform(-4, 4), 50, 97);
        double reaction = round1(coverage * 100);
        r.pron = round1(pron);
        r.fluency = round1(fluency);
        r.reaction = reaction;
        r.natural = round1(natural);
        String fb;
        if (coverage < 0.5) {
            fb = "只读到了原句的一部分，建议先逐句慢读，把每个单词念清楚。";
        } else if (coverage < 0.9) {
            fb = "大部分读到位了，试着把长句分段，稳住节奏再连起来。";
        } else {
            fb = "读得很完整，语调自然。继续保持，注意重音与情感。";
        }
        r.grammarFeedback = fb;
        r.phonemeIssues = new ArrayList<>();
        if (sc > 0 && (target != null && target.toLowerCase().contains("the"))) {
            r.phonemeIssues.add(Map.of("word", "the", "phoneme", "/ðə/", "note", "th 需舌尖轻触上齿"));
        }
        return r;
    }

    private static double clamp(double v, double lo, double hi) {
        double r = Math.min(hi, Math.max(lo, v));
        return Math.round(r * 10.0) / 10.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}