package allen.studyhbase;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.util.Bytes;

import org.junit.Assert;

import org.junit.Test;

import com.alipay.simplehbase.util.Util;

public class HbaseBasicOpTest extends HbaseTestBase {

    @Test
    public void testDelete() throws Exception {

        Get get = new Get(rowKey1);
        Result result = table.get(get);
        Assert.assertTrue(!result.isEmpty());

        Delete delete = new Delete(rowKey1);
        table.delete(delete);

        get = new Get(rowKey1);
        result = table.get(get);
        Assert.assertTrue(result.isEmpty());

    }

    @Test
    public void testDeleteNotExistRow() throws Exception {
        byte[] rowKey = Bytes.toBytes("allen_test_row");
        Delete delete = new Delete(rowKey);
        table.delete(delete);
    }

    @Test
    public void testScan_start_end() throws Exception {

        Set<String> resultRowKeys = new HashSet<String>();
        Scan scan = new Scan(rowKey1, rowKey2);
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result = resultScanner.next(); result != null; result = resultScanner
                .next()) {
            resultRowKeys.add(Bytes.toString(result.getRow()));
        }

        Util.close(resultScanner);

        Assert.assertTrue(resultRowKeys.size() == 1);
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr1));
    }

    @Test
    public void testScan_same_start_end() throws Exception {

        Set<String> resultRowKeys = new HashSet<String>();
        Scan scan = new Scan(rowKey1, rowKey1);
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result = resultScanner.next(); result != null; result = resultScanner
                .next()) {
            resultRowKeys.add(Bytes.toString(result.getRow()));
        }

        Util.close(resultScanner);

        Assert.assertTrue(resultRowKeys.size() == 1);
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr1));
    }

    @Test
    public void testScan_start() throws Exception {
        Set<String> resultRowKeys = new HashSet<String>();

        Scan scan = new Scan(rowKey1);
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result = resultScanner.next(); result != null; result = resultScanner
                .next()) {
            resultRowKeys.add(Bytes.toString(result.getRow()));
        }

        Util.close(resultScanner);

        Assert.assertTrue(resultRowKeys.size() == 4);
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr1));
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr2));
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr3));
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr4));
    }

    @Test
    public void testScan_withColumn() throws Exception {
        Set<String> resultRowKeys = new HashSet<String>();

        Scan scan = new Scan(rowKey1);
        scan.addColumn(ColumnFamilyName, QName1);
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result = resultScanner.next(); result != null; result = resultScanner
                .next()) {
            resultRowKeys.add(Bytes.toString(result.getRow()));
        }

        Util.close(resultScanner);

        Assert.assertTrue(resultRowKeys.size() == 3);
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr1));
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr2));
        Assert.assertTrue(resultRowKeys.contains(rowKeyStr3));

    }

    @Test
    public void testCheckAndPut() throws Exception {
        byte[] rowKey = Bytes.toBytes("allen_test_row");
        Put put = new Put(rowKey);
        put.add(ColumnFamilyName, QName1, Bytes.toBytes("a"));
        put.add(ColumnFamilyName, QName2, Bytes.toBytes("b"));

        boolean result = false;

        result = table.checkAndPut(rowKey, ColumnFamilyName, QName2,
                Bytes.toBytes("b"), put);
        // check fail, put fail.
        Assert.assertFalse(result);

        result = table.checkAndPut(rowKey, ColumnFamilyName, QName2, null, put);
        // check ok, put ok.
        Assert.assertTrue(result);

        result = table.checkAndPut(rowKey, ColumnFamilyName, QName2, null, put);
        // check fail, put fail.
        Assert.assertFalse(result);

        result = table.checkAndPut(rowKey, ColumnFamilyName, QName2,
                Bytes.toBytes("b"), put);
        // check ok, put ok.
        Assert.assertTrue(result);
    }

    @Test
    public void testPutAndGet() throws Exception {
        byte[] rowKey = Bytes.toBytes("allen_test_row");
        Put put = new Put(rowKey);
        put.add(ColumnFamilyName, QName1, Bytes.toBytes("a"));
        put.add(ColumnFamilyName, QName3, null);
        table.put(put);

        Get get = new Get(rowKey);
        Result result = table.get(get);

        byte[] q1 = result.getValue(ColumnFamilyName, QName1);
        byte[] q2 = result.getValue(ColumnFamilyName, QName2);
        byte[] q3 = result.getValue(ColumnFamilyName, QName3);

        Assert.assertEquals("a", Bytes.toString(q1));
        // we get null byte array here.
        Assert.assertEquals(null, Bytes.toString(q2));
        // we get empty byte array here. not a null.
        Assert.assertEquals("", Bytes.toString(q3));

        // get a row doesn't exist.
        byte[] rowKey2 = Bytes.toBytes("allen_test_row_not_exist");
        get = new Get(rowKey2);
        result = table.get(get);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testPutWithoutColumn() throws Exception {
        byte[] rowKey = Bytes.toBytes("allen_test_row");
        Put put = new Put(rowKey);
        try {
            table.put(put);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ignore.
        }
    }
}
