package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.IntentConfig;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultIntentCatalogProvider implements IntentCatalogProvider {

    private static final List<IntentConfig> DEFAULT_CATALOG = buildDefaultCatalog();

    @Override
    public List<IntentConfig> findAll(Long tenantId) {
        return DEFAULT_CATALOG;
    }

    @Override
    public IntentConfig findByLabel(Long tenantId, String label) {
        if (label == null) return null;
        return DEFAULT_CATALOG.stream()
                .filter(i -> label.equals(i.getLabel()))
                .findFirst()
                .orElse(null);
    }

    private static List<IntentConfig> buildDefaultCatalog() {
        return List.of(
                intent("reasoning", "推理思考", "逻辑分析、数学计算、复杂推理", 1),
                intent("casual_chat", "日常聊天", "闲聊、问候、简单对话", 2),
                intent("planning", "项目规划", "架构设计、方案拆解、任务规划", 3),
                intent("simple_instruction", "简单执行", "指令跟随、格式转换、简单任务", 4),
                intent("coding_review", "代码开发与审查", "编程、调试、代码审查、技术问答", 5),
                intent("long_context_summary", "长文本处理与摘要", "摘要、总结、翻译、长文处理", 6),
                intent("creative_writing", "创意写作与角色扮演", "文案、创意写作、角色扮演、润色", 7),
                intent("structured_extraction", "结构化输出与数据提取", "结构化输出、信息抽取、数据整理", 8)
        );
    }

    private static IntentConfig intent(String label, String name, String description, int sortOrder) {
        IntentConfig c = new IntentConfig();
        c.setLabel(label);
        c.setName(name);
        c.setDescription(description);
        c.setSortOrder(sortOrder);
        c.setTargetKeyIds(List.of());
        c.setKeyWeights(new LinkedHashMap<>());
        c.setEnabled(true);
        return c;
    }
}
