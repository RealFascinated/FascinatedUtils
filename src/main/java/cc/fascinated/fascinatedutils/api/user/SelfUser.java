package cc.fascinated.fascinatedutils.api.user;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public class SelfUser {

    private final Alumite alumite;
    private final User user;
    private UserStatus preferredUserStatus;
    private Activity activity;

    public void updatePreferredUserStatus(UserStatus userStatus) throws AlumiteApiException {
        if (userStatus == null) {
            throw new IllegalArgumentException("Preferred user status is required.");
        }
        if (userStatus == UserStatus.OFFLINE) {
            throw new IllegalArgumentException("Preferred user status cannot be offline.");
        }
        alumite.sendUpdateUserStatus(userStatus);
        preferredUserStatus = userStatus;
    }

    public void updateActivity(Activity newActivity) throws AlumiteApiException {
        if (newActivity == null) {
            throw new IllegalArgumentException("Activity is required.");
        }
        alumite.sendUpdateActivity(newActivity);
        activity = newActivity;
    }
}
