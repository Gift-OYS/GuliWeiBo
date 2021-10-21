package com.huan.dao;

import com.huan.constants.Constants;
import com.huan.utils.HBaseUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 1.发布微博
 * 2.删除微博
 * 3.关注用户
 * 4.取关用户
 * 5.获取用户初始化界面
 * 6.获取微博详情
 */
public class HBaseDao {

    //TODO 1.发布微博
    public static void publishWeiBo(String uid, String content) throws IOException {
        //1.获取Connection对象
        Connection connection = HBaseUtils.connection;
        connection = ConnectionFactory.createConnection( Constants.CONFIGURATION );

        //第一部分:操作微博内容表
        //1.获取微博内容表对象
        Table contTable = connection.getTable( TableName.valueOf( Constants.CONTENT_TABLE ) );

        //2.获取当前时间戳
        long ts = System.currentTimeMillis();

        //3.获取RowKey
        String rowKey = uid + "_" + ts;

        //4.创建Put对象
        Put contPut = new Put( Bytes.toBytes( rowKey ) );
        //5.给Put对象赋值
        contPut.addColumn( Bytes.toBytes( Constants.CONTENT_TABLE_CF ), Bytes.toBytes( "content" ), Bytes.toBytes( content ) );
        //6.执行插入数据操作
        contTable.put( contPut );

        //第二部分操作微博收件箱表
        //1.获取用户关系表对象
        Table relaTable = connection.getTable( TableName.valueOf( Constants.RELATION_TABLE ) );

        //2.获取当前发布微博人的fans列族数据
        Get get = new Get( Bytes.toBytes( uid ) );
        Result result = relaTable.get( get );

        //3.创建一个集合，用户存放微博内容表的Put对象
        ArrayList<Put> inboxPuts = new ArrayList<>();
        //4.遍历粉丝
        for (Cell cell : result.rawCells()) {
            //5.构建微博收件箱表的Put对象
            Put inboxPut = new Put( CellUtil.cloneQualifier( cell ) );

            //6.给收件箱表的Put对象赋值
            inboxPut.addColumn( Bytes.toBytes( Constants.INBOX_TABLE_CF ), Bytes.toBytes( uid ), Bytes.toBytes( rowKey ) );

            //7.将收件箱表的Put对象存入集合
            inboxPuts.add( inboxPut );
        }

        //8.判断是否有粉丝
        if (inboxPuts.size() > 0) {
            //获取收件箱表对象
            Table inboxTable = connection.getTable( TableName.valueOf( Constants.INBOX_TABLE ) );

            //执行收件箱表数据插入操作
            inboxTable.put( inboxPuts );

            //关闭收件箱表
            inboxTable.close();

        }

        //关闭资源(关系表和内容表)
        relaTable.close();
        contTable.close();
        connection.close();
    }

    //TODO 2.关注用户
    public static void addAttends(String uid, String... attends) throws IOException {
        //1.校验是否添加了待关注的人
        if (attends.length <= 0) {
            System.out.println( "请选择待关注的人。。。" );
            return;
        }

        //获取Connection对象
        Connection connection = ConnectionFactory.createConnection( Constants.CONFIGURATION );

        //第一部分:操作用户关系表
        //1.获取用户关系表对象
        Table relaTable = connection.getTable( TableName.valueOf( Constants.RELATION_TABLE ) );
        //2.创建一个集合，用于存放用户关系表的Put对象
        ArrayList<Put> relaPuts = new ArrayList<>();
        //3.创建操作者的Put对象
        Put uidPut = new Put( Bytes.toBytes( uid ) );
        //4.循环创建被故被关注者的Put对象
        for (String attend : attends) {
            //5.给操作者的Put对象赋值
            uidPut.addColumn( Bytes.toBytes(Constants.RELATION_TABLE_CF1),Bytes.toBytes(attend),Bytes.toBytes(attend));
            //6.创建被关注着的Put对象
            Put attendPut = new Put( Bytes.toBytes( attend ) );
            //7.给被关注着的Put对象复制
            attendPut.addColumn( Bytes.toBytes( Constants.RELATION_TABLE_CF2 ), Bytes.toBytes( uid ), Bytes.toBytes( uid ) );
            //8.将被关注着的Put对象放入集合
            relaPuts.add( attendPut );
        }
        //9.将操作者的Put对象添加至集合
        relaPuts.add( uidPut );

        //10.执行用户关系表插入数据操作
        relaTable.put( relaPuts );

        //第二部分:操作收件箱表
        //1.获取微博内容表
        Table contTable = connection.getTable( TableName.valueOf( Constants.CONTENT_TABLE ) );
        //2.创建收件箱表的Put对象
        Put inboxPut = new Put( Bytes.toBytes( uid ) );
        //3.循环attends,获取每个被关注者的近期发布的微博
        for (String attend:attends) {

            //4.获取当前被关注者近期发布的微博 (Scan) -> 集合ResultScanner
            Scan scan = new Scan(Bytes.toBytes(attend+"_"  ),Bytes.toBytes( attend+ "|" ));
            ResultScanner resultScanner = contTable.getScanner( scan );

            //TODO 定义一个时间戳
            long ts = System.currentTimeMillis();

            //5.获取的值进行遍历
            for (Result result:resultScanner) {

                //6.给收件箱表的Put对象赋值
                inboxPut.addColumn( Bytes.toBytes( Constants.INBOX_TABLE_CF )
                        ,Bytes.toBytes(attend  ),ts++,result.getRow());
            }
        }
        //7.判断当前的Put对象是否为空
        if (!inboxPut.isEmpty()){

            //8.获取收件箱表数据
            Table inboxTable = connection.getTable( TableName.valueOf( Constants.INBOX_TABLE ) );
            //9.插入数据
            inboxTable.put( inboxPut );
            //10.关闭收件箱连接
            inboxTable.close();
        }

        //关闭资源
        relaTable.close();
        contTable.close();
        connection.close();
    }

    //TODO 3.取关用户
    public static void deleteAttends(String uid,String... dels) throws IOException {

        if (dels.length <= 0){
            System.out.println("请添加取关的用户");
            return;
        }

        //获取Connection对象
        Connection connection = ConnectionFactory.createConnection( Constants.CONFIGURATION );
        //第一部分：操作用户关系表
        //1.获取用户关系表
        Table relaTable = connection.getTable( TableName.valueOf( Constants.RELATION_TABLE ) );
        //2.创建一个集合，用于存放用户关系表的Delete对象
        ArrayList<Delete> relaDeletes = new ArrayList<>();
        //3.创建操作者的Delete对象
        Delete uidDelete = new Delete( Bytes.toBytes( uid ) );
        //4.循环创建被取关者的Delete对象
        for (String del:dels) {

            //5.给操作者Delete对象赋值
            uidDelete.addColumn( Bytes.toBytes( Constants.RELATION_TABLE_CF1 ),
                    Bytes.toBytes( del ));
            //6.创建被取关者的Delete对象
            Delete delDelete = new Delete( Bytes.toBytes( del ) );
            //7.给被取关者的Delete对象赋值
            delDelete.addColumn( Bytes.toBytes( Constants.RELATION_TABLE_CF2 ),Bytes.toBytes( uid ) );
            //8.将被取关者的Delete对象添加至集合
            relaDeletes.add( delDelete );
        }

        //9.将操作者的Delete对象添加至集合
        relaDeletes.add( uidDelete );
        //10.执行用户关系表的删除操作
        relaTable.delete( relaDeletes );
        //第二部分：操作收件箱表
        //1.获取收件箱表对象
        Table inboxTable = connection.getTable( TableName.valueOf( Constants.INBOX_TABLE ) );
        //2.创建操作者的Delete对象
        Delete inboxDelete = new Delete( Bytes.toBytes( uid ) );
        //3.给操作者的Delete对象赋值
        for (String del : dels) {
            inboxDelete.addColumn( Bytes.toBytes( Constants.INBOX_TABLE_CF ),Bytes.toBytes( del ) );
        }

        //4.执行收件箱表的删除操作
        inboxTable.delete( inboxDelete );

        //关闭资源
        relaTable.close();
        inboxTable.close();
        connection.close();

    }

    //TODO 4.获取初始化页面数据
    public static void getInt(String uid) throws IOException {
        //获取Connection对象
        Connection connection = ConnectionFactory.createConnection( Constants.CONFIGURATION );
        //1.获取收件箱表对象
        Table inboxTable = connection.getTable( TableName.valueOf( Constants.INBOX_TABLE ) );
        //2.获取微博内容表对象
        Table contTable = connection.getTable( TableName.valueOf( Constants.CONTENT_TABLE ) );

        //3.创建收件箱表Get对象，并获取数据(设置最大版本)
        Get inboxGet = new Get( Bytes.toBytes( uid ) );
        inboxGet.setMaxVersions();
        Result result = inboxTable.get( inboxGet );
        //4.遍历获取的数据
        for (Cell cell : result.rawCells()){

            //5.构建微博内容表Get对象
            Get contGet = new Get( CellUtil.cloneValue( cell ) );
            //6.获取该Get对象的数据内容
            Result contResult = contTable.get( contGet );

            //7.解析内容并打印
            for (Cell contCell : contResult.rawCells()) {

                System.out.println("PK:"+Bytes.toString(  CellUtil.cloneRow( contCell ))
                        +",CF:"+Bytes.toString( CellUtil.cloneFamily( contCell ) )
                        +",CN:"+Bytes.toString( CellUtil.cloneQualifier( contCell ) )
                        +",Value:"+Bytes.toString( CellUtil.cloneValue( contCell ) ));
            }
        }
        //关闭资源
        inboxTable.close();
        contTable.close();
        connection.close();

    }

    //TODO 5.获取用户初始化界面 (获取某个人所有微博详情)
    public static void getWeiBo(String uid) throws IOException {
        //1.获取Connection对象
        Connection connection = ConnectionFactory.createConnection( Constants.CONFIGURATION );
        //2.获取微博内容表对象
        Table table = connection.getTable( TableName.valueOf( Constants.CONTENT_TABLE ) );
        //3.构建Scan对象
        Scan scan = new Scan();

        //构建过滤器
        RowFilter rowFilter = new RowFilter( CompareFilter.CompareOp. EQUAL,new SubstringComparator( uid+"_"));
        scan.setFilter( rowFilter );

        //4.获取数据
        ResultScanner resultScanner = table.getScanner( scan );
        //5.解析数据并打印
        for (Result result : resultScanner) {
            for (Cell cell : result.rawCells()) {

                System.out.println("PK:"+Bytes.toString(  CellUtil.cloneRow( cell ))
                        +",CF:"+Bytes.toString( CellUtil.cloneFamily( cell ) )
                        +",CN:"+Bytes.toString( CellUtil.cloneQualifier( cell ) )
                        +",Value:"+Bytes.toString( CellUtil.cloneValue( cell ) ));
            }
        }


        //6.关闭资源
        table.close();
        connection.close();

    }

}