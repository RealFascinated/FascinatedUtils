package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SocialChatListWidget {

    public record Props(
        List<Channel> channels,
            float scrollY,
            Consumer<Float> scrollYSink,
        Function<Channel, FWidget> channelRowFactory,
            Supplier<FWidget> emptyFactory
    ) {}

    public static FScrollColumnWidget build(Props props) {
        FColumnWidget body = new FColumnWidget(4f, cc.fascinated.fascinatedutils.gui.core.Align.START);
        if (props.channels() == null || props.channels().isEmpty()) {
            body.addChild(props.emptyFactory().get());
        } else {
            for (Channel channel : props.channels()) {
                body.addChild(props.channelRowFactory().apply(channel));
            }
        }
        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        scroll.setScrollOffsetY(props.scrollY());
        scroll.setScrollOffsetChangeListener(props.scrollYSink());
        return scroll;
    }
}
