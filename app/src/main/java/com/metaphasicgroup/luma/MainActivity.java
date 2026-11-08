package com.metaphasicgroup.luma;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Luma dashboard — ORIGINAL code written for Phase 3 (no dashboard library).
 *  Tabs: Icons (searchable grid of this flavor's icons), Apply, Sets,
 *  Wallpapers (Phase 6: preview + set the 10 Luma wallpapers),
 *  Request, About. The Sets tab is deliberately honest: it does NOT claim
 *  to switch the applied set in place, because launchers read a pack's
 *  appfilter.xml + drawables statically from the installed APK. Switching
 *  sets means installing/selecting a different Luma flavor APK in the
 *  launcher. See README § "The 4-set delivery decision".
 */
public class MainActivity extends AppCompatActivity {
    private String applyPackPackage; // pack the Apply tab targets (own or a sibling flavor)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyPackPackage = getPackageName();

        TextView setTitle = findViewById(R.id.set_title);
        setTitle.setText("Luma — " + capitalize(BuildConfig.FLAVOR) + "  ·  PROTOTYPE");
        TextView setSub = findViewById(R.id.set_subtitle);
        setSub.setText(IconCatalog.ICON_NAMES.length + " icons  ·  " + BuildConfig.FLAVOR
                + " set  ·  " + getString(R.string.app_name));

        RecyclerView grid = findViewById(R.id.icon_grid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        IconAdapter adapter = new IconAdapter(this, IconCatalog.ICON_NAMES, IconCatalog.ICON_DRAWABLES);
        grid.setAdapter(adapter);

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { adapter.filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        showTab(R.id.tab_icons);
        findViewById(R.id.btn_icons).setOnClickListener(v -> showTab(R.id.tab_icons));
        findViewById(R.id.btn_apply).setOnClickListener(v -> { buildApplyList(); showTab(R.id.tab_apply); });
        findViewById(R.id.btn_sets).setOnClickListener(v -> { buildSetsList(); showTab(R.id.tab_sets); });
        findViewById(R.id.btn_wallpapers).setOnClickListener(v -> { buildWallpapersList(); showTab(R.id.tab_wallpapers); });
        findViewById(R.id.btn_request).setOnClickListener(v -> showTab(R.id.tab_request));
        findViewById(R.id.btn_about).setOnClickListener(v -> showTab(R.id.tab_about));

        findViewById(R.id.btn_copy_request).setOnClickListener(v -> {
            String text = requestText();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Luma icon request", text));
            Toast.makeText(this, "Request copied — paste it into an email or message.", Toast.LENGTH_LONG).show();
        });
        findViewById(R.id.btn_share_request).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Luma icon request");
            i.putExtra(Intent.EXTRA_TEXT, requestText());
            startActivity(Intent.createChooser(i, "Send icon request"));
        });
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void showTab(int id) {
        for (int tab : new int[]{R.id.tab_icons, R.id.tab_apply, R.id.tab_sets, R.id.tab_wallpapers, R.id.tab_request, R.id.tab_about}) {
            findViewById(tab).setVisibility(tab == id ? View.VISIBLE : View.GONE);
        }
    }

    /** Phase 6 Wallpapers tab: a preview of each of the 10 wallpapers
     *  (design/wallpapers/, shared across flavors) with a "Set wallpaper"
     *  action per wallpaper. WallpaperManager.setResource with
     *  FLAG_SYSTEM | FLAG_LOCK (API 24+; minSdk here is 26) applies the
     *  wallpaper to the home and lock screens. SET_WALLPAPER is a normal
     *  permission, so no runtime request is needed. */
    private void buildWallpapersList() {
        LinearLayout list = findViewById(R.id.wallpaper_list);
        list.removeAllViews();
        TextView note = findViewById(R.id.wallpaper_note);
        // Phase 7 per-flavor sets: Clear and Midnight wallpapers are
        // documented 3 light / 3 dark / 4 mid-tone; Prism's tone split is
        // not documented, so its note states no split. Existing flavors
        // keep the shared Phase 6 wording exactly.
        if (BuildConfig.FLAVOR.equals("prism")) {
            note.setText("10 Prism wallpapers (1080 x 1920) in the Prism palette. "
                    + "Tap Set wallpaper to apply one to your home and lock screens. "
                    + "Widgets (clock, search) are added from your launcher's widget picker, not here.");
        } else {
            note.setText("10 Luma wallpapers (1080 x 1920) in the Luma palette: 3 light, 3 dark, "
                    + "4 mid-tone. Tap Set wallpaper to apply one to your home and lock screens. "
                    + "Widgets (clock, search) are added from your launcher's widget picker, not here.");
        }
        int[] res = {R.drawable.wallpaper_01, R.drawable.wallpaper_02, R.drawable.wallpaper_03,
                R.drawable.wallpaper_04, R.drawable.wallpaper_05, R.drawable.wallpaper_06,
                R.drawable.wallpaper_07, R.drawable.wallpaper_08, R.drawable.wallpaper_09,
                R.drawable.wallpaper_10};
        String[] labels = {"Wallpaper 1 — Light", "Wallpaper 2 — Light", "Wallpaper 3 — Light",
                "Wallpaper 4 — Dark", "Wallpaper 5 — Dark", "Wallpaper 6 — Dark",
                "Wallpaper 7 — Mid-tone", "Wallpaper 8 — Mid-tone", "Wallpaper 9 — Mid-tone",
                "Wallpaper 10 — Mid-tone"};
        for (int i = 0; i < res.length; i++) {
            final int resid = res[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            ImageView preview = new ImageView(this);
            preview.setImageResource(resid);
            preview.setAdjustViewBounds(true);
            row.addView(preview);
            Button b = new Button(this);
            b.setText("Set wallpaper — " + labels[i]);
            b.setOnClickListener(v -> {
                try {
                    WallpaperManager wm = WallpaperManager.getInstance(this);
                    wm.setResource(resid, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                    Toast.makeText(this, "Wallpaper set (home + lock screen).", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Could not set wallpaper (" + e.getClass().getSimpleName() + ").",
                            Toast.LENGTH_LONG).show();
                }
            });
            row.addView(b);
            list.addView(row);
        }
    }

    private void buildApplyList() {
        LinearLayout list = findViewById(R.id.apply_list);
        list.removeAllViews();
        for (LauncherApply.Launcher l : LauncherApply.LAUNCHERS) {
            boolean installed = LauncherApply.isInstalled(this, l);
            Button b = new Button(this);
            b.setText(l.name + (installed ? "  (installed)" : "  (not installed)"));
            b.setOnClickListener(v -> {
                if (!installed) {
                    new AlertDialog.Builder(this)
                        .setTitle(l.name)
                        .setMessage("This launcher is not installed. Install it first, then return here.\n\n"
                                + "Manual path:\n" + l.manualSteps)
                        .setPositiveButton("OK", null).show();
                    return;
                }
                String result = LauncherApply.apply(this, l, applyPackPackage);
                new AlertDialog.Builder(this)
                    .setTitle(l.name)
                    .setMessage(result + "\n\nIf the pack did not apply, use the manual path:\n" + l.manualSteps)
                    .setPositiveButton("OK", null).show();
            });
            list.addView(b);
        }
        TextView note = findViewById(R.id.apply_note);
        note.setText("Applying: " + applyPackPackage + "\n"
                + "Samsung One UI Home does not apply third-party packs natively — use Good Lock → "
                + "Theme Park → Icon → Create New → Iconpack (documented route, not yet tested against Luma). "
                + "The stock Pixel launcher does not support third-party icon packs.");
    }

    private void buildSetsList() {
        LinearLayout list = findViewById(R.id.sets_list);
        list.removeAllViews();
        TextView explain = findViewById(R.id.sets_explain);
        explain.setText("Why switching sets needs a different install: launchers read an icon pack's "
                + "appfilter.xml and drawables statically from the installed APK. No in-dashboard "
                + "picker can change the set a launcher applies — any dashboard that claimed otherwise "
                + "would only change its own preview. To use another set, install that set's Luma app "
                + "and select it in your launcher (or use Apply below for an installed sibling).");
        for (LauncherApply.Flavor f : LauncherApply.FLAVORS) {
            boolean isCurrent = f.pkg.equals(getPackageName());
            boolean installed = isCurrent || isPackageInstalled(f.pkg);
            Button b = new Button(this);
            b.setText(f.label + (isCurrent ? "  (this install)" : installed ? "  (installed)" : "  (not installed)"));
            b.setOnClickListener(v -> {
                if (isCurrent) { Toast.makeText(this, "This is the set you have open.", Toast.LENGTH_SHORT).show(); return; }
                if (!installed) {
                    new AlertDialog.Builder(this).setTitle(f.label)
                        .setMessage("This set is not installed. Install the " + f.label + " Luma app, "
                                + "then select it in your launcher's icon-pack setting.")
                        .setPositiveButton("OK", null).show();
                    return;
                }
                applyPackPackage = f.pkg;
                buildApplyList();
                showTab(R.id.tab_apply);
                Toast.makeText(this, "Apply tab now targets " + f.label + ". Pick your launcher there.",
                        Toast.LENGTH_LONG).show();
            });
            list.addView(b);
        }
    }

    private boolean isPackageInstalled(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private String requestText() {
        String app = ((EditText) findViewById(R.id.req_app)).getText().toString();
        String pkg = ((EditText) findViewById(R.id.req_pkg)).getText().toString();
        String note = ((EditText) findViewById(R.id.req_note)).getText().toString();
        return "Luma icon request (" + BuildConfig.FLAVOR + " set)\nApp: " + app
                + "\nPackage: " + pkg + "\nNotes: " + note;
    }
}
