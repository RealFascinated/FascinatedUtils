package cc.fascinated.fascinatedutils.updater;

public record UpdateRequiredEvent(String latestVersion, String downloadUrl, String stagedPath) {}
