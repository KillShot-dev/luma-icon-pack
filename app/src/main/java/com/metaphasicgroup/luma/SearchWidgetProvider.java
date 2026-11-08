package com.metaphasicgroup.luma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Luma search widget — ORIGINAL code written for Phase 6 (no widget library).
 *
 * <p>A single Luma-styled search bar. It is a display-only bar, not an input
 * field: {@code RemoteViews} layouts cannot contain an {@code EditText}, so
 * the layout uses a {@code TextView} showing the "Search" hint plus a
 * magnifier glyph. Tapping the bar fires the standard web-search intent,
 * {@code Intent.ACTION_WEB_SEARCH} ("android.intent.action.WEB_SEARCH"),
 * with no query extra, so the device's search handler opens its own (empty)
 * search UI. No query text is invented or attached.</p>
 *
 * <p>There is nothing to refresh, so the provider info sets
 * {@code updatePeriodMillis="0"} and {@code onUpdate()} only (re)binds the
 * layout and the tap action.</p>
 *
 * <p>Integration (done by the integration coordinator, not here): declare
 * this class in the manifest as a {@code <receiver>} with
 * {@code android:exported="true"}, an {@code APPWIDGET_UPDATE} intent filter,
 * and {@code android.appwidget.provider} meta-data pointing at
 * {@code @xml/search_widget_info}. See MANIFEST_SNIPPET.xml in this staging
 * directory.</p>
 */
public class SearchWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_search);

            Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, searchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_search_root, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
