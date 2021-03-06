package win.doyto.query.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import win.doyto.query.config.GlobalConfiguration;
import win.doyto.query.core.test.TestEntity;
import win.doyto.query.core.test.TestEnum;
import win.doyto.query.core.test.TestQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CrudBuilderTest
 *
 * @author f0rb
 */
class CrudBuilderTest {

    private DynamicEntity dynamicEntity;
    private List<Object> argList;
    private CrudBuilder<TestEntity> testEntityCrudBuilder = new CrudBuilder<>(TestEntity.class);
    private CrudBuilder<DynamicEntity> dynamicEntityCrudBuilder = new CrudBuilder<>(DynamicEntity.class);

    @BeforeEach
    void setUp() {

        dynamicEntity = new DynamicEntity();
        dynamicEntity.setUser("f0rb");
        dynamicEntity.setProject("i18n");
        dynamicEntity.setLocale("zh");
        dynamicEntity.setValue("中文");
        dynamicEntity.setScore(100);

        argList = new ArrayList<>();
    }

    @Test
    void create() {
        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildCreateAndArgs(new TestEntity());
        String expected = "INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        assertEquals(expected, sqlAndArgs.getSql());
    }

    @Test
    void update() {
        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildUpdateAndArgs(new TestEntity());
        String expected = "UPDATE user SET username = ?, password = ?, mobile = ?, email = ?, nickname = ?, userLevel = ?, memo = ?, valid = ? WHERE id = ?";
        assertEquals(expected, sqlAndArgs.getSql());
    }

    @Test
    void createDynamicEntity() {

        SqlAndArgs sqlAndArgs = dynamicEntityCrudBuilder.buildCreateAndArgs(dynamicEntity);
        assertEquals("INSERT INTO t_dynamic_f0rb_i18n (locale_zh, user_score, memo) VALUES (?, ?, ?)", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("中文", 100, null);
    }

    @Test
    void updateDynamicEntity() {
        dynamicEntity.setId(1);

        SqlAndArgs sqlAndArgs = dynamicEntityCrudBuilder.buildUpdateAndArgs(dynamicEntity);
        assertEquals("UPDATE t_dynamic_f0rb_i18n SET locale_zh = ?, user_score = ?, memo = ? WHERE id = ?", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("中文", 100, null, 1);
    }


    @Test
    void buildPatchAndArgs() {
        dynamicEntity.setId(1);
        dynamicEntity.setValue(null);
        dynamicEntity.setScore(null);
        dynamicEntity.setMemo("memo");

        SqlAndArgs sqlAndArgs = dynamicEntityCrudBuilder.buildPatchAndArgsWithId(dynamicEntity);

        assertEquals("UPDATE t_dynamic_f0rb_i18n SET memo = ? WHERE id = ?", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("memo", 1);
    }

    @Test
    void replaceTableName() {

        DynamicEntity entity = new DynamicEntity();
        entity.setId(1);
        entity.setUser("f0rb");
        entity.setProject("i18n");

        assertEquals("t_dynamic_f0rb_i18n", CommonUtil.replaceHolderInString(entity, DynamicEntity.TABLE));
        assertEquals("user", CommonUtil.replaceHolderInString(new TestEntity(), TestEntity.TABLE));

    }
    @Test
    void fixReplaceTableNameWithTail() {

        DynamicEntity entity = new DynamicEntity();
        entity.setId(1);
        entity.setUser("f0rb");
        entity.setProject("i18n");

        assertEquals("t_dynamic_f0rb_i18n_any", CommonUtil.replaceHolderInString(entity, "t_dynamic_${user}_${project}" + "_any"));

    }

    @Test
    void supportMapFieldToUnderscore() {
        GlobalConfiguration.instance().setMapCamelCaseToUnderscore(true);

        try {
            TestEntity testEntity = new TestEntity();
            testEntity.setId(1);
            testEntity.setUserLevel(TestEnum.VIP);
            testEntity.setValid(true);

            SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildPatchAndArgsWithId(testEntity);

            assertEquals("UPDATE user SET user_level = ?, valid = ? WHERE id = ?", sqlAndArgs.getSql());
            assertThat(sqlAndArgs.getArgs()).containsExactly(0, true, 1);
        } finally {
            GlobalConfiguration.instance().setMapCamelCaseToUnderscore(false);
        }
    }

    @Test
    void buildPatchAndArgsWithQuery() {
        TestEntity testEntity = new TestEntity();
        testEntity.setNickname("测试");

        TestQuery testQuery = TestQuery.builder().username("test").pageNumber(2).pageSize(5).build();

        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildPatchAndArgsWithQuery(testEntity, testQuery);

        assertEquals("UPDATE user SET nickname = ? WHERE username = ? LIMIT 5", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("测试", "test");
    }

    @Test
    void createMulti() {
        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildCreateAndArgs(Arrays.asList(new TestEntity(), new TestEntity(), new TestEntity()));
        assertEquals(
            "INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?)", sqlAndArgs.sql);
    }

    @Test
    void supportDynamicTableName() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("i18n").scoreLt(100).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_i18n WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(100);
    }

    @Test
    void fixSQLInject() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("; DROP TABLE menu;").scoreLt(80).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_${project} WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(80);
    }

    @Test
    void buildDeleteAndArgs() {
        TestQuery testQuery = TestQuery.builder().username("test").build();
        testQuery.setPageNumber(3).setPageSize(10);

        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildDeleteAndArgs(testQuery);

        assertEquals("DELETE FROM user WHERE username = ? LIMIT 10", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("test");
    }

    @Test
    void supportUnderlineScore() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("i18n_0001").scoreLt(100).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_i18n_0001 WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(100);
    }

    @Test
    void createMultiOnDuplicate() {
        SqlAndArgs sqlAndArgs = testEntityCrudBuilder.buildCreateAndArgs(
                Arrays.asList(new TestEntity(), new TestEntity(), new TestEntity()),
                "mobile", "email"
        );
        assertEquals(
                "INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE " +
                        "mobile = VALUES (mobile), " +
                        "email = VALUES (email)",
                sqlAndArgs.sql);
    }

    @Test
    void buildPatchAndArgsForDynamicColumn() {
        dynamicEntity.setId(1);
        SqlAndArgs sqlAndArgs = dynamicEntityCrudBuilder.buildPatchAndArgsWithId(dynamicEntity);
        assertEquals("UPDATE t_dynamic_f0rb_i18n SET locale_zh = ?, user_score = ? WHERE id = ?", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly("中文", 100, 1);
    }

    @Test
    void buildDeleteAndArgsForDynamicTable() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("i18n").scoreLt(100).pageSize(10).build();
        SqlAndArgs sqlAndArgs = dynamicEntityCrudBuilder.buildDeleteAndArgs(dynamicQuery);
        assertEquals("DELETE FROM t_dynamic_f0rb_i18n WHERE score < ? LIMIT 10", sqlAndArgs.getSql());
        assertThat(sqlAndArgs.getArgs()).containsExactly(100);
    }
}