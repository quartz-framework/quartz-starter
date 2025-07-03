package xyz.quartzframework.data;

import xyz.quartzframework.data.annotation.Storage;
import xyz.quartzframework.data.query.Query;
import xyz.quartzframework.data.query.QueryParameter;
import xyz.quartzframework.data.storage.JPAStorage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Storage
public interface UserStorage extends JPAStorage<UserEntity, UUID> {

    @Query("find distinct where username = ?1")
    List<UserEntity> findByUsername(String username);

    Optional<UserEntity> findFirstByEnabledTrueOrderByCreatedAtDesc();

    @Query("from UserEntity u where u.enabled = true")
    long countByEnabledTrue();

    @Query("select count(*) from users u where u.username = ?1")
    long countByUsername(String username);

    @Query("find where enabled = true order by createdAt desc")
    List<UserEntity> findEnabledOrderedByCreatedAtDesc();

    @Query("from UserEntity u where u.username like ?1")
    List<UserEntity> findByUsernameLike(String pattern);

    @Query(value = "select * from users where enabled = true and username like ?1", nativeQuery = true)
    List<UserEntity> nativeFindEnabledByUsernameLike(String pattern);

    @Query(value = "select count(*) from users where enabled = true", nativeQuery = true)
    long nativeCountEnabledUsers();

    @Query("from UserEntity u where u.username in (?1)")
    List<UserEntity> findByUsernameIn(List<String> usernames);

    @Query("from UserEntity u where u.username not in (?1)")
    List<UserEntity> findByUsernameNotIn(List<String> usernames);

    @Query("from UserEntity u where u.createdAt > ?1 and u.enabled = true")
    List<UserEntity> findEnabledCreatedAfter(Instant since);

    @Query("from UserEntity u where u.createdAt < ?1 order by createdAt desc")
    List<UserEntity> findCreatedBeforeOrdered(Instant date);

    @Query("from UserEntity u where u.username like ?1 and u.enabled = true")
    List<UserEntity> findEnabledByUsernameLike(String pattern);

    @Query("from UserEntity u where u.enabled = true order by username desc limit 5")
    List<UserEntity> findTop5EnabledOrderByUsernameDesc();

    @Query(value = "select * from users where email is null and enabled = true", nativeQuery = true)
    List<UserEntity> nativeFindEnabledWithNullEmail();

    @Query(value = "select * from users where email is not null and enabled = true order by createdAt desc limit 10", nativeQuery = true)
    List<UserEntity> nativeFindRecentEnabledWithEmail();

    @Query(value = "select * from users where username in (?1)", nativeQuery = true)
    List<UserEntity> nativeFindByUsernameIn(List<String> usernames);

    @Query(value = "select count(*) from users where createdAt > ?1 and enabled = true", nativeQuery = true)
    long nativeCountRecentEnabled(Instant since);

    @Query(value = "select * from users where enabled = :enabled and username like :pattern", nativeQuery = true)
    List<UserEntity> nativeFindByEnabledAndUsernamePattern(
            @QueryParameter("enabled") boolean enabled,
            @QueryParameter("pattern") String pattern
    );

    @Query(value = "select * from users where username like ?1 and enabled = true order by createdAt desc limit 3", nativeQuery = true)
    List<UserEntity> nativeTop3EnabledByUsername(String pattern);
}