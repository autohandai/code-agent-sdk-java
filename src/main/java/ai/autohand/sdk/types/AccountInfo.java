package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Information about the authenticated CLI account.
 *
 * @param email the account email address
 * @param organization the organization name, if applicable
 * @param subscriptionType the subscription tier
 * @see AutohandSDK#accountInfo()
 */
public record AccountInfo(
    String email,
    @JsonInclude(JsonInclude.Include.NON_NULL) String organization,
    @JsonInclude(JsonInclude.Include.NON_NULL) String subscriptionType
) {
}
