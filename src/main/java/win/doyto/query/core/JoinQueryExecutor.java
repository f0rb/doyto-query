package win.doyto.query.core;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import win.doyto.query.service.PageList;

import java.util.List;

/**
 * JoinQueryExecutor
 *
 * @author f0rb on 2019-06-09
 */
@AllArgsConstructor
public class JoinQueryExecutor<E, Q extends PageQuery> {

    @Autowired
    private JdbcOperations jdbcOperations;
    private final JoinQueryBuilder joinQueryBuilder;
    private BeanPropertyRowMapper<E> beanPropertyRowMapper;

    public JoinQueryExecutor(Class<E> entityClass) {
        this.joinQueryBuilder = new JoinQueryBuilder(entityClass);
        this.beanPropertyRowMapper = new BeanPropertyRowMapper<>(entityClass);
    }

    public JoinQueryExecutor(JdbcOperations jdbcOperations, Class<E> entityClass) {
        this(entityClass);
        this.jdbcOperations = jdbcOperations;
    }

    public PageList<E> page(Q q) {
        return new PageList<>(query(q), count(q));
    }

    public List<E> query(Q q) {
        SqlAndArgs sqlAndArgs = buildJoinSelectAndArgs(q);
        return jdbcOperations.query(sqlAndArgs.getSql(), beanPropertyRowMapper, sqlAndArgs.getArgs());
    }

    public Long count(Q q) {
        SqlAndArgs sqlAndArgs = joinQueryBuilder.buildJoinCountAndArgs(q);
        return jdbcOperations.queryForObject(sqlAndArgs.getSql(), sqlAndArgs.getArgs(), Long.class);
    }

    SqlAndArgs buildJoinSelectAndArgs(Q q) {
        return joinQueryBuilder.buildJoinSelectAndArgs(q);
    }
}
