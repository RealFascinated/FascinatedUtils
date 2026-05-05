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
    private volatile boolean detailLoaded;

    public GroupChannel(Alumite alumite, int channelId) {
        super(alumite, channelId, ChannelKind.GROUP);
    }

    @Override
    public ChannelDetail.GroupChannelDetail detail() {
        if (!detailLoaded) {
            return null;
        }
        return new ChannelDetail.GroupChannelDetail(id(), lastReadMessageId(), name(), ownerUserId, members);
    }

    @Override
    public GroupChannel asGroupChannel() {
        return this;
    }

    void applyDetail(ChannelDetail.GroupChannelDetail detail) {
        if (detail == null) {
            return;
        }
        applyLastReadMessageId(detail.lastReadMessageId());
        applyName(detail.name());
        ownerUserId = detail.ownerUserId();
        members = detail.members() == null ? List.of() : List.copyOf(detail.members());
        detailLoaded = true;
    }
}
