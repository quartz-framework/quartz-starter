package xyz.quartzframework.data;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.val;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import xyz.quartzframework.data.interceptor.TransactionCleanupInterceptor;
import xyz.quartzframework.data.interceptor.TransactionalInterceptor;
import xyz.quartzframework.data.manager.DefaultJPATransactionManager;
import xyz.quartzframework.data.query.HQLQueryParser;
import xyz.quartzframework.data.query.JPAQueryExecutor;
import xyz.quartzframework.data.query.NativeQueryParser;
import xyz.quartzframework.data.query.SimpleQueryParser;
import xyz.quartzframework.data.storage.HibernateJPAStorage;
import xyz.quartzframework.data.storage.SimpleStorage;
import xyz.quartzframework.data.util.ProxyFactoryUtil;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserStorageTest {

    private UserStorage storage;
    private EntityManagerFactory emf;

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    @BeforeEach
    void setup() {
        if (emf != null) {
            emf.close();
        }
        emf = Persistence.createEntityManagerFactory("test-unit");
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new UserEntity(id1, "admin", true, Instant.now().minusSeconds(1000)));
            em.persist(new UserEntity(id2, "bob", false, Instant.now().minusSeconds(500)));
            em.persist(new UserEntity(UUID.randomUUID(), "carol", true, Instant.now()));

            em.persist(UserProfile.builder()
                    .id(UUID.randomUUID())
                    .displayName("Admin")
                    .country("BR")
                    .user(em.find(UserEntity.class, id1))
                    .build());

            em.getTransaction().commit();
        }
        SimpleStorage<UserEntity, UUID> target = new HibernateJPAStorage<>(emf, UserEntity.class, UUID.class);
        val interceptor = new TransactionalInterceptor(new DefaultJPATransactionManager(emf), false);
        val cleanupInterceptor = new TransactionCleanupInterceptor(false);
        var factory = ProxyFactoryUtil.createProxyFactory(
                new SimpleQueryParser(new HQLQueryParser(), new NativeQueryParser()),
                target,
                UserEntity.class,
                UserStorage.class,
                new JPAQueryExecutor<>(emf, UserEntity.class),
                interceptor, cleanupInterceptor
        );
        storage = (UserStorage) factory.getProxy();
    }

    @Test
    void testFindByUsername() {
        var result = storage.findByUsername("admin");
        assertEquals(1, result.size());
    }

    @Test
    void testExistsByUsername() {
        boolean exists = storage.countByUsername("admin") > 0;
        assertTrue(exists);
        boolean notExists = storage.countByUsername("nonexistent") == 0L;
        assertTrue(notExists);
    }

    @Test
    void testFindFirstByEnabledTrueOrderByCreatedAtDesc() {
        var result = storage.findFirstByEnabledTrueOrderByCreatedAtDesc();
        assertEquals("carol", result.map(UserEntity::getUsername).orElse(null));
    }

    @Test
    void testSaveAndFindAll() {
        UUID newId = UUID.randomUUID();
        var user = new UserEntity(newId, "dave", true, Instant.now());
        storage.save(user);
        var all = storage.findAll();
        assertEquals(4, all.size());
    }

    @Test
    void testCountByEnabledTrue() {
        long count = storage.countByEnabledTrue();
        assertEquals(2, count);
    }

    @Test
    void testCountByUsernameNonexistent() {
        long count = storage.countByUsername("nonexistent");
        assertEquals(0, count);
    }

    @Test
    void testFindByUsernameCaseSensitive() {
        var resultLower = storage.findByUsername("admin");
        var resultUpper = storage.findByUsername("Admin");
        assertEquals(1, resultLower.size());
        assertEquals(0, resultUpper.size());
    }

    @Test
    void testSaveSameIdUpdatesRecord() {
        var updated = new UserEntity(id1, "newName", false, Instant.now());
        storage.save(updated);

        var result = storage.findById(id1).orElseThrow();
        assertEquals("newName", result.getUsername());
        assertFalse(result.isEnabled());
    }

    @Test
    void testFindAllEmpty() {
        cleanup();
        emf = Persistence.createEntityManagerFactory("test-unit");
        storage = (UserStorage) ProxyFactoryUtil.createProxyFactory(
                new SimpleQueryParser(new HQLQueryParser(), new NativeQueryParser()),
                new HibernateJPAStorage<>(emf, UserEntity.class, UUID.class),
                UserEntity.class,
                UserStorage.class,
                new JPAQueryExecutor<>(emf, UserEntity.class)
        ).getProxy();
        var result = storage.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteUser() {
        storage.deleteById(id2);
        var found = storage.findByUsername("bob");
        assertTrue(found.isEmpty());
    }

    @Test
    void testUpdateUser() {
        storage.findById(id2).ifPresent(user -> {
            user.setUsername("robert");
            storage.save(user);
        });
        var updated = storage.findByUsername("robert");
        assertEquals(1, updated.size());
        assertEquals("robert", updated.get(0).getUsername());
    }

    @Test
    void testMultipleEnabledUsersOrdering() {
        var result = storage.findFirstByEnabledTrueOrderByCreatedAtDesc();
        assertTrue(result.isPresent());
        assertEquals("carol", result.get().getUsername());
    }

    @Test
    void testFindEnabledOrderedByCreatedAtDesc() {
        var result = storage.findEnabledOrderedByCreatedAtDesc();
        assertEquals(2, result.size());
        assertEquals("carol", result.get(0).getUsername()); // mais recente
        assertEquals("admin", result.get(1).getUsername());
    }

    @Test
    void testFindByUsernameLike() {
        var result = storage.findByUsernameLike("a%");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testNativeFindEnabledByUsernameLike() {
        var result = storage.nativeFindEnabledByUsernameLike("%ar%");
        assertEquals(1, result.size());
        assertEquals("carol", result.get(0).getUsername());
    }

    @Test
    void testNativeCountEnabledUsers() {
        storage.save(new UserEntity(UUID.randomUUID(), "x", true, Instant.now()));
        storage.save(new UserEntity(UUID.randomUUID(), "y", true, Instant.now()));
        long count = storage.nativeCountEnabledUsers();
        assertEquals(4, count);
    }

    @Test
    void testFindByUsernameEqualsBob() {
        var result = storage.findByUsername("bob");
        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).getUsername());
    }

    @Test
    void testFindByUsernameNotEqualCarol() {
        var all = storage.findAll();
        var result = all.stream().filter(u -> !u.getUsername().equals("carol")).toList();
        assertEquals(2, result.size());
    }

    @Test
    void testFindByUsernameLikeBPercent() {
        var result = storage.findByUsernameLike("b%");
        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).getUsername());
    }

    @Test
    void testFindByUsernameNotLikeCPercent() {
        var all = storage.findAll();
        var result = all.stream().filter(u -> !u.getUsername().startsWith("c")).toList();
        assertEquals(2, result.size());
    }

    @Test
    void testFindEnabledUsers() {
        var result = storage.findEnabledOrderedByCreatedAtDesc();
        assertTrue(result.stream().allMatch(UserEntity::isEnabled));
    }

    @Test
    void testFindDisabledUsers() {
        var all = storage.findAll();
        var disabled = all.stream().filter(u -> !u.isEnabled()).toList();
        assertEquals(1, disabled.size());
        assertEquals("bob", disabled.get(0).getUsername());
    }

    @Test
    void testFindCreatedAtGreaterThan() {
        var cutoff = Instant.now().minusSeconds(800);
        var result = storage.findAll().stream().filter(u -> u.getCreatedAt().isAfter(cutoff)).toList();
        assertTrue(result.size() >= 2);
    }

    @Test
    void testFindCreatedAtLessThan() {
        var cutoff = Instant.now().minusSeconds(800);
        var result = storage.findAll().stream().filter(u -> u.getCreatedAt().isBefore(cutoff)).toList();
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testFindByEnabledTrueAndUsernameLike() {
        var result = storage.findAll().stream()
                .filter(u -> u.isEnabled() && u.getUsername().contains("a"))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    void testFindByCreatedAtNotNull() {
        var result = storage.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    void testFindByEnabledInList() {
        List<Boolean> options = List.of(true, false);
        var result = storage.findAll().stream()
                .filter(u -> options.contains(u.isEnabled()))
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    void testFindByUsernameIn() {
        var result = storage.findByUsernameIn(List.of("admin", "carol"));
        assertEquals(2, result.size());
    }

    @Test
    void testFindByUsernameNotIn() {
        var result = storage.findByUsernameNotIn(List.of("bob"));
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(u -> u.getUsername().equals("bob")));
    }

    @Test
    void testFindEnabledCreatedAfter() {
        var cutoff = Instant.now().minusSeconds(800);
        var result = storage.findEnabledCreatedAfter(cutoff);
        assertEquals(1, result.size());
        assertEquals("carol", result.get(0).getUsername());
    }

    @Test
    void testFindCreatedBeforeOrdered() {
        var cutoff = Instant.now().minusSeconds(800);
        var result = storage.findCreatedBeforeOrdered(cutoff);
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testFindEnabledByUsernameLike() {
        var result = storage.findEnabledByUsernameLike("%a%");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(UserEntity::isEnabled));
    }

    @Test
    void testFindTop5EnabledOrderByUsernameDesc() {
        var result = storage.findTop5EnabledOrderByUsernameDesc();
        assertEquals(2, result.size());
        assertEquals("carol", result.get(0).getUsername());
        assertEquals("admin", result.get(1).getUsername());
    }

    @Test
    void testNativeFindEnabledWithNullEmail() {
        assertThrows(SQLGrammarException.class, () -> storage.nativeFindEnabledWithNullEmail());
    }

    @Test
    void testNativeFindRecentEnabledWithEmail() {
        assertThrows(SQLGrammarException.class, () -> storage.nativeFindRecentEnabledWithEmail());
    }

    @Test
    void testNativeFindByUsernameIn() {
        var result = storage.nativeFindByUsernameIn(List.of("admin", "carol"));
        assertEquals(2, result.size());
    }

    @Test
    void testNativeCountRecentEnabled() {
        var cutoff = Instant.now().minusSeconds(800);
        long count = storage.nativeCountRecentEnabled(cutoff);
        assertEquals(1, count);
    }

    @Test
    void testNativeFindByEnabledAndUsernamePattern() {
        var result = storage.nativeFindByEnabledAndUsernamePattern(true, "%a%");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.getUsername().contains("a")));
    }

    @Test
    void testNativeTop3EnabledByUsername() {
        var result = storage.nativeTop3EnabledByUsername("%a%");
        assertEquals(2, result.size());
    }

    @Test
    void testTransactionalRollback() {

        class Service {

            @Transactional
            public void execute(UserStorage storage) {
                storage.save(new UserEntity(UUID.randomUUID(), "tempUser", true, Instant.now()));
                throw new RuntimeException("Simulated failure");
            }
        }
        var service = ProxyFactoryUtil.proxyWithInterceptor(
                new Service(),
                new TransactionalInterceptor(new DefaultJPATransactionManager(emf), false)
        );
        assertThrows(RuntimeException.class, () -> service.execute(storage));
        var found = storage.findByUsername("tempUser");
        assertTrue(found.isEmpty());
    }

    @Test
    void testTransactionalRollbackWithoutException() {

        class Service {

            @Transactional
            public void execute(UserStorage storage) {
                storage.save(new UserEntity(UUID.randomUUID(), "tempUser", true, Instant.now()));
            }
        }
        var service = ProxyFactoryUtil.proxyWithInterceptor(
                new Service(),
                new TransactionalInterceptor(new DefaultJPATransactionManager(emf), false)
        );
        assertDoesNotThrow(() -> service.execute(storage));
        var found = storage.findByUsername("tempUser");
        assertFalse(found.isEmpty());
    }

    @Test
    void testTransactionalCommit() {

        class Service {

            @Transactional
            public void execute(UserStorage storage) {
                storage.save(new UserEntity(UUID.randomUUID(), "persistedUser", true, Instant.now()));
            }

        }

        var service = ProxyFactoryUtil.proxyWithInterceptor(
                new Service(),
                new TransactionalInterceptor(new DefaultJPATransactionManager(emf), false)
        );
        service.execute(storage);
        var found = storage.findByUsername("persistedUser");
        assertEquals(1, found.size());
    }

    @Test
    void testFindEnabledUsersWithProfile() {
        var result = storage.findEnabledUsersWithProfile();
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
        assertEquals("Admin", result.get(0).displayName());
    }

    @Test
    void testFindUserWithProfile() {
        var user = storage.findUserWithProfile("admin");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertNotNull(user.getProfile());
        assertEquals("Admin", user.getProfile().getDisplayName());
    }

    @Test
    void testCountUsersPerCountry() {
        var result = storage.countUsersPerCountry();
        assertFalse(result.isEmpty());
        var country = (String) result.get(0)[0];
        var count = (Long) result.get(0)[1];
        assertEquals("BR", country);
        assertEquals(1L, count);
    }

    @Test
    void testExistsUserInCountry() {
        assertTrue(storage.existsUserInCountry("BR"));
        assertFalse(storage.existsUserInCountry("US"));
    }

    @Test
    void testFindUsernamesByCountry() {
        var result = storage.findUsernamesByCountry("BR");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0)[0]);
        assertEquals("BR", result.get(0)[1]);
    }

    @Test
    void testFindRecentEnabledUsersBetween() {
        var start = Instant.now().minusSeconds(2000);
        var end = Instant.now();
        var result = storage.findRecentEnabledUsersBetween(start, end);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(UserEntity::isEnabled));
    }

    @Test
    void testFindUsernamesExcluding() {
        var result = storage.findUsernamesExcluding("c%", List.of("bob"));
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testExistsEnabledInCountry() {
        assertTrue(storage.existsEnabledInCountry("BR"));
        assertFalse(storage.existsEnabledInCountry("US"));
    }

    @Test
    void testCountUsersFromCountry() {
        long count = storage.countUsersFromCountry("BR");
        assertEquals(1, count);
    }

    @Test
    void testFindUserSummaries() {
        var result = storage.findUserSummaries();
        assertFalse(result.isEmpty());
        assertEquals("admin", result.get(0).username());
    }

    @Test
    void testNativeFindUsersByCountry() {
        var result = storage.nativeFindUsersByCountry("BR");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testNativeCountUsersCreatedToday() {
        long count = storage.nativeCountUsersCreatedToday();
        assertTrue(count >= 1);
    }

    @Test
    void testFindEnabledWithDefinedCountry() {
        var result = storage.findEnabledWithDefinedCountry();
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testFindUserSummariesByCountry() {
        var result = storage.findUserSummariesByCountry("BR");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
    }

    @Test
    void testCountEnabledUsersPerCountryExcluding() {
        var result = storage.countEnabledUsersPerCountryExcluding(List.of("US", "AR"));
        assertEquals(1, result.size());
        assertEquals("BR", result.get(0)[0]);
        assertEquals(1L, result.get(0)[1]);
    }

    @Test
    void testExistsUsersWithPattern() {
        assertTrue(storage.existsUsersWithPattern("a%"));
        assertFalse(storage.existsUsersWithPattern("zzz%"));
    }

    @Test
    void testFindRecentEnabledWithCountry() {
        var result = storage.findRecentEnabledWithCountry();
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testFindUsernamesInCountries() {
        var result = storage.findUsernamesInCountries(List.of("BR"));
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0)[0]);
        assertEquals("BR", result.get(0)[1]);
    }

    @Test
    void testCountUsersAfterDateByCountry() {
        var result = storage.countUsersAfterDateByCountry(Instant.now().minusSeconds(2000));
        assertEquals(1, result.size());
        assertEquals("BR", result.get(0)[0]);
        assertEquals(1L, result.get(0)[1]);
    }

    @Test
    void testFindEnabledUsersNotInUsernames() {
        var result = storage.findEnabledUsersNotInUsernames(List.of("carol"));
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    void testFindUserWithProfileFetched() {
        var user = storage.findUserWithProfileFetched("admin");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertNotNull(user.getProfile());
        assertEquals("Admin", user.getProfile().getDisplayName());
    }

    @Test
    void testFindUsersWithoutProfile() {
        var result = storage.findUsersWithoutProfile();
        var usernames = result.stream().map(UserEntity::getUsername).toList();
        assertTrue(usernames.contains("bob") || usernames.contains("carol"));
    }

    @Test
    void testFindSummariesByCountryOrdered() {
        var result = storage.findSummariesByCountryOrdered("BR");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
    }

    @Test
    void testFindUsersInNonExcludedCountries() {
        var result = storage.findUsersInNonExcludedCountries(List.of("US", "AR"));
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0)[0]);
    }

    @Test
    void testFindDistinctCountriesOfEnabledUsers() {
        var result = storage.findDistinctCountriesOfEnabledUsers();
        assertTrue(result.contains("BR"));
    }

    @Test
    void testCountDistinctProfilesOfEnabledUsers() {
        cleanup();
        emf = Persistence.createEntityManagerFactory("test-unit");
        val storage = (UserStorage) ProxyFactoryUtil.createProxyFactory(
                new SimpleQueryParser(new HQLQueryParser(), new NativeQueryParser()),
                new HibernateJPAStorage<>(emf, UserEntity.class, UUID.class),
                UserEntity.class,
                UserStorage.class,
                new JPAQueryExecutor<>(emf, UserEntity.class)
        ).getProxy();
        val profileStorage = (UserProfileStorage) ProxyFactoryUtil.createProxyFactory(
                new SimpleQueryParser(new HQLQueryParser(), new NativeQueryParser()),
                new HibernateJPAStorage<>(emf, UserProfile.class, UUID.class),
                UserProfile.class,
                UserProfileStorage.class,
                new JPAQueryExecutor<>(emf, UserProfile.class)
        ).getProxy();
        val user1 = new UserEntity();
        user1.setId(UUID.randomUUID());
        user1.setUsername("enabled_with_profile");
        user1.setEnabled(true);
        user1.setCreatedAt(Instant.now());
        storage.save(user1);

        val profile1 = new UserProfile();
        profile1.setId(UUID.randomUUID());
        profile1.setDisplayName("Profile 1");
        profile1.setCountry("BR");
        profile1.setUser(user1);
        profileStorage.save(profile1);

        val user2 = new UserEntity();
        user2.setId(UUID.randomUUID());
        user2.setUsername("disabled_with_profile");
        user2.setEnabled(false);
        user2.setCreatedAt(Instant.now());
        storage.save(user2);

        val profile2 = new UserProfile();
        profile2.setId(UUID.randomUUID());
        profile2.setDisplayName("Profile 2");
        profile2.setCountry("US");
        profile2.setUser(user2);
        profileStorage.save(profile2);

        val user3 = new UserEntity();
        user3.setId(UUID.randomUUID());
        user3.setUsername("enabled_no_profile");
        user3.setEnabled(true);
        user3.setCreatedAt(Instant.now());
        storage.save(user3);
        var result = storage.findAll();
        result.forEach(s -> System.out.println("user: " + s.toString()));
        long count = profileStorage.countProfilesWithEnabledUsers();
        assertEquals(1, count);
    }

    @Test
    void testFindUsersCreatedAfterWithCountry() {
        var result = storage.findUsersCreatedAfterWithCountry(Instant.now().minusSeconds(24000));
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(r -> "BR".equals(r[0])));
        assertTrue(result.stream().anyMatch(r -> "BR".equals(r[0]) && "admin".equals(r[1])));
    }

    @Test
    void testFindUsernamesWithCountryLike() {
        var result = storage.findUsernamesWithCountryLike("a%");
        assertTrue(result.contains("admin"));
    }

    @Test
    void testFindCreatedBetweenAndProfileEmpty() {
        var start = Instant.now().minusSeconds(2000);
        var end = Instant.now().plusSeconds(10);
        var result = storage.findCreatedBetweenAndProfileEmpty(start, end);
        var usernames = result.stream().map(UserEntity::getUsername).toList();
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("carol"));
        assertFalse(usernames.contains("admin"));
        assertEquals(2, usernames.size());
    }

    @AfterEach
    void cleanup() {
        if (emf != null) emf.close();
    }
}