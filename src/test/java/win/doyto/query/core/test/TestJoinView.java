package win.doyto.query.core.test;

import lombok.Getter;
import lombok.Setter;
import win.doyto.query.annotation.Joins;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * TestJoinView
 *
 * @author f0rb on 2019-06-09
 */
@Getter
@Setter
@Table(name = "user u")
@Joins({
    @Joins.Join("left join user_and_role ur on ur.userId = u.id"),
    @Joins.Join("left join role r on r.id = ur.roleId and r.roleName = #{roleName}")
})
public class TestJoinView {

    @Column(name = "u.username")
    private String username;

    @Column(name = "r.roleName")
    private String roleName;
}