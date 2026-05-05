package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
public non-sealed class GroupChannel extends Channel {

    private volatile int ownerUserId;
    private volatile List<GroupMember> members = List.of();

    public GroupChannel(Alumite alumite, int channelId) {
        super(alumite, channelId, ChannelKind.GROUP);
    }

    @Override
    public GroupChannel asGroupChannel() {
        return this;
    }

    void applyDetail(Integer lastReadMessageId, String name, int ownerUserId, List<GroupMember> members, String lastMessageAt, LastMessagePreview lastMessagePreview) {
        applyLastReadMessageId(lastReadMessageId);
        applyName(name);
        applyLastMessageAt(lastMessageAt);
        applyLastMessagePreview(lastMessagePreview);
        this.ownerUserId = ownerUserId;
        this.members = members == null ? List.of() : List.copyOf(members);
    }
}
