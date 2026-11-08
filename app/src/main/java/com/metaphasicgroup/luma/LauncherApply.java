package com.metaphasicgroup.luma;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Per-launcher apply routes. ORIGINAL code; intent strings below are ONLY
 *  those sourced in Phase 3 (see README § Apply intents + sources):
 *  Nova / Action / ADW / Apex direct intents come from the CandyBar
 *  LauncherHelper summary (DeepWiki) and, for Nova, Nova's own manifest
 *  (IconThemePrompt handles com.teslacoilsw.launcher.APPLY_ICON_THEME).
 *  Every other launcher is a settings-launch or documented manual path —
 *  no intent string is guessed for them. All routes are UNTESTED against
 *  Luma until Phase 4; the dashboard always shows the manual fallback.
 */
public class LauncherApply {
    public enum Method { DIRECT_NOVA, DIRECT_ACTION, DIRECT_ADW, DIRECT_APEX, SETTINGS_LAUNCH, MANUAL }

    public static class Launcher {
        final String name; final String[] pkgs; final Method method; final String manualSteps;
        Launcher(String n, String[] p, Method m, String steps) { name = n; pkgs = p; method = m; manualSteps = steps; }
    }
    public static class Flavor {
        final String label; final String pkg;
        Flavor(String l, String p) { label = l; pkg = p; }
    }
    public static final Flavor[] FLAVORS = {
        new Flavor("Regular", "com.metaphasicgroup.luma.regular"),
        new Flavor("Dark", "com.metaphasicgroup.luma.dark"),
        new Flavor("Gradient", "com.metaphasicgroup.luma.gradient"),
        new Flavor("Glyph", "com.metaphasicgroup.luma.glyph"),
        new Flavor("Clear", "com.metaphasicgroup.luma.clear"),
        new Flavor("Prism", "com.metaphasicgroup.luma.prism"),
        new Flavor("Midnight", "com.metaphasicgroup.luma.midnight"),
    };
    public static final Launcher[] LAUNCHERS = {
        new Launcher("Nova Launcher", new String[]{"com.teslacoilsw.launcher"}, Method.DIRECT_NOVA,
            "Nova Settings > Look & feel > Icon theme > select this Luma set."),
        new Launcher("Action Launcher", new String[]{"com.actionlauncher.playstore"}, Method.DIRECT_ACTION,
            "Home settings > Appearance > Icon pack > select this Luma set."),
        new Launcher("ADW Launcher 2", new String[]{"org.adw.launcher"}, Method.DIRECT_ADW,
            "ADW Settings > Theme settings > select this Luma set. (Exact ADW 2 path to confirm in Phase 4.)"),
        new Launcher("Apex Launcher", new String[]{"com.anddoes.launcher"}, Method.DIRECT_APEX,
            "Apex settings > Theme settings > Icon Pack > select this Luma set."),
        new Launcher("Lawnchair", new String[]{"ch.deletescape.lawnchair.plah", "app.lawnchair", "app.lawnchair.play"}, Method.MANUAL,
            "Home Settings > General > Icon Style > select this Luma set."),
        new Launcher("Niagara Launcher", new String[]{"bitpit.launcher"}, Method.MANUAL,
            "Niagara Settings > find the icon-pack setting > select this Luma set. (Exact path to confirm in Phase 4.)"),
        new Launcher("Smart Launcher", new String[]{"ginlemon.flowerfree"}, Method.MANUAL,
            "Smart Launcher settings > find the icon-pack setting > select this Luma set. (Exact path to confirm in Phase 4.)"),
        new Launcher("Hyperion Launcher", new String[]{"projekt.launcher"}, Method.SETTINGS_LAUNCH,
            "Open Hyperion settings and choose this Luma set as the icon pack. (Exact path to confirm in Phase 4.)"),
        new Launcher("Poco Launcher", new String[]{"com.mi.android.globallauncher"}, Method.SETTINGS_LAUNCH,
            "Open Poco settings and choose this Luma set as the icon pack. (Exact path to confirm in Phase 4.)"),
        new Launcher("Microsoft Launcher", new String[]{"com.microsoft.launcher"}, Method.MANUAL,
            "Microsoft Launcher settings > Personalization > Change icon pack > select this Luma set."),
    };

    public static boolean isInstalled(Context ctx, Launcher l) {
        PackageManager pm = ctx.getPackageManager();
        for (String p : l.pkgs) {
            try { pm.getPackageInfo(p, 0); return true; } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return false;
    }
    private static String installedPkg(Context ctx, Launcher l) {
        PackageManager pm = ctx.getPackageManager();
        for (String p : l.pkgs) {
            try { pm.getPackageInfo(p, 0); return p; } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return l.pkgs[0];
    }

    /** Attempts the sourced direct route; returns a human-readable result.
     *  Direct routes carry no invented extras: Nova's prompt is addressed
     *  to the Nova package with the sourced action; if Nova does not
     *  pre-select Luma, the caller shows the manual path. */
    public static String apply(Context ctx, Launcher l, String packPackage) {
        try {
            switch (l.method) {
                case DIRECT_NOVA: {
                    Intent i = new Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME");
                    i.setPackage(installedPkg(ctx, l));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return "Sent Nova's apply request. If Nova did not pre-select Luma, pick it in the theme list.";
                }
                case DIRECT_ACTION: {
                    Intent i = ctx.getPackageManager().getLaunchIntentForPackage(installedPkg(ctx, l));
                    if (i == null) return "Could not open Action Launcher.";
                    i.putExtra("apply_icon_pack", packPackage);
                    ctx.startActivity(i);
                    return "Opened Action Launcher with the apply request.";
                }
                case DIRECT_ADW: {
                    Intent i = new Intent("org.adw.launcher.SET_THEME");
                    i.setPackage(installedPkg(ctx, l));
                    ctx.sendBroadcast(i);
                    return "Sent ADW's theme broadcast. Check ADW's theme settings if nothing changed.";
                }
                case DIRECT_APEX: {
                    Intent i = new Intent("com.anddoes.launcher.SET_THEME");
                    i.setPackage(installedPkg(ctx, l));
                    ctx.sendBroadcast(i);
                    return "Sent Apex's theme broadcast. Check Apex's theme settings if nothing changed.";
                }
                case SETTINGS_LAUNCH: {
                    Intent i = ctx.getPackageManager().getLaunchIntentForPackage(installedPkg(ctx, l));
                    if (i == null) return "Could not open this launcher.";
                    ctx.startActivity(i);
                    return "Opened the launcher — choose this Luma set in its icon-pack setting.";
                }
                default:
                    return "This launcher needs manual selection (see below).";
            }
        } catch (Exception e) {
            return "Direct apply failed (" + e.getClass().getSimpleName() + "). Use the manual path below.";
        }
    }
}
