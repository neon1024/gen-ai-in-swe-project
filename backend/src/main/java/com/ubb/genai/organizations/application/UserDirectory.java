package com.ubb.genai.organizations.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Display names of user accounts, owned by the authentication module. */
public interface UserDirectory {

    /** Display name per known user id; unknown ids are absent from the result. */
    Map<UUID, String> displayNames(Collection<UUID> userIds);
}
