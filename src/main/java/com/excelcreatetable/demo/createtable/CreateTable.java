package com.excelcreatetable.demo.createtable;/**
 * @author wzj
 * @date 2020/3/17 14:11
 * @version 1.0
 */

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.excelcreatetable.demo.dbconnect.DBConnect;
import com.excelcreatetable.demo.easyexcelreadhead.ReadHead;
import com.excelcreatetable.demo.saveData.SaveData;
import com.excelcreatetable.demo.threadLocalShare.ThreadLocalShare;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.util.Enumeration;


/**
 * auther wzj
 * date 2020/3/17 14:11
 */
@Controller
@RequestMapping("/test")
public class CreateTable {




    @Autowired
    private ReadHead readHead;



    @Autowired
    private SaveData saveData;

    private final static String driver="com.mysql.cj.jdbc.Driver";
    private final static String url="jdbc:mysql://192.168.33.111:3306/xinhe?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final static String userName="root";
    private final static String passWord="123456";
    private static Connection connection;
    private CallableStatement call;

    /*static {
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url,userName,passWord);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (SQLException sql){
            sql.printStackTrace();
        }

    }*/

    @GetMapping("/createTable")
    @ResponseBody
    public void createTable(String filePath, String tableName , HttpServletRequest httpServletRequest){
        ThreadLocalShare.filePath.set(filePath);
        Enumeration<String> attributeNames = httpServletRequest.getAttributeNames();
       while(attributeNames.hasMoreElements()){
           System.out.println(attributeNames.nextElement());
       }
        System.out.println(attributeNames);
        String test = (String) httpServletRequest.getAttribute("filePath");
        System.out.println(test);
        String serverName = httpServletRequest.getServerName();
        System.out.println(serverName);
        readHead.readHead(filePath,tableName);
    }

    /**
     *创建表失败
     * @param head 表的列名
     * @param tableName 表名
     */
    public void createTable(String[] head,String tableName){


        String[] clount = new String[head.length];
        if (head.length==0 || head==null){
            throw new RuntimeException("列为空");
        }


        StringBuilder sql =new StringBuilder("create table "+tableName+"(id int auto_increment primary key not null") ;
        //拼接语句
        if (head!=null && head.length>0){
            sql.append(",");
            //sql +=",";
            int length = head.length;
            for (int i = 0; i < length; i++) {
                //建表字段
                String columName = head[i].trim();
                //将汉字转化为拼音
                String hanziColum = ToPinyin(columName);
                //将字段保存下来,方便保存数据
                clount[i]=hanziColum;
                sql.append(hanziColum+" varchar(200) DEFAULT NULL COMMENT "+"\""+columName+"\"");
                //sql+=hanziColum+" varchar(255) DEFAULT NULL COMMENT "+"\""+columName+"\"";
                //防止最后一个
                if (i<length-1){
                    sql.append(",");
                    //sql+=",";
                }
            }
        }
        //拼凑完 建表语句 设置默认字符集
        sql.append(")DEFAULT CHARSET=utf8 COMMENT = '"+ThreadLocalShare.filePath.get()+"'");
        //sql+=")DEFAULT CHARSET=utf8;";
        System.out.println("创建表的sql语句 = " + sql);

        try {
            //connection = dataSource.getConnection();
            CreateTable.connection = DBConnect.getConnection();
            call = CreateTable.connection.prepareCall(sql.toString());
            boolean execute = call.execute(sql.toString());
            System.out.println("创建表成功");
            //保存数据
            saveData2(tableName,clount);
        } catch (SQLException e) {
            System.out.println("创建表失败");
            e.printStackTrace();
            throw new RuntimeException("创建失败");
        }
    }

    /**
     * 将汉字转化为汉字
     * @param chinese
     * @return
     */
    public static String ToPinyin(String chinese){
        String pinyinStr = "";
        char[] newChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        for (int i = 0; i < newChar.length; i++) {
            if (newChar[i] > 128) {
                try {
                    pinyinStr += PinyinHelper.toHanyuPinyinStringArray(newChar[i], defaultFormat)[0];
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            }else{
                pinyinStr += newChar[i];
            }
        }
        return pinyinStr;
    }

    /**
     * 保存数据
     * @param tableName 表名
     * @param column 列名
     */
    public void saveData2(String tableName,String[] column){
        String filePath = ThreadLocalShare.filePath.get();
        SaveData saveData = new SaveData();
        saveData.saveData(filePath,tableName,column);
    }
}
