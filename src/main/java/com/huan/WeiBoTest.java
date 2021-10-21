package com.huan;

import com.huan.constants.Constants;
import com.huan.dao.HBaseDao;
import com.huan.utils.HBaseUtils;

import java.io.IOException;

public class WeiBoTest {

    public static void init(){

        try {
            //1.创建命名空间
            HBaseUtils.createNameSpace( Constants.NAMESPACE );
            //2.创建微博内容表
            HBaseUtils.createTable( Constants.CONTENT_TABLE,Constants.CONTENT_TABLE_VERSION,Constants.CONTENT_TABLE_CF );
            //3.创建用户关系表
            HBaseUtils.createTable( Constants.RELATION_TABLE,Constants.RELATION_TABLE_VERSION,Constants.RELATION_TABLE_CF1,Constants.RELATION_TABLE_CF2 );
            //4.创建收件箱表
            HBaseUtils.createTable( Constants.INBOX_TABLE,Constants.INBOX_TABLE_VERSION,Constants.INBOX_TABLE_CF );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        /**
         * 1001:昊哥
         * 1002:不明人物
         * 1003:大欢欢
         */

        //TODO 1.初始化
        init();

        //TODO 1001发布微博
        HBaseDao.publishWeiBo( "1001","昊哥爱欢欢！！！" );
        //TODO 1002关注1001和1003
        HBaseDao.addAttends( "1002","1001","1003" );
        //TODO 获取1002初始化界面
        HBaseDao.getInt( "1002" );
        System.out.println("-------------1111----------------");
        //TODO 1003发布3条微博，同时1001发布2条微博
        HBaseDao.publishWeiBo( "1003","欢欢也爱昊哥");
        Thread.sleep( 10 );
        HBaseDao.publishWeiBo( "1001","爱你爱你,满脑子都是你");
        Thread.sleep( 10 );
        HBaseDao.publishWeiBo( "1003","真的丫,欢欢也是！！");
        Thread.sleep( 10 );
        HBaseDao.publishWeiBo( "1001","亲亲我的猪");
        Thread.sleep( 10 );
        HBaseDao.publishWeiBo( "1003","欢欢害羞！！");
        //TODO 再次获取1002初始化界面
        HBaseDao.getInt("1002"  );
        System.out.println("-------------2222-------------");
        //TODO 1002取关1003
        HBaseDao.deleteAttends( "1002","1003" );
        //TODO 再次获取1002初始化界面
        HBaseDao.getInt("1002"  );
        System.out.println("-------------3333-------------");
        //TODO 1002再次关注1003
        HBaseDao.addAttends( "1002","1003" );
        //TODO 获取1002初始化页面
        HBaseDao.getInt("1002"  );
        System.out.println("-------------4444--------------");

        //TODO 获取1001微博详情
        HBaseDao.getWeiBo( "1001" );
    }

}