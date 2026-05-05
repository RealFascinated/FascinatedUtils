package cc.fascinated.fascinatedutils.api.user;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SelfUser {

    private final Alumite alumite;

    public String id() {
        return alumite.activeUserId();
    }

    public User user() {
        return alumite.users().selfUser();
    }

    public Presence preferredPresence() {
        return alumite.currentPreferredPresence();
    }

    public void updatePreferredPresence(Presence presence) throws AlumiteApiException {
        alumite.updatePreferredPresence(presence);
    }
}
