package cc.fascinated.fascinatedutils.api.channel.json;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public record SendChannelMessageBodyDTO(@Nullable String content, List<String> attachmentIds) {}
