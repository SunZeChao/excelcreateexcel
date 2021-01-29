package com.excelcreatetable.demo.dbconnect;/**
 * @author wzj
 * @date 2020/3/17 18:01
 * @version 1.0
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * auther wzj
 * date 2020/3/17 18:01
 */
public class DBConnect {

    private final static String driver="com.mysql.cj.jdbc.Driver";
//    private final static String url="jdbc:mysql://192.168.33.124:3306/check0721?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
//    private final static String userName="root";
//    private final static String passWord="devuser2099";
    private final static String url="jdbc:mysql://localhost/check_temp?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final static String userName="root";
    private final static String passWord="root";
    private static Connection connection;

    /**
     * 获取数据库连接
     * @return
     */
    public static Connection getConnection(){
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(url,userName,passWord);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
