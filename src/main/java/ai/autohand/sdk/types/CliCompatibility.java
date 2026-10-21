package ai.autohand.sdk.types;

import java.util.List;
import java.util.Set;

/** Rejects obsolete wrapper switches before spawning the CLI. */
final class CliCompatibility {
    private static final Set<String> UNSUPPORTED = Set.of("--agents-md","--no-agents-md","--agents-md-create","--agents-md-auto-update","--agents-md-path","--auto-save-interval","--compression-threshold","--summarization-threshold","--skills","--skill-sources","--install-missing-skills","--permission-mode","--permission-allow-list","--permission-deny-list");
    private static final Set<String> SWITCHES = Set.of("--bare","--offline","--yes","--y","--dry-run","--debug","--auto-commit","-c","--unrestricted","--restricted","--plan","--answer-only","--setup-only","--no-idle-logout","--auto-skill","--learn","--learn-update","--project","--permissions","--settings","--traces-on","--traces-off","--login","--logout","--patch","--acp","--sdk-resume-last","--tmux","--auto-mode","--no-worktree","--interactive-on-complete","--setup","--about","--feedback","--cc","--context-compact","--no-cc","--no-context-compact","--ephemeral");
    static void validate(List<String> args) {
        for(int index=0; index<args.size(); index++) {
            var argument=args.get(index);
            if(UNSUPPORTED.contains(argument)) throw new IllegalArgumentException(argument + " is unsupported by the current CLI; use a supported CLI option or runtime RPC instead");
            if(!SWITCHES.contains(argument)) index++;
        }
    }
}
