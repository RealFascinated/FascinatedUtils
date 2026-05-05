package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.user.User;

import java.util.List;

public sealed interface ChannelDetail permits ChannelDetail.DmChannelDetail, ChannelDetail.GroupChannelDetail {

    int id();

    Integer lastReadMessageId();

    ChannelKind kind();

    record DmChannelDetail(int id, Integer lastReadMessageId, User recipient) implements ChannelDetail {

        @Override
        public ChannelKind kind() {
            return ChannelKind.DM;
        }
    }

    record GroupChannelDetail(int id, Integer lastReadMessageId, String name, int ownerUserId,
                              List<GroupMember> members) implements ChannelDetail {

        @Override
        public ChannelKind kind() {
            return ChannelKind.GROUP;
        }
    }
}
