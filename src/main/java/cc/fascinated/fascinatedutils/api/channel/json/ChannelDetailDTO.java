package cc.fascinated.fascinatedutils.api.channel.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;

import java.util.List;

public sealed interface ChannelDetailDTO permits ChannelDetailDTO.DmChannelDetailDTO, ChannelDetailDTO.GroupChannelDetailDTO {

    int id();

    Integer lastReadMessageId();

    String lastMessageAt();

    LastMessagePreviewDTO lastMessagePreview();

    ChannelKindDTO type();

    record DmChannelDetailDTO(int id, Integer lastReadMessageId, String lastMessageAt,
                              LastMessagePreviewDTO lastMessagePreview,
                              PublicUserDTO recipient) implements ChannelDetailDTO {

        @Override
        public ChannelKindDTO type() {
            return ChannelKindDTO.DM;
        }
    }

    record GroupChannelDetailDTO(int id, Integer lastReadMessageId, String lastMessageAt,
                                 LastMessagePreviewDTO lastMessagePreview,
                                 String name, Integer ownerUserId,
                                 List<ChannelMemberDTO> members) implements ChannelDetailDTO {

        @Override
        public ChannelKindDTO type() {
            return ChannelKindDTO.GROUP;
        }
    }
}
