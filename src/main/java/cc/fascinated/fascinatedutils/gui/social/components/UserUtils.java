package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.common.TimeUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserUtils {

    public static String statusLine(User user) {
        UserStatus userStatus = user == null || user.userStatus() == null ? UserStatus.OFFLINE : user.userStatus();
        if (userStatus == UserStatus.OFFLINE && user != null && user.lastSeen() != null) {
            long lastSeenTime = user.lastSeen().getTime();
            return userStatus.label() + " · " + TimeUtils.timeAgo(lastSeenTime, System.currentTimeMillis() - lastSeenTime < 61_000 ? 1 : 2);
        }
        if (user != null && user.activity() != null && userStatus != UserStatus.OFFLINE) {
            return userStatus.label() + " · " + user.activity().label();
        }
        return userStatus.label();
    }
}
