package bio.terra.pearl.core.dao;

import bio.terra.pearl.core.model.BaseEntity;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.result.RowView;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class BaseJdbiDao<T extends BaseEntity> {
    protected Jdbi jdbi;
    protected List<String> insertFields;
    protected List<String> insertFieldSymbols;
    protected List<String> insertColumns;
    protected String updateFieldString;

    protected List<String> getQueryFields;
    protected List<String> getQueryFieldSymbols;
    protected List<String> getQueryColumns;

    protected String tableName;
    protected Class<T> clazz;

    protected abstract Class<T> getClazz();

    protected RowMapper<T> getRowMapper() {
        return BeanMapper.of(getClazz());
    }
    protected RowMapper<T> getRowMapper(String prefix) {
        return BeanMapper.of(getClazz(), prefix);
    }

    protected List<String> getInsertExcludedFields() {
        return Arrays.asList("id", "class");
    }

    protected boolean isSimpleFieldType(Class fieldType) {
        return Enum.class.isAssignableFrom(fieldType) ||
                Arrays.asList(String.class, Boolean.class, Instant.class, boolean.class, int.class, UUID.class)
                        .contains(fieldType);
    }

    protected List<String> generateInsertFields(Class clazz) {
        try {
            BeanInfo info = Introspector.getBeanInfo(clazz);
            List<String> allSimpleProperties = Arrays.asList(info.getPropertyDescriptors()).stream()
                    .filter(descriptor -> isSimpleFieldType(descriptor.getPropertyType()))
                    .filter(descriptor -> !getInsertExcludedFields().contains(descriptor.getName()))
                    .map(descriptor -> descriptor.getName())
                    .collect(Collectors.toList());
            return allSimpleProperties;
        } catch (IntrospectionException e) {
            throw new RuntimeException("Unable to introspect " + getClazz().getName());
        }
    }

    protected List<String> generateGetFields(Class clazz) {
        try {
            BeanInfo info = Introspector.getBeanInfo(clazz);
            List<String> allSimpleProperties = Arrays.asList(info.getPropertyDescriptors()).stream()
                    .filter(descriptor -> isSimpleFieldType(descriptor.getPropertyType()))
                    .map(descriptor -> descriptor.getName())
                    .collect(Collectors.toList());
            return allSimpleProperties;
        } catch (IntrospectionException e) {
            throw new RuntimeException("Unable to introspect " + getClazz().getName());
        }
    }

    protected List<String> generateInsertColumns(List<String> insertFields) {
        return insertFields.stream().map(field -> toSnakeCase(field))
                .collect(Collectors.toList());
    }

    protected List<String> generateGetColumns(List<String> insertFields) {
        return insertFields.stream().map(field -> toSnakeCase(field))
                .collect(Collectors.toList());
    }

    protected String generateUpdateFieldString(List<String> insertFieldSymbols, List<String> insertColumns) {
        return IntStream
                .range(0, insertFieldSymbols.size())
                .mapToObj(i -> insertColumns.get(i) + " = " + insertFieldSymbols.get(i))
                .collect(Collectors.joining(", "));
    }

    protected String getTableName() {
        return toSnakeCase(getClazz().getSimpleName());
    };

    public BaseJdbiDao(Jdbi jdbi) {
        this.jdbi = jdbi;
        clazz = getClazz();
        insertFields = generateInsertFields(clazz);
        insertFieldSymbols = insertFields.stream().map(field -> ":" + field).collect(Collectors.toList());
        insertColumns = generateInsertColumns(insertFields);
        getQueryFields = generateGetFields(clazz);
        getQueryFieldSymbols = getQueryFields.stream().map(field -> ":" + field).collect(Collectors.toList());
        getQueryColumns = generateGetColumns(getQueryFields);
        updateFieldString = generateUpdateFieldString(insertFieldSymbols, insertColumns);
        tableName = getTableName();
        initializeRowMapper(jdbi);
    }

    protected void initializeRowMapper(Jdbi jdbi) {
        jdbi.registerRowMapper(clazz, getRowMapper());
    }

    public T create(T modelObj) {
        if (modelObj.getId() != null) {
            throw new IllegalArgumentException("object passed to create already has id - " + modelObj.getId());
        }
        return jdbi.withHandle(handle ->
                handle.createUpdate("insert into " + tableName + " (" + StringUtils.join(insertColumns, ", ") +") " +
                                "values (" + StringUtils.join(insertFieldSymbols, ", ") + ");")
                        .bindBean(modelObj)
                        .executeAndReturnGeneratedKeys()
                        .mapTo(clazz)
                        .one()
        );
    }

    /** basic get-by-id */
    public Optional<T> find(UUID id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("select * from " + tableName + " where id = :id;")
                        .bind("id", id)
                        .mapTo(clazz)
                        .findOne()
        );
    }

    protected Optional<T> findWithChild(UUID id, String childIdPropertyName, String childPropertyName, BaseJdbiDao childDao) {
        List<String> parentCols = getQueryColumns.stream().map(col -> "a." + col + " a_" + col)
                .collect(Collectors.toList());
        List<String> childCols = ((List<String>) childDao.getQueryColumns).stream().map(col -> "b." + col + " b_" + col)
                .collect(Collectors.toList());
        return jdbi.withHandle(handle ->
                handle.createQuery("select " + String.join(", ", parentCols) + ", "
                        + String.join(", ", childCols)
                        + " from " + tableName + " a left join " + childDao.tableName
                        + " b on a." + toSnakeCase(childIdPropertyName) + " = b.id"
                        + " where a.id = :id")
                        .bind("id", id)
                        .registerRowMapper(clazz, getRowMapper("a"))
                        .registerRowMapper(childDao.clazz, childDao.getRowMapper("b"))
                        .reduceRows((Map<UUID, T> map, RowView rowView) -> {
                            T parent = map.computeIfAbsent(
                                    rowView.getColumn("a_id", UUID.class),
                                    rowId -> rowView.getRow(clazz));
                            if (rowView.getColumn("b_id", UUID.class) != null) {
                                PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(parent);
                                accessor.setPropertyValue(childPropertyName, rowView.getRow(childDao.getClazz()));
                            }

                        })
                        .findFirst()
        );
    }

    protected Optional<T> findByProperty(String columnName, Object columnValue) {
        return jdbi.withHandle(handle ->
                handle.createQuery("select * from " + tableName + " where " + columnName + " = :columnValue;")
                        .bind("columnValue", columnValue)
                        .mapTo(clazz)
                        .findOne()
        );
    }

    protected List<T> findAllByProperty(String columnName, Object columnValue) {
        return jdbi.withHandle(handle ->
                handle.createQuery("select * from " + tableName + " where " + columnName + " = :columnValue;")
                        .bind("columnValue", columnValue)
                        .mapTo(clazz)
                        .list()
        );
    }

    /** defaults to matching on id if provided. */
    public Optional<T> findOneMatch(T matchObj) {
        if (matchObj.getId() != null) {
            return find(matchObj.getId());
        }
        return Optional.empty();
    }

    public void delete(UUID id) {
        jdbi.withHandle(handle ->
                handle.createUpdate("delete from " + tableName + " where id = :id;")
                        .bind("id", id)
                        .execute()
        );
    }

    protected void deleteByUuidProperty(String columnName, UUID columnValue) {
        jdbi.withHandle(handle ->
                handle.createUpdate("delete from " + tableName + " where " + columnName + " = :propertyValue;")
                        .bind("propertyValue", columnValue)
                        .execute()
        );
    }

    protected void deleteByParentUuid(String parentColumnName, UUID parentUUID, BaseJdbiDao parentDao) {
        jdbi.withHandle(handle ->
                handle.createUpdate("delete from " + tableName + " join  " + parentDao.tableName + " on "
                        + tableName + ".id = " + parentDao.tableName + "." + parentColumnName
                        + " where " + parentColumnName + " = :parentUUID;")
                        .bind("parentUUID", parentUUID)
                        .execute()
        );
    }

    // from https://stackoverflow.com/questions/10310321/regex-for-converting-camelcase-to-camel-case-in-java
    protected static String toSnakeCase(String camelCased) {
        return camelCased.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }
}
