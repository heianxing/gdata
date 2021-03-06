package com.gsralex.gdata.jdbctemplate;

import com.gsralex.gdata.DataSourceConfg;
import com.gsralex.gdata.domain.Foo;
import com.gsralex.gdata.domain.FooSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author gsralex
 * @date 2018/3/10
 */
public class JdbcTemplateUtilsTest {

    private JdbcTemplateUtils templateUtils;


    @Before
    public void setUpBeforeClass() throws Exception {
        templateUtils = new JdbcTemplateUtils(DataSourceConfg.getDataSource());
    }

    @Test
    public void insert() throws Exception {
        Foo foo1 = new Foo();
        foo1.setFoo1("1234");
        foo1.setFoo2(123.13);
        foo1.setFoo3(new Date());
        foo1.setFoo4(1);
        Assert.assertEquals(true, templateUtils.insert(foo1));
        Assert.assertEquals(0, foo1.getId());

        Foo foo2 = new Foo();
        foo2.setFoo1("1234");
        foo2.setFoo2(123.13);
        foo2.setFoo3(new Date());
        foo2.setFoo4(1);
        Assert.assertEquals(true, templateUtils.insert(foo2, true));
        Assert.assertNotEquals(0, foo2.getId());
    }

    @Test
    public void batchInsert() throws Exception {
        List<Foo> fooList = FooSource.getEntityList();
        Assert.assertEquals(templateUtils.batchInsert(fooList), 2);
    }

    @Test
    public void update() throws Exception {
        Foo foo = FooSource.getEntity();
        templateUtils.insert(foo, true);
        foo.setFoo1("111");
        foo.setFoo2(111);
        Date now = new Date();
        foo.setFoo3(now);
        foo.setFoo4(111);

        templateUtils.update(foo);
        Foo data = templateUtils.get("select * from t_foo where id=?", new Object[]{foo.getId()}, Foo.class);
        Assert.assertEquals(data.getFoo1(), foo.getFoo1());
        Assert.assertEquals(data.getFoo4(), foo.getFoo4());

    }

    @Test
    public void batchUpdate() throws Exception {
        Foo foo1 = FooSource.getEntity();
        Foo foo2 = FooSource.getEntity();
        List<Foo> list = new ArrayList<>();
        list.add(foo1);
        list.add(foo2);
        Assert.assertEquals(templateUtils.batchUpdate(list), 2);
    }

    @Test
    public void get() throws Exception {
        Foo foo1 = new Foo();
        foo1.setFoo1("1234");
        foo1.setFoo2(123.13);
        foo1.setFoo3(new Date());
        foo1.setFoo4(1);
        Assert.assertEquals(true, templateUtils.insert(foo1, true));
        Foo foo1Data = templateUtils.get("select * from t_foo where id=?", new Object[]{foo1.getId()}, Foo.class);
        Assert.assertNotEquals(foo1Data, null);

        String cnt = templateUtils.get("select count(1) from t_foo ", null, String.class);
        Assert.assertNotEquals(cnt, null);
    }

    @Test
    public void getList() throws Exception {
        Foo foo1 = FooSource.getEntity();
        Assert.assertEquals(true, templateUtils.insert(foo1, true));
        List<Foo> fooList = templateUtils.getList("select * from t_foo where id=?", new Object[]{foo1.getId()}, Foo.class);
        Assert.assertEquals(fooList.size(), 1);
    }

    @Test
    public void getJdbcTemplate() throws Exception {

    }

}