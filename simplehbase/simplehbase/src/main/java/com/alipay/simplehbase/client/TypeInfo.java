package com.alipay.simplehbase.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alipay.simplehbase.exception.SimpleHBaseException;
import com.alipay.simplehbase.util.Util;

/**
 * POJO type and Hbase table mapping info.
 * 
 * @author xinzhi
 * */
public class TypeInfo {

    /**
     * Parse TypeInfo from POJO's type.
     * 
     * @param type POJO's type.
     * @return TypeInfo.
     * */
    public static TypeInfo parse(Class<?> type) {
        Util.checkNull(type);

        TypeInfo typeInfo = new TypeInfo();
        typeInfo.type = type;

        Field[] fields = type.getDeclaredFields();

        int versionFieldCounter = 0;

        for (Field field : fields) {

            field.setAccessible(true);

            ColumnInfo columnInfo = ColumnInfo.parse(type, field);
            if (columnInfo == null) {
                continue;
            }

            typeInfo.columnInfos.add(columnInfo);

            if (field.isAnnotationPresent(HBaseVersion.class)) {
                typeInfo.versionedColumnInfo = columnInfo;
                versionFieldCounter++;
            }

            typeInfo.families.add(columnInfo.family);
        }

        if (versionFieldCounter > 1) {
            throw new SimpleHBaseException("more than one versioned fields.");
        }

        if (typeInfo.columnInfos.isEmpty()) {
            throw new SimpleHBaseException("columnInfos is empty.");
        }

        return typeInfo;
    }

    /** POJO's type. */
    private Class<?>         type;
    /** POJO's ColumnInfo list. */
    private List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();
    /** Versioned ColumnInfo, can be null. */
    private ColumnInfo       versionedColumnInfo;
    /** All the family name of this type info. */
    private Set<String>      families    = new HashSet<String>();

    /**
     * Is versioned TypeInfo.
     * */
    public boolean isVersionedType() {
        return versionedColumnInfo != null;
    }

    public Class<?> getType() {
        return type;
    }

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public ColumnInfo getVersionedColumnInfo() {
        return versionedColumnInfo;
    }

    public Set<String> getFamilies() {
        return families;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-----------------" + getClass()
                + "-----------------------\n");
        sb.append("type=" + type + "\n");
        sb.append("versionedColumnInfo=" + versionedColumnInfo + "\n");
        sb.append("families=" + families + "\n");
        for (ColumnInfo columnInfo : columnInfos) {
            sb.append(columnInfo + "\n");
        }
        sb.append("-----------------" + getClass()
                + "-----------------------\n");
        return sb.toString();
    }
}
