package com.gsralex.gdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gsralex
 * @version 2018/2/18
 */
public class Mapper {

    private String tableName;
    private Class type;
    private Map<String, FieldColumn> mapper;
    private Map<FieldEnum, List<FieldColumn>> fieldMapper;

    public Mapper() {
        mapper = new HashMap<>();
        fieldMapper = new HashMap<>();
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public Map<String, FieldColumn> getMapper() {
        return mapper;
    }

    public Map<FieldEnum, List<FieldColumn>> getFieldMapper() {
        return fieldMapper;
    }

}
