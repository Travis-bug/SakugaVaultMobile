package ca.gbc.comp3074.sakugavault.services.profile;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.dto.ProfileSummaryDto;

/** The Profile screen's data. Mirrors the back end's Services/Profile/IProfileService. */
public interface IProfileService {

    /** GET /api/profile/me: counts and recent watch history. */
    void getMyProfile(ApiCallback<ProfileSummaryDto> callback);
}
