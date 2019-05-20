package win.doyto.query.module.user;

import win.doyto.query.jpa2.AbstractMockRepository;

/**
 * MockUserRepository
 *
 * @author f0rb
 */
public class MockUserRepository extends AbstractMockRepository<UserEntity, Long, UserQuery> implements UserRepository {
    public MockUserRepository() {
        super(UserEntity.TABLE);
    }
}
