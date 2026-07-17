package ai.autohand.sdk.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Configuration for the CLI-backed SDK. */
public final class SDKConfig {
    private final String cwd;
    private final String cliPath;
    private final boolean debug;
    private final int timeoutMs;
    private final String model;
    private final String systemPrompt;
    private final String appendSystemPrompt;
    private final List<SkillReference> skills;
    private final List<String> additionalDirectories;
    private final List<String> extraArgs;
    private final Map<String, String> environment;
    private final Boolean unrestricted;
    private final Boolean autoMode;
    private final Boolean autoSkill;
    private final Boolean autoCommit;
    private final Boolean contextCompact;
    private final Integer maxIterations;
    private final Integer maxRuntime;
    private final Double maxCost;
    private final Double temperature;
    private final String yolo;
    private final Integer yoloTimeout;
    private final String sessionId;
    private final Boolean persistSession;
    private final Boolean resume;
    private final Boolean continueSession;
    private final Boolean bare;
    private final Boolean idleLogout;
    private final String fork;
    private final String systemPromptFile;
    private final String appendSystemPromptFile;
    private final String mcpConfig;
    private final String agents;
    private final String pluginDir;
    private final String displayLanguage;
    private final String sessionPath;
    private final Integer autoSaveInterval;
    private final Boolean agentsMdEnabled;
    private final Boolean agentsMdCreate;
    private final String agentsMdPath;
    private final Boolean agentsMdAutoUpdate;
    private final Integer maxTokens;
    private final Double compressionThreshold;
    private final Double summarizationThreshold;
    private final List<SkillSource> skillSources;
    private final Boolean installMissingSkills;
    private final String provider;
    private final String apiKey;
    private final String baseUrl;
    private final String autohandAIPlan;
    private final FeatureFlagSettings features;
    private final Object[] options;

    /**
     * Compatibility constructor used by the generated examples. The first
     * String in the option tail is treated as the model, matching the
     * TypeScript SDK's common constructor shape.
     */
    public SDKConfig(String cwd, String cliPath, boolean debug, int timeoutMs, Object... options) {
        this(fromLegacy(cwd, cliPath, debug, timeoutMs, options));
    }

    private SDKConfig(Builder builder) {
        this.cwd = blankToDefault(builder.cwd, ".");
        this.cliPath = blankToNull(builder.cliPath);
        this.debug = builder.debug;
        this.timeoutMs = builder.timeoutMs <= 0 ? 300_000 : builder.timeoutMs;
        this.model = blankToNull(builder.model);
        this.systemPrompt = blankToNull(builder.systemPrompt);
        this.appendSystemPrompt = blankToNull(builder.appendSystemPrompt);
        this.skills = List.copyOf(builder.skills);
        this.additionalDirectories = List.copyOf(builder.additionalDirectories);
        this.extraArgs = List.copyOf(builder.extraArgs);
        Map<String, String> processEnvironment = new LinkedHashMap<>(builder.environment);
        this.unrestricted = builder.unrestricted;
        this.autoMode = builder.autoMode;
        this.autoSkill = builder.autoSkill;
        this.autoCommit = builder.autoCommit;
        this.contextCompact = builder.contextCompact;
        this.maxIterations = builder.maxIterations;
        this.maxRuntime = builder.maxRuntime;
        this.maxCost = builder.maxCost;
        this.temperature = builder.temperature;
        this.yolo = blankToNull(builder.yolo);
        this.yoloTimeout = builder.yoloTimeout;
        this.sessionId = blankToNull(builder.sessionId);
        this.persistSession = builder.persistSession;
        this.resume = builder.resume;
        this.continueSession = builder.continueSession;
        this.bare = builder.bare;
        this.idleLogout = builder.idleLogout;
        this.fork = blankToNull(builder.fork);
        this.systemPromptFile = blankToNull(builder.systemPromptFile);
        this.appendSystemPromptFile = blankToNull(builder.appendSystemPromptFile);
        this.mcpConfig = blankToNull(builder.mcpConfig);
        this.agents = blankToNull(builder.agents);
        this.pluginDir = blankToNull(builder.pluginDir);
        this.displayLanguage = blankToNull(builder.displayLanguage);
        this.sessionPath = blankToNull(builder.sessionPath);
        this.autoSaveInterval = builder.autoSaveInterval;
        this.agentsMdEnabled = builder.agentsMdEnabled;
        this.agentsMdCreate = builder.agentsMdCreate;
        this.agentsMdPath = blankToNull(builder.agentsMdPath);
        this.agentsMdAutoUpdate = builder.agentsMdAutoUpdate;
        this.maxTokens = builder.maxTokens;
        this.compressionThreshold = builder.compressionThreshold;
        this.summarizationThreshold = builder.summarizationThreshold;
        this.skillSources = List.copyOf(builder.skillSources);
        this.installMissingSkills = builder.installMissingSkills;
        this.provider = blankToNull(builder.provider);
        this.apiKey = blankToNull(builder.apiKey);
        this.baseUrl = blankToNull(builder.baseUrl);
        this.autohandAIPlan = blankToNull(builder.autohandAIPlan);
        this.features = builder.features;
        if ("autohandai".equalsIgnoreCase(provider)) {
            processEnvironment.putIfAbsent("AUTOHAND_AI_PLAN", autohandAIPlan == null ? "cloud" : autohandAIPlan);
            if (apiKey != null) {
                processEnvironment.putIfAbsent("AUTOHAND_AI_API_KEY", apiKey);
            }
            if (baseUrl != null) {
                processEnvironment.putIfAbsent("AUTOHAND_AI_BASE_URL", baseUrl);
            }
        }
        this.environment = Map.copyOf(processEnvironment);
        this.options = Arrays.copyOf(builder.options, builder.options.length);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .cwd(cwd)
                .cliPath(cliPath)
                .debug(debug)
                .timeoutMs(timeoutMs)
                .model(model)
                .systemPrompt(systemPrompt)
                .appendSystemPrompt(appendSystemPrompt)
                .skills(skills)
                .additionalDirectories(additionalDirectories)
                .extraArgs(extraArgs)
                .environment(environment)
                .unrestricted(unrestricted)
                .autoMode(autoMode)
                .autoSkill(autoSkill)
                .autoCommit(autoCommit)
                .contextCompact(contextCompact)
                .maxIterations(maxIterations)
                .maxRuntime(maxRuntime)
                .maxCost(maxCost)
                .temperature(temperature)
                .yolo(yolo)
                .yoloTimeout(yoloTimeout)
                .sessionId(sessionId)
                .persistSession(persistSession)
                .resume(resume)
                .continueSession(continueSession)
                .bare(bare)
                .idleLogout(idleLogout)
                .fork(fork)
                .systemPromptFile(systemPromptFile)
                .appendSystemPromptFile(appendSystemPromptFile)
                .mcpConfig(mcpConfig)
                .agents(agents)
                .pluginDir(pluginDir)
                .displayLanguage(displayLanguage)
                .sessionPath(sessionPath)
                .autoSaveInterval(autoSaveInterval)
                .agentsMdEnabled(agentsMdEnabled)
                .agentsMdCreate(agentsMdCreate)
                .agentsMdPath(agentsMdPath)
                .agentsMdAutoUpdate(agentsMdAutoUpdate)
                .maxTokens(maxTokens)
                .compressionThreshold(compressionThreshold)
                .summarizationThreshold(summarizationThreshold)
                .skillSources(skillSources)
                .installMissingSkills(installMissingSkills)
                .provider(provider)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .autohandAIPlan(autohandAIPlan)
                .features(features)
                .options(options);
    }

    public List<String> cliArgs() {
        List<String> args = new ArrayList<>();
        args.add("--mode");
        args.add("rpc");

        addFlag(args, "--bare", bare);
        addFlag(args, "--unrestricted", unrestricted);
        addFlag(args, "--auto-mode", autoMode);
        addFlag(args, "--auto-skill", autoSkill);
        addFlag(args, "-c", autoCommit);
        if (Boolean.FALSE.equals(contextCompact)) {
            args.add("--no-context-compact");
        } else {
            addFlag(args, "--context-compact", contextCompact);
        }
        addFlag(args, "--persist-session", persistSession);
        addFlag(args, "--resume", resume);
        addFlag(args, "--continue", continueSession);
        if (Boolean.FALSE.equals(idleLogout)) {
            args.add("--no-idle-logout");
        }
        addOption(args, "--session-id", sessionId);
        addOption(args, "--fork", fork);
        addOption(args, "--session-path", sessionPath);
        addOption(args, "--auto-save-interval", autoSaveInterval);
        if (Boolean.FALSE.equals(agentsMdEnabled)) {
            args.add("--no-agents-md");
        } else {
            addFlag(args, "--agents-md", agentsMdEnabled);
        }
        addFlag(args, "--agents-md-create", agentsMdCreate);
        addOption(args, "--agents-md-path", agentsMdPath);
        addFlag(args, "--agents-md-auto-update", agentsMdAutoUpdate);
        addOption(args, "--max-tokens", maxTokens);
        addOption(args, "--compression-threshold", compressionThreshold);
        addOption(args, "--summarization-threshold", summarizationThreshold);
        addOption(args, "--display-language", displayLanguage);
        addOption(args, "--model", model);
        addOption(args, "--sys-prompt", systemPrompt);
        addOption(args, "--append-sys-prompt", appendSystemPrompt);
        addOption(args, "--system-prompt-file", systemPromptFile);
        addOption(args, "--append-system-prompt-file", appendSystemPromptFile);
        addOption(args, "--mcp-config", mcpConfig);
        addOption(args, "--agents", agents);
        addOption(args, "--plugin-dir", pluginDir);
        addOption(args, "--max-iterations", maxIterations);
        addOption(args, "--max-runtime", maxRuntime);
        addOption(args, "--max-cost", maxCost);
        addOption(args, "--temperature", temperature);
        addOption(args, "--yolo", yolo);
        addOption(args, "--yolo-timeout", yoloTimeout);

        List<String> skillNames = skills.stream()
                .map(SDKConfig::skillToken)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        if (!skillNames.isEmpty()) {
            args.add("--skills");
            args.add(String.join(",", skillNames));
        }
        if (!skillSources.isEmpty()) {
            args.add("--skill-sources");
            args.add(skillSources.stream().map(SkillSource::cliValue).collect(java.util.stream.Collectors.joining(",")));
        }
        addFlag(args, "--install-missing-skills", installMissingSkills);

        for (String dir : additionalDirectories) {
            addOption(args, "--add-dir", dir);
        }

        args.addAll(extraArgs);
        return args;
    }

    public String cwd() {
        return cwd;
    }

    public String cliPath() {
        return cliPath;
    }

    public boolean debug() {
        return debug;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public Object[] options() {
        return Arrays.copyOf(options, options.length);
    }

    public String model() {
        return model;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String appendSystemPrompt() {
        return appendSystemPrompt;
    }

    public List<SkillReference> skills() {
        return skills;
    }

    public List<String> additionalDirectories() {
        return additionalDirectories;
    }

    public List<String> extraArgs() {
        return extraArgs;
    }

    public Map<String, String> environment() {
        return environment;
    }

    public FeatureFlagSettings features() {
        return features;
    }

    private static Builder fromLegacy(String cwd, String cliPath, boolean debug, int timeoutMs, Object[] options) {
        Builder builder = builder()
                .cwd(cwd)
                .cliPath(cliPath)
                .debug(debug)
                .timeoutMs(timeoutMs)
                .options(options == null ? new Object[0] : options);

        if (options != null) {
            for (Object option : options) {
                if (builder.model == null && option instanceof String value && !value.isBlank()) {
                    builder.model(value);
                } else if (option instanceof SkillReference skill) {
                    builder.skills.add(skill);
                } else if (option instanceof List<?> values) {
                    for (Object value : values) {
                        if (value instanceof SkillReference skill) {
                            builder.skills.add(skill);
                        }
                    }
                }
            }
        }

        return builder;
    }

    private static String skillToken(SkillReference reference) {
        if (reference.path() != null && !reference.path().isBlank()) {
            return reference.path();
        }
        return reference.name();
    }

    private static void addFlag(List<String> args, String flag, Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            args.add(flag);
        }
    }

    private static void addOption(List<String> args, String flag, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            args.add(flag);
            args.add(String.valueOf(value));
        }
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static final class Builder {
        private String cwd = ".";
        private String cliPath;
        private boolean debug;
        private int timeoutMs = 300_000;
        private String model;
        private String systemPrompt;
        private String appendSystemPrompt;
        private final List<SkillReference> skills = new ArrayList<>();
        private final List<String> additionalDirectories = new ArrayList<>();
        private final List<String> extraArgs = new ArrayList<>();
        private final Map<String, String> environment = new LinkedHashMap<>();
        private Boolean unrestricted;
        private Boolean autoMode;
        private Boolean autoSkill;
        private Boolean autoCommit;
        private Boolean contextCompact;
        private Integer maxIterations;
        private Integer maxRuntime;
        private Double maxCost;
        private Double temperature;
        private String yolo;
        private Integer yoloTimeout;
        private String sessionId;
        private Boolean persistSession;
        private Boolean resume;
        private Boolean continueSession;
        private Boolean bare;
        private Boolean idleLogout;
        private String fork;
        private String systemPromptFile;
        private String appendSystemPromptFile;
        private String mcpConfig;
        private String agents;
        private String pluginDir;
        private String displayLanguage;
        private String sessionPath;
        private Integer autoSaveInterval;
        private Boolean agentsMdEnabled;
        private Boolean agentsMdCreate;
        private String agentsMdPath;
        private Boolean agentsMdAutoUpdate;
        private Integer maxTokens;
        private Double compressionThreshold;
        private Double summarizationThreshold;
        private final List<SkillSource> skillSources = new ArrayList<>();
        private Boolean installMissingSkills;
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String autohandAIPlan;
        private FeatureFlagSettings features;
        private Object[] options = new Object[0];

        public Builder cwd(String cwd) {
            this.cwd = cwd;
            return this;
        }

        public Builder cliPath(String cliPath) {
            this.cliPath = cliPath;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder appendSystemPrompt(String appendSystemPrompt) {
            this.appendSystemPrompt = appendSystemPrompt;
            return this;
        }

        public Builder skills(List<SkillReference> skills) {
            this.skills.clear();
            if (skills != null) {
                this.skills.addAll(skills);
            }
            return this;
        }

        public Builder addSkill(SkillReference skill) {
            if (skill != null) {
                this.skills.add(skill);
            }
            return this;
        }

        public Builder additionalDirectories(List<String> directories) {
            this.additionalDirectories.clear();
            if (directories != null) {
                this.additionalDirectories.addAll(directories);
            }
            return this;
        }

        public Builder addDirectory(String directory) {
            if (directory != null && !directory.isBlank()) {
                this.additionalDirectories.add(directory);
            }
            return this;
        }

        public Builder extraArgs(List<String> extraArgs) {
            this.extraArgs.clear();
            if (extraArgs != null) {
                this.extraArgs.addAll(extraArgs);
            }
            return this;
        }

        public Builder addExtraArg(String arg) {
            if (arg != null && !arg.isBlank()) {
                this.extraArgs.add(arg);
            }
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment.clear();
            if (environment != null) {
                this.environment.putAll(environment);
            }
            return this;
        }

        public Builder env(String key, String value) {
            if (key != null && value != null) {
                this.environment.put(key, value);
            }
            return this;
        }

        public Builder unrestricted(Boolean unrestricted) {
            this.unrestricted = unrestricted;
            return this;
        }

        public Builder autoMode(Boolean autoMode) {
            this.autoMode = autoMode;
            return this;
        }

        public Builder autoSkill(Boolean autoSkill) {
            this.autoSkill = autoSkill;
            return this;
        }

        public Builder autoCommit(Boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder contextCompact(Boolean contextCompact) {
            this.contextCompact = contextCompact;
            return this;
        }

        public Builder maxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder maxRuntime(Integer maxRuntime) {
            this.maxRuntime = maxRuntime;
            return this;
        }

        public Builder maxCost(Double maxCost) {
            this.maxCost = maxCost;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder yolo(String yolo) {
            this.yolo = yolo;
            return this;
        }

        public Builder yoloTimeout(Integer yoloTimeout) {
            this.yoloTimeout = yoloTimeout;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder persistSession(Boolean persistSession) {
            this.persistSession = persistSession;
            return this;
        }

        public Builder resume(Boolean resume) {
            this.resume = resume;
            return this;
        }

        public Builder continueSession(Boolean continueSession) {
            this.continueSession = continueSession;
            return this;
        }

        public Builder bare(Boolean bare) {
            this.bare = bare;
            return this;
        }

        public Builder idleLogout(Boolean idleLogout) {
            this.idleLogout = idleLogout;
            return this;
        }

        public Builder fork(String fork) {
            this.fork = fork;
            return this;
        }

        public Builder systemPromptFile(String systemPromptFile) {
            this.systemPromptFile = systemPromptFile;
            return this;
        }

        public Builder appendSystemPromptFile(String appendSystemPromptFile) {
            this.appendSystemPromptFile = appendSystemPromptFile;
            return this;
        }

        public Builder mcpConfig(String mcpConfig) {
            this.mcpConfig = mcpConfig;
            return this;
        }

        public Builder agents(String agents) {
            this.agents = agents;
            return this;
        }

        public Builder pluginDir(String pluginDir) {
            this.pluginDir = pluginDir;
            return this;
        }

        public Builder displayLanguage(String displayLanguage) {
            this.displayLanguage = displayLanguage;
            return this;
        }

        public Builder sessionPath(String sessionPath) {
            this.sessionPath = sessionPath;
            return this;
        }

        public Builder autoSaveInterval(Integer autoSaveInterval) {
            this.autoSaveInterval = autoSaveInterval;
            return this;
        }

        public Builder agentsMdEnabled(Boolean agentsMdEnabled) {
            this.agentsMdEnabled = agentsMdEnabled;
            return this;
        }

        public Builder agentsMdCreate(Boolean agentsMdCreate) {
            this.agentsMdCreate = agentsMdCreate;
            return this;
        }

        public Builder agentsMdPath(String agentsMdPath) {
            this.agentsMdPath = agentsMdPath;
            return this;
        }

        public Builder agentsMdAutoUpdate(Boolean agentsMdAutoUpdate) {
            this.agentsMdAutoUpdate = agentsMdAutoUpdate;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder compressionThreshold(Double compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
            return this;
        }

        public Builder summarizationThreshold(Double summarizationThreshold) {
            this.summarizationThreshold = summarizationThreshold;
            return this;
        }

        public Builder skillSources(List<SkillSource> skillSources) {
            this.skillSources.clear();
            if (skillSources != null) {
                this.skillSources.addAll(skillSources);
            }
            return this;
        }

        public Builder addSkillSource(SkillSource skillSource) {
            if (skillSource != null) {
                this.skillSources.add(skillSource);
            }
            return this;
        }

        public Builder installMissingSkills(Boolean installMissingSkills) {
            this.installMissingSkills = installMissingSkills;
            return this;
        }

        /** Built-in provider ID or a custom provider ID such as {@code custom:acme}. */
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder provider(ProviderName provider) {
            this.provider = provider == null ? null : provider.name().toLowerCase(java.util.Locale.ROOT);
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder autohandAIPlan(String autohandAIPlan) {
            this.autohandAIPlan = autohandAIPlan;
            return this;
        }

        public Builder features(FeatureFlagSettings features) {
            this.features = features;
            return this;
        }

        private Builder options(Object[] options) {
            this.options = options == null ? new Object[0] : Arrays.copyOf(options, options.length);
            return this;
        }

        public SDKConfig build() {
            return new SDKConfig(this);
        }
    }
}
