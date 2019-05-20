package win.doyto.query.mybatis;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * MapperTable
 *
 * @author f0rb
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface MapperTable {
    String value();
}
