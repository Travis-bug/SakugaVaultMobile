package ca.gbc.comp3074.sakugavault.services;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import ca.gbc.comp3074.sakugavault.services.auth.AuthService;
import ca.gbc.comp3074.sakugavault.services.auth.IAuthService;
import ca.gbc.comp3074.sakugavault.services.catalog.CatalogService;
import ca.gbc.comp3074.sakugavault.services.catalog.ICatalogService;
import ca.gbc.comp3074.sakugavault.services.profile.IProfileService;
import ca.gbc.comp3074.sakugavault.services.profile.ProfileService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchHistoryService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchListService;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchPageService;
import ca.gbc.comp3074.sakugavault.services.watch.WatchHistoryService;
import ca.gbc.comp3074.sakugavault.services.watch.WatchListService;
import ca.gbc.comp3074.sakugavault.services.watch.WatchPageService;

/**
 * One place to get each service, like the back end's dependency-injection setup
 * (services.AddScoped&lt;IAuthService, AuthService&gt;() ...). Screens ask for the
 * interface, never the class, so an implementation can be swapped without touching them.
 */
public final class AppServices {

    private static IAuthService auth;
    private static ICatalogService catalog;
    private static IWatchPageService watchPage;
    private static IWatchHistoryService watchHistory;
    private static IWatchListService watchList;
    private static IProfileService profile;

    private AppServices() {
    }

    public static synchronized IAuthService auth(Context context) {
        if (auth == null) {
            auth = new AuthService(context.getApplicationContext());
        }
        return auth;
    }

    public static synchronized ICatalogService catalog(Context context) {
        if (catalog == null) {
            catalog = new CatalogService(context.getApplicationContext());
        }
        return catalog;
    }

    public static synchronized IWatchPageService watchPage(Context context) {
        if (watchPage == null) {
            watchPage = new WatchPageService(context.getApplicationContext());
        }
        return watchPage;
    }

    public static synchronized IWatchHistoryService watchHistory(Context context) {
        if (watchHistory == null) {
            watchHistory = new WatchHistoryService(context.getApplicationContext());
        }
        return watchHistory;
    }

    public static synchronized IWatchListService watchList(Context context) {
        if (watchList == null) {
            watchList = new WatchListService(context.getApplicationContext(), watchPage(context));
        }
        return watchList;
    }

    public static synchronized IProfileService profile(Context context) {
        if (profile == null) {
            profile = new ProfileService(context.getApplicationContext());
        }
        return profile;
    }
}
