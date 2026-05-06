package cc.fascinated.fascinatedutils.updater;

import lombok.Getter;

@Getter
public class UpdateCheckResult {
    private String latestVersion;
    private boolean upToDate;
    private String downloadUrl;
}
