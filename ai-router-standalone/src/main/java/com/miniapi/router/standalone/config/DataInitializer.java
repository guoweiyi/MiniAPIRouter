package com.miniapi.router.standalone.config;

import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.domain.RouteRule;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.spi.RouteRuleRepository;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.standalone.entity.IntentConfigDO;
import com.miniapi.router.standalone.mapper.IntentConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final Long TENANT_ID = 1L;

    private final ApiKeyConfigRepository keyRepository;
    private final RouteRuleRepository ruleRepository;
    private final IntentConfigMapper intentMapper;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(ApiKeyConfigRepository keyRepository, RouteRuleRepository ruleRepository,
                           IntentConfigMapper intentMapper, DataSource dataSource) {
        this.keyRepository = keyRepository;
        this.ruleRepository = ruleRepository;
        this.intentMapper = intentMapper;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateIntentSchema();
        seedIntents();

        List<RouteRule> existingRules = ruleRepository.findByTenantId(TENANT_ID);
        boolean hasIntentRoute = existingRules.stream()
                .anyMatch(r -> "Intent Route".equals(r.getRuleName()));
        boolean hasAutoRoute = existingRules.stream()
                .anyMatch(r -> "Auto Route".equals(r.getRuleName()));
        if (!hasIntentRoute) createIntentRoute();
        if (!hasAutoRoute) createAutoRoute();

        if (SetupWizard.hasSetupData()) {
            createKeyFromSetup();
            SetupWizard.deleteSetupData();
        }

        printStartupBanner();
    }

    private void migrateIntentSchema() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(intent_config)");
        boolean hasIsDefault = columns.stream().anyMatch(c -> "is_default".equals(c.get("name")));
        boolean hasCustomized = columns.stream().anyMatch(c -> "customized".equals(c.get("name")));
        if (!hasIsDefault) {
            jdbcTemplate.execute("ALTER TABLE intent_config ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0");
            log.info("[Migration] Added is_default column to intent_config");
        }
        if (!hasCustomized) {
            jdbcTemplate.execute("ALTER TABLE intent_config ADD COLUMN customized INTEGER NOT NULL DEFAULT 0");
            log.info("[Migration] Added customized column to intent_config");
        }
    }

    private void seedIntents() {
        Long defaultCount = intentMapper.selectCount(
                new LambdaQueryWrapper<IntentConfigDO>().eq(IntentConfigDO::getIsDefault, 1));
        if (defaultCount == null || defaultCount == 0) {
            IntentConfigDO dft = new IntentConfigDO();
            dft.setTenantId(TENANT_ID);
            dft.setLabel("default");
            dft.setName("默认意图路由");
            dft.setDescription("作为其他意图的默认模板，编辑后会同步到未自定义的意图");
            dft.setTargetKeyIds(List.of());
            dft.setKeyWeights(new LinkedHashMap<>());
            dft.setSortOrder(0);
            dft.setEnabled(1);
            dft.setIsDefault(1);
            dft.setCustomized(0);
            intentMapper.insert(dft);
            log.info("[Init] Seeded default intent config");
        }

        Long count = intentMapper.selectCount(null);
        if (count != null && count > 1) return;
        int order = 1;
        seed("reasoning", "推理思考", "逻辑分析、数学计算、复杂推理", order++);
        seed("casual_chat", "日常聊天", "闲聊、问候、简单对话", order++);
        seed("planning", "项目规划", "架构设计、方案拆解、任务规划", order++);
        seed("simple_instruction", "简单执行", "指令跟随、格式转换、简单任务", order++);
        seed("coding_review", "代码开发与审查", "编程、调试、代码审查、技术问答", order++);
        seed("long_context_summary", "长文本处理与摘要", "摘要、总结、翻译、长文处理", order++);
        seed("creative_writing", "创意写作与角色扮演", "文案、创意写作、角色扮演、润色", order++);
        seed("structured_extraction", "结构化输出与数据提取", "结构化输出、信息抽取、数据整理", order++);
        log.info("[Init] Seeded {} intent configs", order - 1);
    }

    private void seed(String label, String name, String description, int sortOrder) {
        IntentConfigDO dO = new IntentConfigDO();
        dO.setTenantId(TENANT_ID);
        dO.setLabel(label);
        dO.setName(name);
        dO.setDescription(description);
        dO.setTargetKeyIds(List.of());
        dO.setKeyWeights(new LinkedHashMap<>());
        dO.setSortOrder(sortOrder);
        dO.setEnabled(1);
        dO.setIsDefault(0);
        dO.setCustomized(0);
        intentMapper.insert(dO);
    }

    private void createIntentRoute() {
        RouteRule rule = new RouteRule();
        rule.setTenantId(TENANT_ID);
        rule.setRuleName("Intent Route");
        rule.setMatchType("intent");
        rule.setMatchPattern("*");
        rule.setTargetKeyIds(List.of());
        rule.setStrategy("weight");
        rule.setIntentModel("deepseek-v4-flash");
        rule.setFallbackEnabled(true);
        rule.setMaxFallback(2);
        rule.setPriority(10);
        rule.setEnabled(true);
        rule.setDescription("按意图路由：意图评估模型分析请求意图，从 intent_config 表读取意图目录与用户配置的模型权重，路由到最适合的模型；未配置时继承 Auto Route 的全部 Key 与默认权重。");
        ruleRepository.save(rule);
        log.info("[Init] Intent route created (intent catalog from intent_config table)");
    }

    private void createAutoRoute() {
        RouteRule rule = new RouteRule();
        rule.setTenantId(TENANT_ID);
        rule.setRuleName("Auto Route");
        rule.setMatchType("model");
        rule.setMatchPattern("*");
        rule.setTargetKeyIds(List.of());
        rule.setStrategy("weight");
        rule.setFallbackEnabled(true);
        rule.setMaxFallback(2);
        rule.setPriority(100);
        rule.setEnabled(true);
        rule.setDescription("Auto-routes to all configured API keys.");
        ruleRepository.save(rule);
        log.info("[Init] Auto route created");
    }

    private void createKeyFromSetup() {
        try {
            String json = SetupWizard.readSetupData();
            if (json == null) return;

            var node = JsonUtils.parse(json);

            ApiKeyConfig config = new ApiKeyConfig();
            config.setTenantId(TENANT_ID);
            config.setName(node.path("provider").asText("deepseek") + " Key");
            config.setProvider(node.path("provider").asText("deepseek"));
            config.setProtocol("anthropic".equalsIgnoreCase(config.getProvider()) ? "anthropic" : "openai");
            config.setApiKey(node.path("api_key").asText());
            config.setBaseUrl(node.path("base_url").asText());
            List<String> models = new ArrayList<>();
            node.path("models").forEach(m -> models.add(m.asText()));
            config.setModels(models);
            config.setWeight(1);
            config.setPriority(0);
            config.setMaxConcurrent(10);
            config.setTimeoutMs(30000);
            config.setRetryCount(1);
            config.setStatus(1);
            config.setHealthStatus("unknown");

            keyRepository.save(config);
            log.info("[Init] API Key '{}' created from setup wizard (provider={}, models={})",
                    config.getName(), config.getProvider(), config.getModels());
        } catch (Exception e) {
            log.warn("[Init] Failed to create key from setup data: {}", e.getMessage());
        }
    }

    private void printStartupBanner() {
        List<ApiKeyConfig> keys = keyRepository.findByTenantId(TENANT_ID);
        long activeKeys = keys.stream().filter(k -> k.getStatus() != null && k.getStatus() == 1).count();

        String authToken = System.getProperty("miniapi.router.auth-token", "sk-miniapi-standalone");
        String port = System.getProperty("server.port", "9090");

        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║       MiniAPIRouter Standalone 已就绪      ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf( "  ║  服务地址  : http://localhost:%-11s║%n", port);
        System.out.printf( "  ║  认证 Token : %-30s║%n", truncate(authToken, 30));
        System.out.printf( "  ║  API Keys  : %-30d║%n", activeKeys);
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  管理接口: /api/v1/config/*              ║");
        System.out.println("  ║  代理接口: /v1/chat/completions           ║");
        System.out.println("  ║            /v1/messages                   ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        if (activeKeys == 0) {
            System.out.println();
            System.out.println("  ⚠ 尚未配置 API Key。请通过 API 添加:");
            System.out.println("    curl -X POST http://localhost:" + port + "/api/v1/config/api-keys \\");
            System.out.println("      -H 'Authorization: Bearer " + authToken + "' \\");
            System.out.println("      -H 'Content-Type: application/json' \\");
            System.out.println("      -d '{\"name\":\"My Key\",\"provider\":\"deepseek\",\"api_key\":\"sk-xxx\",\"base_url\":\"https://api.deepseek.com\",\"models\":[\"deepseek-v4-flash\"]}'");
        }
        System.out.println();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
