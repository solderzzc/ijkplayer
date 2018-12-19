package tv.danmaku.ijk.media.example.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tv.danmaku.ijk.media.example.activities.SetupActivity;
import tv.danmaku.ijk.media.example.activities.VideoActivity;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, VideoActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
