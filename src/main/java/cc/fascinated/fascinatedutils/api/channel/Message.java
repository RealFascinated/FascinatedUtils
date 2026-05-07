package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public record Message(String id, String channelId, String authorId, @Nullable String content, String createdAt,
                      @Nullable String editedAt, List<AttachmentDTO> attachments) {
    public User user() {
        return Alumite.INSTANCE.users().getUser(authorId);
    }
}
