package cc.fascinated.fascinatedutils.systems.activity;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.SelfUser;
import cc.fascinated.fascinatedutils.event.impl.JoinMultiplayerServerEvent;
import cc.fascinated.fascinatedutils.event.impl.SingleplayerWorldLoadEvent;
import cc.fascinated.fascinatedutils.event.impl.TitleScreenLoadEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import meteordevelopment.orbit.EventHandler;

public class ActivityHandler {

    public static final ActivityHandler INSTANCE = new ActivityHandler();
    private Activity currentActivity = Activity.IN_MAIN_MENU;

    @EventHandler
    public void onAuthenticate(AlumiteAuthenticatedEvent event) {
        updateActivity(currentActivity);
    }

    @EventHandler
    public void onTitleScreenLoad(TitleScreenLoadEvent event) {
        updateActivity(Activity.IN_MAIN_MENU);
    }

    @EventHandler
    public void onSingleplayerWorldLoad(SingleplayerWorldLoadEvent event) {
        updateActivity(Activity.IN_SINGLEPLAYER);
    }

    @EventHandler
    public void onJoinMultiplayerServer(JoinMultiplayerServerEvent event) {
        updateActivity(Activity.IN_SERVER);
    }

    private void updateActivity(Activity activity) {
        SelfUser selfUser = Alumite.INSTANCE.users().selfUser();
        if (selfUser == null) {
            return;
        }
        selfUser.updateActivity(activity);
        currentActivity = activity;
    }
}
