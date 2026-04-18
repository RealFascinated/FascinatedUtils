package cc.fascinated.fascinatedutils.updater;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class UpdateRequiredEvent {
    private final String latestVersion;
    private final String downloadUrl;
    private final String stagedPath;
}
