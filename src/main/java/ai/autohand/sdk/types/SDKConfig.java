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
        this.environment = Map.copyOf(builder.environment);
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
                .options(options);
    }

    public List<String> cliArgs() {
        List<String> args = new ArrayList<>();
        args.add("--mode");
        args.add("rpc");

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
        addOption(args, "--session-id", sessionId);
        addOption(args, "--model", model);
        addOption(args, "--sys-prompt", systemPrompt);
        addOption(args, "--append-sys-prompt", appendSystemPrompt);
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

        private Builder options(Object[] options) {
            this.options = options == null ? new Object[0] : Arrays.copyOf(options, options.length);
            return this;
        }

        public SDKConfig build() {
            return new SDKConfig(this);
        }
    }
}
