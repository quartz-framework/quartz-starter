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

    @Query("select count(u) from UserEntity u where u.enabled = true")
    long countByEnabledTrue();

    @Query(value = "select count(*) from users u where u.username = ?1", nativeQuery = true)
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

    @Query("select new xyz.quartzframework.data.UserWithProfileDTO(u.id, u.username, p.displayName) " +
            "from UserEntity u join u.profile p where u.enabled = true")
    List<UserWithProfileDTO> findEnabledUsersWithProfile();

    @Query("from UserEntity u join fetch u.profile where u.username = :username")
    UserEntity findUserWithProfile(@QueryParameter("username") String username);

    @Query("select p.country, count(u) from UserEntity u join u.profile p group by p.country")
    List<Object[]> countUsersPerCountry();

    @Query("exists where profile.country = :country")
    boolean existsUserInCountry(@QueryParameter("country") String country);

    @Query("select u.username, p.country from UserEntity u join u.profile p where p.country = :country")
    List<Object[]> findUsernamesByCountry(@QueryParameter("country") String country);

    @Query("from UserEntity u where u.createdAt >= :start and u.createdAt <= :end and u.enabled = true order by u.createdAt desc limit 10")
    List<UserEntity> findRecentEnabledUsersBetween(@QueryParameter("start") Instant start, @QueryParameter("end") Instant end);

    @Query("from UserEntity u where u.username not like :prefix and u.username not in (:list)")
    List<UserEntity> findUsernamesExcluding(
            @QueryParameter("prefix") String pattern,
            @QueryParameter("list") List<String> excluded
    );

    @Query("select count(u) > 0 from UserEntity u join u.profile p where p.country = :country and u.enabled = true")
    boolean existsEnabledInCountry(@QueryParameter("country") String country);

    @Query("select count(u) from UserEntity u join u.profile p where p.country = :country")
    long countUsersFromCountry(@QueryParameter("country") String country);

    @Query("select new xyz.quartzframework.data.UserSummaryDTO(u.id, u.username, u.createdAt) from UserEntity u where u.enabled = true")
    List<UserSummaryDTO> findUserSummaries();

    @Query(value = "select u.* from users u join user_profiles p on p.user_id = u.id where p.country = ?1 order by u.createdAt desc limit 5", nativeQuery = true)
    List<UserEntity> nativeFindUsersByCountry(String country);

    @Query(value = """
    select count(*)
    from users
    where createdAt >= cast(current_date as timestamp)
      and createdAt < cast(current_date + 1 as timestamp)
""", nativeQuery = true)
    long nativeCountUsersCreatedToday();

    @Query("from UserEntity u join u.profile p where u.enabled = true and p.country is not null order by u.createdAt desc")
    List<UserEntity> findEnabledWithDefinedCountry();

    @Query("select new xyz.quartzframework.data.UserSummaryDTO(u.id, u.username, u.createdAt) " +
            "from UserEntity u join u.profile p where p.country = :country order by u.createdAt")
    List<UserSummaryDTO> findUserSummariesByCountry(@QueryParameter("country") String country);

    @Query("select p.country, count(u) from UserEntity u join u.profile p " +
            "where p.country not in (:excluded) and u.enabled = true group by p.country")
    List<Object[]> countEnabledUsersPerCountryExcluding(@QueryParameter("excluded") List<String> excludedCountries);

    @Query("select count(u) > 0 from UserEntity u join u.profile p where u.username like :pattern")
    boolean existsUsersWithPattern(@QueryParameter("pattern") String usernamePattern);

    @Query("from UserEntity u join u.profile p where u.enabled = true and p.country is not null order by u.createdAt desc limit 10")
    List<UserEntity> findRecentEnabledWithCountry();

    @Query("select u.username, p.country from UserEntity u join u.profile p where p.country in (:countries)")
    List<Object[]> findUsernamesInCountries(@QueryParameter("countries") List<String> countries);

    @Query("select p.country, count(u) from UserEntity u join u.profile p where u.createdAt > :after group by p.country")
    List<Object[]> countUsersAfterDateByCountry(@QueryParameter("after") Instant after);

    @Query("from UserEntity u where u.username not in (:excluded) and u.enabled = true")
    List<UserEntity> findEnabledUsersNotInUsernames(@QueryParameter("excluded") List<String> usernames);

    @Query("from UserEntity u left join u.profile p where u.enabled = true")
    List<Object[]> findEnabledUsersWithOrWithoutProfile();

    @Query("from UserEntity u join fetch u.profile p where u.username = :username")
    UserEntity findUserWithProfileFetched(@QueryParameter("username") String username);

    @Query("from UserEntity u left join u.profile p where p.id is null")
    List<UserEntity> findUsersWithoutProfile();

    @Query("select new xyz.quartzframework.data.UserSummaryDTO(u.id, u.username, u.createdAt) " +
            "from UserEntity u join u.profile p where p.country = :country and u.enabled = true order by u.createdAt desc")
    List<UserSummaryDTO> findSummariesByCountryOrdered(@QueryParameter("country") String country);

    @Query("select u.username, p.country from UserEntity u join u.profile p where p.country not in (:excluded)")
    List<Object[]> findUsersInNonExcludedCountries(@QueryParameter("excluded") List<String> excludedCountries);

    @Query("select distinct p.country from UserEntity u join u.profile p where u.enabled = true")
    List<String> findDistinctCountriesOfEnabledUsers();

    @Query("select p.country, u.username from UserEntity u join u.profile p where u.createdAt > :after order by p.country")
    List<Object[]> findUsersCreatedAfterWithCountry(@QueryParameter("after") Instant after);

    @Query("select u.username from UserEntity u join u.profile p where p.country is not null and u.username like :pattern")
    List<String> findUsernamesWithCountryLike(@QueryParameter("pattern") String pattern);

    @Query("from UserEntity u where u.createdAt between :start and :end and u.profile is null")
    List<UserEntity> findCreatedBetweenAndProfileEmpty(@QueryParameter("start") Instant start, @QueryParameter("end") Instant end);
}