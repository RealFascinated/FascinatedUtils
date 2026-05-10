package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Accessors(fluent = true)
public class Message {

    private final String id;
    private final String channelId;
    private final String authorId;
    @Nullable private final String content;
    private final String createdAt;
    @Nullable private final String editedAt;
    private final List<AttachmentDTO> attachments;
    private volatile boolean pending;

    public Message(String id, String channelId, String authorId, @Nullable String content,
                   String createdAt, @Nullable String editedAt, List<AttachmentDTO> attachments) {
        this.id = id;
        this.channelId = channelId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
        this.attachments = attachments;
    }

    static Message optimistic(String tempId, String channelId, String authorId,
                               @Nullable String content, String createdAt) {
        Message message = new Message(tempId, channelId, authorId, content, createdAt, null, List.of());
        message.pending = true;
        return message;
    }

    void confirm() {
        this.pending = false;
    }

    void markPending() {
        this.pending = true;
    }

    public User user() {
        return Alumite.INSTANCE.users().getUser(authorId);
    }
}
