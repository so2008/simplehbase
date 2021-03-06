package com.alipay.simplehbase.client;

import org.apache.hadoop.hbase.HTableDescriptor;

import com.alipay.simplehbase.config.HBaseDataSource;

/**
 * SimpleHbaseAdminClient.
 * 
 * @author xinzhi
 * */
public interface SimpleHbaseAdminClient {

    /**
     * Creates a new table. Synchronous operation.
     */
    public void createTable(HTableDescriptor tableDescriptor);

    /**
     * Deletes a table. Synchronous operation.
     */
    public void deleteTable(final String tableName);

    public HBaseDataSource getHBaseDataSource();

    public void setHBaseDataSource(HBaseDataSource hbaseDataSource);
}
