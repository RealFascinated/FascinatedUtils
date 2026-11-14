package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.mixininterface.ChatComponentAccess;
import cc.fascinated.fascinatedutils.systems.chat.ChatHistory;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Getter
public class ChatModule extends Module {
    private static final DateTimeFormatter TIME_24_HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12_HOUR = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final BooleanSetting timestamps = BooleanSetting.builder().id("timestamps").defaultValue(true).build();
    private final BooleanSetting twentyFourHourTime = BooleanSetting.builder().id("twenty_four_hour_time").defaultValue(true).build();
    private final BooleanSetting stackDuplicates = BooleanSetting.builder().id("stack_duplicates").defaultValue(true).build();
    private final BooleanSetting highlightMentions = BooleanSetting.builder().id("highlight_mentions").defaultValue(true).build();
    private final ColorSetting mentionColor = ColorSetting.builder().id("mention_color").defaultValue(new SettingColor(255, 170, 0, 255)).build();
    private final BooleanSetting chatSearch = BooleanSetting.builder().id("chat_search").defaultValue(true).build();
    private final BooleanSetting saveHistory = BooleanSetting.builder().id("save_history").defaultValue(true).build();

    @Getter(AccessLevel.NONE)
    private final ChatHistory history = ChatHistory.INSTANCE;
    @Getter(AccessLevel.NONE)
    private String lastPlainText = "";
    @Getter(AccessLevel.NONE)
    private Component lastShownMessage;
    @Getter(AccessLevel.NONE)
    private int duplicateCount = 1;

    public ChatModule() {
        super("Chat", ModuleCategory.GENERAL);
        addSetting(timestamps);
        addSetting(twentyFourHourTime);
        addSetting(stackDuplicates);
        addSetting(highlightMentions);
        addSetting(mentionColor);
        addSetting(chatSearch);
        addSetting(saveHistory);
    }

    /**
     * Applies stacking, mention highlights and timestamps to an incoming chat message, and records it for the saved history.
     *
     * @param contents the original message
     * @param chat     the chat component the message is being added to
     * @return the message to show, the same instance when nothing changed
     */
    public Component transform(Component contents, ChatComponentAccess chat) {
        String plainText = contents.getString();
        boolean stacked = stackIfDuplicate(plainText, chat);

        Component result = contents;
        if (highlightMentions.isEnabled()) {
            result = highlightMentions(result, plainText);
        }
        if (stacked) {
            result = Component.empty().append(result).append(Component.literal(" [x" + duplicateCount + "]").withStyle(ChatFormatting.GRAY));
        }
        if (timestamps.isEnabled()) {
            String time = (twentyFourHourTime.isEnabled() ? TIME_24_HOUR : TIME_12_HOUR).format(LocalTime.now());
            result = Component.empty().append(Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY)).append(result);
        }

        if (saveHistory.isEnabled()) {
            if (stacked) {
                history.replaceLast(result);
            } else {
                history.record(result);
            }
        }
        lastShownMessage = result;
        return result;
    }

    private boolean stackIfDuplicate(String plainText, ChatComponentAccess chat) {
        boolean sameAsLast = stackDuplicates.isEnabled() && !plainText.isBlank() && plainText.equals(lastPlainText);
        List<GuiMessage> allMessages = chat.alumite$getAllMessages();
        lastPlainText = plainText;
        if (sameAsLast && !allMessages.isEmpty() && allMessages.getFirst().content() == lastShownMessage) {
            allMessages.removeFirst();
            chat.alumite$refreshTrimmedMessages();
            duplicateCount++;
            return true;
        }
        duplicateCount = 1;
        return false;
    }

    private Component highlightMentions(Component contents, String plainText) {
        String username = Minecraft.getInstance().getUser().getName();
        if (username == null || username.isBlank()) {
            return contents;
        }
        String lowerText = plainText.toLowerCase(Locale.ROOT);
        String lowerName = username.toLowerCase(Locale.ROOT);
        int senderEnd = senderPrefixEnd(plainText);
        if (lowerText.indexOf(lowerName, senderEnd) < 0) {
            return contents;
        }

        TextColor color = TextColor.fromRgb(mentionColor.getResolvedArgb() & 0xFFFFFF);
        MutableComponent rebuilt = Component.empty();
        int[] consumed = {0};
        contents.visit((style, text) -> {
            int segmentStart = consumed[0];
            consumed[0] += text.length();
            int cursor = 0;
            while (cursor < text.length()) {
                int match = lowerText.indexOf(lowerName, Math.max(segmentStart + cursor, senderEnd)) - segmentStart;
                if (match < cursor || match >= text.length() || match + lowerName.length() > text.length()) {
                    break;
                }
                if (match > cursor) {
                    rebuilt.append(Component.literal(text.substring(cursor, match)).setStyle(style));
                }
                rebuilt.append(Component.literal(text.substring(match, match + lowerName.length())).setStyle(style.withColor(color).withBold(true)));
                cursor = match + lowerName.length();
            }
            if (cursor < text.length()) {
                rebuilt.append(Component.literal(text.substring(cursor)).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return rebuilt;
    }

    /**
     * Finds where a "Name: message" or "&lt;Name&gt; message" sender prefix ends, so the sender's own name is not highlighted.
     */
    private static int senderPrefixEnd(String plainText) {
        int colon = plainText.indexOf(": ");
        int bracket = plainText.indexOf("> ");
        if (colon < 0 && bracket < 0) {
            return 0;
        }
        int end = colon < 0 ? bracket : bracket < 0 ? colon : Math.min(colon, bracket);
        return end + 2;
    }
}
