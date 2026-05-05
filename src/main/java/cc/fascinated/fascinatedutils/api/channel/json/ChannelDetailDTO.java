package cc.fascinated.fascinatedutils.api.channel.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;

import java.util.List;

public sealed interface ChannelDetailDTO permits ChannelDetailDTO.DmChannelDetailDTO, ChannelDetailDTO.GroupChannelDetailDTO {

    String id();

    String lastReadMessageId();

    String lastMessageAt();

    LastMessagePreviewDTO lastMessagePreview();

    ChannelKindDTO type();

    record DmChannelDetailDTO(String id, String lastReadMessageId, String lastMessageAt,
                              LastMessagePreviewDTO lastMessagePreview,
                              PublicUserDTO recipient) implements ChannelDetailDTO {

        @Override
        public ChannelKindDTO type() {
            return ChannelKindDTO.DM;
        }
    }

    record GroupChannelDetailDTO(String id, String lastReadMessageId, String lastMessageAt,
                                 LastMessagePreviewDTO lastMessagePreview,
                                 String name, String ownerUserId,
                                 List<ChannelMemberDTO> members) implements ChannelDetailDTO {

        @Override
        public ChannelKindDTO type() {
            return ChannelKindDTO.GROUP;
        }
    }
}
