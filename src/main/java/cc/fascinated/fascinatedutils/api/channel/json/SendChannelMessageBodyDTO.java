package cc.fascinated.fascinatedutils.api.channel.json;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SendChannelMessageBodyDTO(@Nullable String content, List<String> attachmentIds) {}
