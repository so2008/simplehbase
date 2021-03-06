package com.alipay.simplehbase.client;

import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.coprocessor.AggregationClient;
import org.apache.hadoop.hbase.client.coprocessor.LongColumnInterpreter;
import org.apache.hadoop.hbase.filter.Filter;

import com.alipay.simplehbase.antlr.auto.StatementsParser.ProgContext;
import com.alipay.simplehbase.antlr.manual.TreeUtil;
import com.alipay.simplehbase.config.HBaseColumnSchema;
import com.alipay.simplehbase.exception.SimpleHBaseException;
import com.alipay.simplehbase.hql.HBaseQuery;
import com.alipay.simplehbase.util.StringUtil;
import com.alipay.simplehbase.util.Util;

/**
 * SimpleHbaseClient default implementation.
 * 
 * @author xinzhi
 * */
public class SimpleHbaseClientImpl extends SimpleHbaseClientBase {

    @Override
    public <T> T findObject(RowKey rowKey, Class<? extends T> type) {
        List<T> result = findObjectList(rowKey, rowKey, type);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Override
    public <T> T findObject(RowKey rowKey, Class<? extends T> type, String id,
            Map<String, Object> para) {
        List<T> result = findObjectList(rowKey, rowKey, type, id, para);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Override
    public <T> T findObjectByRawHql(RowKey rowKey, Class<? extends T> type,
            String hql, Map<String, Object> para) {
        List<T> result = findObjectListByRawHql(rowKey, rowKey, type, hql, para);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Override
    public <T> List<T> findObjectList(RowKey startRowKey, RowKey endRowKey,
            Class<? extends T> type) {
        return findObjectList(startRowKey, endRowKey, type, 0L, Long.MAX_VALUE);
    }

    @Override
    public <T> List<T> findObjectList(RowKey startRowKey, RowKey endRowKey,
            Class<? extends T> type, long startIndex, long length) {
        return findObjectList(startRowKey, endRowKey, type, startIndex, length,
                null);
    }

    @Override
    public <T> List<T> findObjectList(RowKey startRowKey, RowKey endRowKey,
            Class<? extends T> type, String id, Map<String, Object> para) {
        return findObjectList(startRowKey, endRowKey, type, 0L, Long.MAX_VALUE,
                id, para);

    }

    @Override
    public <T> List<T> findObjectList(RowKey startRowKey, RowKey endRowKey,
            Class<? extends T> type, long startIndex, long length, String id,
            Map<String, Object> para) {
        HBaseQuery hbaseQuery = getHbaseTableConfig().getQueryMap().get(id);
        Util.checkNull(hbaseQuery);

        StringBuilder sb = new StringBuilder();
        Map<Object, Object> context = new HashMap<Object, Object>();
        hbaseQuery.getHqlNode().applyParaMap(para, sb, context);

        String hql = sb.toString().trim();

        return findObjectListByRawHql(startRowKey, endRowKey, type, startIndex,
                length, hql, para);
    }

    @Override
    public <T> List<T> findObjectListByRawHql(RowKey startRowKey,
            RowKey endRowKey, Class<? extends T> type, String hql,
            Map<String, Object> para) {
        return findObjectListByRawHql(startRowKey, endRowKey, type, 0L,
                Long.MAX_VALUE, hql, para);
    }

    @Override
    public <T> List<T> findObjectListByRawHql(RowKey startRowKey,
            RowKey endRowKey, Class<? extends T> type, long startIndex,
            long length, String hql, Map<String, Object> para) {

        if (StringUtil.isEmptyString(hql)) {
            return findObjectList(startRowKey, endRowKey, type);
        }

        ProgContext progContext = TreeUtil.parse(hql);
        Filter filter = TreeUtil.parseSelectFilter(progContext,
                hbaseTableConfig, para);

        return findObjectList(startRowKey, endRowKey, type, startIndex, length,
                filter);
    }

    @Override
    public <T> void putObject(RowKey rowKey, T t) {
        Util.checkRowKey(rowKey);
        Util.checkNull(t);

        Put put = new Put(rowKey.toBytes());
        TypeInfo typeInfo = TypeInfoHolder.findTypeInfo(t.getClass());
        for (ColumnInfo columnInfo : typeInfo.getColumnInfos()) {
            byte[] value = convertPOJOFieldToBytes(t, columnInfo);
            put.add(columnInfo.familyBytes, columnInfo.qualifierBytes, value);
        }

        HTableInterface htableInterface = htableInterface();

        try {
            htableInterface.put(put);
        } catch (IOException e) {
            throw new SimpleHBaseException("putObject. rowkey=" + rowKey
                    + " t=" + t, e);
        } finally {
            Util.close(htableInterface);
        }
    }

    @Override
    public void deleteObject(RowKey rowKey) {
        Util.checkRowKey(rowKey);

        HTableInterface htableInterface = htableInterface();
        Delete delete = new Delete(rowKey.toBytes());
        try {
            htableInterface.delete(delete);
        } catch (IOException e) {
            throw new SimpleHBaseException("deleteObject. rowkey=" + rowKey, e);
        } finally {
            Util.close(htableInterface);
        }
    }

    @Override
    public void deleteObjectList(RowKey startRowKey, RowKey endRowKey) {
        Util.checkRowKey(startRowKey);
        Util.checkRowKey(endRowKey);

        final int deleteBatch = getDeleteBatch();

        while (true) {

            Scan scan = new Scan();
            scan.setStartRow(startRowKey.toBytes());
            scan.setStopRow(endRowKey.toBytes());
            scan.setCaching(getScanCaching());

            List<Delete> deletes = new LinkedList<Delete>();

            HTableInterface htableInterface = htableInterface();
            ResultScanner resultScanner = null;
            try {
                resultScanner = htableInterface.getScanner(scan);
                Result result = null;
                while ((result = resultScanner.next()) != null) {
                    deletes.add(new Delete(result.getRow()));
                    if (deletes.size() >= deleteBatch) {
                        break;
                    }
                }

            } catch (IOException e) {
                throw new SimpleHBaseException("deleteObjectList. startRowKey="
                        + startRowKey + " endRowKey=" + endRowKey, e);
            } finally {
                Util.close(resultScanner);
                Util.close(htableInterface);
            }

            final int deleteListSize = deletes.size();
            if (deleteListSize == 0) {
                return;
            }

            try {
                htableInterface = htableInterface();
                htableInterface.delete(deletes);
            } catch (IOException e) {
                throw new SimpleHBaseException("deleteObjectList. startRowKey="
                        + startRowKey + " endRowKey=" + endRowKey, e);
            } finally {
                Util.close(htableInterface);
            }

            //successful delete will clear the items of deletes list.
            if (deletes.size() > 0) {
                throw new SimpleHBaseException(
                        "deleteObjectList fail. deletes=" + deletes);
            }

            if (deleteListSize < deleteBatch) {
                return;
            }
        }
    }

    @Override
    public <T> boolean insertObject(RowKey rowKey, T t) {
        Util.checkRowKey(rowKey);
        Util.checkNull(t);

        TypeInfo typeInfo = TypeInfoHolder.findTypeInfo(t.getClass());
        checkVersioned(typeInfo);

        Put put = new Put(rowKey.toBytes());
        for (ColumnInfo columnInfo : typeInfo.getColumnInfos()) {
            byte[] value = convertPOJOFieldToBytes(t, columnInfo);
            put.add(columnInfo.familyBytes, columnInfo.qualifierBytes, value);
        }

        HTableInterface htableInterface = htableInterface();

        boolean result = false;
        ColumnInfo versionedColumnInfo = typeInfo.getVersionedColumnInfo();
        try {
            result = htableInterface.checkAndPut(rowKey.toBytes(),
                    versionedColumnInfo.familyBytes,
                    versionedColumnInfo.qualifierBytes, null, put);
        } catch (IOException e) {
            throw new SimpleHBaseException("insertObject. rowkey=" + rowKey
                    + " t=" + t, e);
        } finally {
            Util.close(htableInterface);
        }
        return result;
    }

    @Override
    public <T> boolean updateObject(RowKey rowKey, T oldT, T newT) {
        Util.checkRowKey(rowKey);
        Util.checkNull(oldT);
        Util.checkNull(newT);
        Util.checkIdentityType(oldT, newT);

        TypeInfo typeInfo = TypeInfoHolder.findTypeInfo(newT.getClass());
        checkVersioned(typeInfo);

        Put put = new Put(rowKey.toBytes());
        try {
            boolean needUpdate = false;
            for (ColumnInfo columnInfo : typeInfo.getColumnInfos()) {
                Object oldValue = columnInfo.field.get(oldT);
                Object newValue = columnInfo.field.get(newT);
                if (newValue == null && oldValue == null) {
                    continue;
                } else if (newValue == null || oldValue == null
                        || !newValue.equals(oldValue)) {
                    byte[] value = convertPOJOFieldToBytes(newT, columnInfo);
                    put.add(columnInfo.familyBytes, columnInfo.qualifierBytes,
                            value);
                    needUpdate = true;
                } else {
                    continue;
                }
            }
            if (!needUpdate) {
                return true;
            }
        } catch (Exception e) {
            throw new SimpleHBaseException("updateObject. rowKey=" + rowKey
                    + " oldT=" + oldT + " newT=" + newT, e);
        }

        ColumnInfo versionedColumnInfo = typeInfo.getVersionedColumnInfo();
        byte[] oldValueOfVersion = convertPOJOFieldToBytes(oldT,
                versionedColumnInfo);

        HTableInterface htableInterface = htableInterface();

        boolean result = false;
        try {
            result = htableInterface.checkAndPut(rowKey.toBytes(),
                    versionedColumnInfo.familyBytes,
                    versionedColumnInfo.qualifierBytes, oldValueOfVersion, put);
        } catch (IOException e) {
            throw new SimpleHBaseException("updateObject. rowKey=" + rowKey
                    + " oldT=" + oldT + " newT=" + newT, e);
        } finally {
            Util.close(htableInterface);
        }

        return result;

    }

    @Override
    public <T> boolean updateObjectWithVersion(RowKey rowKey, T t,
            Object oldVersion) {
        Util.checkRowKey(rowKey);
        Util.checkNull(t);
        //not check oldVersion, oldVersion can be null.

        TypeInfo typeInfo = TypeInfoHolder.findTypeInfo(t.getClass());
        checkVersioned(typeInfo);

        Put put = new Put(rowKey.toBytes());
        for (ColumnInfo columnInfo : typeInfo.getColumnInfos()) {
            byte[] value = convertPOJOFieldToBytes(t, columnInfo);
            put.add(columnInfo.familyBytes, columnInfo.qualifierBytes, value);
        }

        ColumnInfo versionedColumnInfo = typeInfo.getVersionedColumnInfo();
        byte[] oldValueOfVersion = convertValueToBytes(oldVersion,
                versionedColumnInfo);

        HTableInterface htableInterface = htableInterface();
        boolean result = false;
        try {
            result = htableInterface.checkAndPut(rowKey.toBytes(),
                    versionedColumnInfo.familyBytes,
                    versionedColumnInfo.qualifierBytes, oldValueOfVersion, put);
        } catch (IOException e) {
            throw new SimpleHBaseException("updateObjectWithVersion. rowKey="
                    + rowKey + " t=" + t + " oldVersion=" + oldVersion, e);
        } finally {
            Util.close(htableInterface);
        }

        return result;
    }

    private <T> List<T> findObjectList(RowKey startRowKey, RowKey endRowKey,
            Class<? extends T> type, long startIndex, long length, Filter filter) {
        Util.checkRowKey(startRowKey);
        Util.checkRowKey(endRowKey);
        Util.checkNull(type);
        if (startIndex < 0) {
            throw new SimpleHBaseException("startIndex is invalid. startIndex="
                    + startIndex);
        }
        if (length < 1) {
            throw new SimpleHBaseException("length is invalid. length="
                    + length);
        }

        Scan scan = new Scan();
        scan.setStartRow(startRowKey.toBytes());
        scan.setStopRow(endRowKey.toBytes());
        scan.setCaching(getScanCaching());
        scan.setFilter(filter);
        applyRequestFamily(type, scan);

        HTableInterface htableInterface = htableInterface();
        ResultScanner resultScanner = null;

        List<T> resultList = new LinkedList<T>();
        try {
            resultScanner = htableInterface.getScanner(scan);
            long ignoreCounter = startIndex;
            long resultCounter = 0L;
            Result result = null;
            while ((result = resultScanner.next()) != null) {
                if (ignoreCounter-- > 0) {
                    continue;
                }

                resultList.add(convert(result, type));

                if (++resultCounter >= length) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new SimpleHBaseException(
                    "findObjectList. startRowKey=" + startRowKey
                            + " endRowKey=" + endRowKey + " type=" + type, e);
        } finally {
            Util.close(resultScanner);
            Util.close(htableInterface);
        }

        return resultList;
    }

    @Override
    public long count(RowKey startRowKey, RowKey endRowKey) {
        return count(startRowKey, endRowKey, null);
    }

    @Override
    public long count(RowKey startRowKey, RowKey endRowKey, String id,
            Map<String, Object> para) {
        HBaseQuery hbaseQuery = getHbaseTableConfig().getQueryMap().get(id);
        Util.checkNull(hbaseQuery);

        StringBuilder sb = new StringBuilder();
        Map<Object, Object> context = new HashMap<Object, Object>();
        hbaseQuery.getHqlNode().applyParaMap(para, sb, context);

        String hql = sb.toString().trim();

        return countByRawHql(startRowKey, endRowKey, hql, para);

    }

    @Override
    public long countByRawHql(RowKey startRowKey, RowKey endRowKey, String hql,
            Map<String, Object> para) {

        if (StringUtil.isEmptyString(hql)) {
            return count(startRowKey, endRowKey);
        }

        ProgContext progContext = TreeUtil.parse(hql);
        Filter filter = TreeUtil.parseCountFilter(progContext,
                hbaseTableConfig, para);

        return count(startRowKey, endRowKey, filter);
    }

    private long count(RowKey startRowKey, RowKey endRowKey, Filter filter) {
        Util.checkRowKey(startRowKey);
        Util.checkRowKey(endRowKey);

        Scan scan = new Scan();
        scan.setStartRow(startRowKey.toBytes());
        scan.setStopRow(endRowKey.toBytes());
        scan.setCaching(getScanCaching());
        scan.setFilter(filter);
        HBaseColumnSchema hbaseColumnSchema = columnSchema();
        scan.addColumn(hbaseColumnSchema.getFamilyBytes(),
                hbaseColumnSchema.getQualifierBytes());
        scan.setFilter(filter);

        LongColumnInterpreter columnInterpreter = new LongColumnInterpreter();
        AggregationClient aggregationClient = aggregationClient();
        try {
            return aggregationClient.rowCount(tableNameBytes(),
                    columnInterpreter, scan);
        } catch (Throwable t) {
            throw new SimpleHBaseException("error when count.", t);
        }
    }
}
