package ca.gbc.comp3074.sakugavault.services.profile;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.SakugaVaultApi;
import ca.gbc.comp3074.sakugavault.api.dto.ProfileSummaryDto;

public final class ProfileService implements IProfileService {

    private final SakugaVaultApi api;

    public ProfileService(Context context) {
        this.api = ApiClient.api(context);
    }

    @Override
    public void getMyProfile(ApiCallback<ProfileSummaryDto> callback) {
        ApiClient.enqueue(api.myProfile(), callback);
    }
}
