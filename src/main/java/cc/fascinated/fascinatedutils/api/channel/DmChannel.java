package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.user.User;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public non-sealed class DmChannel extends Channel {

    private volatile User recipient;
    private volatile boolean detailLoaded;

    public DmChannel(Alumite alumite, int channelId) {
        super(alumite, channelId, ChannelKind.DM);
    }

    @Override
    public ChannelDetail.DmChannelDetail detail() {
        if (!detailLoaded) {
            return null;
        }
        return new ChannelDetail.DmChannelDetail(id(), lastReadMessageId(), recipient);
    }

    @Override
    public DmChannel asDmChannel() {
        return this;
    }

    void applyDetail(ChannelDetail.DmChannelDetail detail) {
        if (detail == null) {
            return;
        }
        applyLastReadMessageId(detail.lastReadMessageId());
        recipient = detail.recipient();
        detailLoaded = true;
    }

    public void hide() throws AlumiteApiException {
        alumite().http().sendAuthorizedExpectNoContent("DELETE", "/channels/" + id() + "/hidden", null, "hide channel", "Failed to close direct message.");
        alumite().channels().hideChannelLocal(id());
    }
}
