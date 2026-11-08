package com.metaphasicgroup.luma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.widget.RemoteViews;

/**
 * Luma clock widget — ORIGINAL code written for Phase 6 (no widget library).
 *
 * <p>Shows the time and the date with two {@code TextClock} views, so the
 * widget updates itself from the system's time broadcasts. There is no
 * manual update logic and no periodic work: the provider info sets
 * {@code updatePeriodMillis="0"} and {@code onUpdate()} only (re)binds the
 * layout and the tap action. {@code TextClock} honors the user's 12/24-hour
 * setting, so both formats are declared in the layout.</p>
 *
 * <p>Tap opens the system clock / alarms screen via
 * {@code AlarmClock.ACTION_SHOW_ALARMS} ("android.intent.action.SHOW_ALARMS").
 * That action needs no permission (unlike ACTION_SET_ALARM / ACTION_SET_TIMER,
 * which require SET_ALARM). If the device has no activity handling it, the
 * tap is a no-op rather than a crash.</p>
 *
 * <p>Integration (done by the integration coordinator, not here): declare
 * this class in the manifest as a {@code <receiver>} with
 * {@code android:exported="true"}, an {@code APPWIDGET_UPDATE} intent filter,
 * and {@code android.appwidget.provider} meta-data pointing at
 * {@code @xml/clock_widget_info}. See MANIFEST_SNIPPET.xml in this staging
 * directory.</p>
 */
public class ClockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_clock);

            Intent clockIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, clockIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_clock_root, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
