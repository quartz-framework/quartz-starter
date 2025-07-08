package xyz.quartzframework.data;

import xyz.quartzframework.data.annotation.Storage;
import xyz.quartzframework.data.query.Query;
import xyz.quartzframework.data.storage.JPAStorage;
import xyz.quartzframework.data.storage.SimpleStorage;

import java.util.UUID;

@Storage
public interface UserProfileStorage extends JPAStorage<UserProfile, UUID> {

    @Query("select count(distinct p.id) from UserProfile p where p.user.enabled = true")
    long countProfilesWithEnabledUsers();

}