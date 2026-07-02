package com.miniapi.router.core.intent;

import com.miniapi.router.core.domain.IntentConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptTemplate {

    public String buildSystemPrompt(List<IntentConfig> catalog) {
        StringBuilder labels = new StringBuilder();
        for (IntentConfig i : catalog) {
            if (!Boolean.TRUE.equals(i.getEnabled())) continue;
            if (labels.length() > 0) labels.append(",\n            ");
            labels.append(i.getLabel()).append("(").append(i.getName());
            if (i.getDescription() != null && !i.getDescription().isBlank()) {
                labels.append("/").append(i.getDescription());
            }
            labels.append(")");
        }
        String template = """
                你是AI路由调度专家。你的任务是将用户问题归类到最匹配的意图标签，并评估问题复杂度(1-100)。

                ## 评分维度（综合考量以下四点）
                1. 推理深度：需要多少步逻辑推导？是否需要形式化证明？
                2. 领域知识：需要多少专业背景知识？
                3. 输出复杂度：输出结构有多复杂？代码量多大？
                4. 约束条件：需要同时满足多少个约束？

                ## 复杂度评分参考（附校准示例）
                - 90-100：专家级推理。需要多步严密逻辑推导、形式化证明、复杂数学论证。
                  示例：证明素数性质定理(95)、推导广义相对论方程(93)、形式化验证算法正确性(97)
                - 75-89：复杂实现。需要同时满足多个约束条件的编程任务、系统架构设计。
                  示例：实现带泛型+线程安全+TTL的LRU缓存(82)、设计多租户微服务架构(78)、编写编译器前端(88)
                - 60-74：中等编码。单一约束的编程实现、项目规划、数据分析。
                  示例：用Python实现快速排序(65)、制定项目排期计划(62)、结构化数据清洗与转换(68)
                - 40-59：创意与转换。有约束的创意写作、复杂文本翻译、长文摘要。
                  示例：写带修辞手法的抒情散文(45)、翻译技术文档(52)、总结万字长文(55)
                - 20-39：简单任务。基础翻译、简单问答、格式转换。
                  示例：翻译一句话(25)、解释一个概念(32)、JSON转CSV(28)
                - 1-19：极简交互。日常闲聊、简单问候、一句话指令。
                  示例："你好"(5)、"今天天气怎么样"(12)、"帮我重启服务"(18)

                ## 意图标签参考
                {{LABELS}},
                other(其他/无法归类),
                invalid_continuation(无效继续指令：如"继续"、"接着说"、"继续写"等纯粹要求继续、不含满意度评判的短语),
                follow_up(追问/不满意：如"然后呢"、"还有呢"、"为什么"、"具体一点"等表示用户没看懂或不满意的追问)

                ## 输出要求
                严格输出以下JSON格式，不要包含任何其他内容、不要使用markdown代码块：
                {"intent":"意图标签","score":复杂度分数,"reasoning":"一句话分析"}

                注意：
                1. 只输出JSON，不要有任何额外文字
                2. score必须是1-100之间的整数，禁止小数""";
        return template.replace("{{LABELS}}", labels.toString());
    }

    public String buildUserPrompt(List<?> candidates, String userQuestion) {
        return "## 用户提问\n" + userQuestion + "\n\n请判断意图并评估复杂度。";
    }

    public String buildFullHistoryPrompt(List<?> candidates, List<Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 完整对话历史\n");
        for (Map<String, Object> msg : messages) {
            String role = (String) msg.get("role");
            Object content = msg.get("content");
            if (content == null) continue;
            String roleLabel = "user".equals(role) ? "用户" : "assistant".equals(role) ? "助手" : role;
            sb.append("[").append(roleLabel).append("]: ").append(content.toString()).append("\n\n");
        }
        sb.append("\n请根据完整对话历史判断用户最新问题的真实意图并评估复杂度。");
        return sb.toString();
    }

    public String extractUserQuestion(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) return "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = messages.get(i);
            Object role = msg.get("role");
            Object content = msg.get("content");
            if ("user".equals(role) && content != null && !content.toString().isBlank()) {
                return content.toString().trim();
            }
        }
        return "";
    }
}
