package cc.fascinated.fascinatedutils.api.channel.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;

import java.util.List;

public sealed interface ChannelDetailWire permits ChannelDetailWire.DmChannelDetailWire, ChannelDetailWire.GroupChannelDetailWire {

    int id();

    Integer lastReadMessageId();

    ChannelKindWire type();

    record DmChannelDetailWire(int id, Integer lastReadMessageId,
                               PublicUserWire recipient) implements ChannelDetailWire {

        @Override
        public ChannelKindWire type() {
            return ChannelKindWire.DM;
        }
    }

    record GroupChannelDetailWire(int id, Integer lastReadMessageId, String name, Integer ownerUserId,
                                  List<ChannelMemberWire> members) implements ChannelDetailWire {

        @Override
        public ChannelKindWire type() {
            return ChannelKindWire.GROUP;
        }
    }
}
