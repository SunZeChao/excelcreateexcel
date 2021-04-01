package com.excelcreatetable.test;

import com.excelcreatetable.demo.createtable.CreateTable;
import com.excelcreatetable.demo.easyexcelreadhead.ReadHead;
import com.excelcreatetable.demo.threadLocalShare.ThreadLocalShare;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.any23.encoding.TikaEncodingDetector;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


/**
 * @author
 * @date 2020/7/13 - 15:34
 */
public class bianliwenjianjia {
    public static void main(String[] args) throws IOException, SQLException {
        traverseFolder("D:\\工作");
    }

    private final static String driver = "com.mysql.cj.jdbc.Driver";
    private final static String url = "jdbc:mysql://localhost:3306/s?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final static String userName = "root";
    private final static String passWord = "root";
    private static Connection connection;

    /**
     * 获取数据库连接
     *
     * @return
     */
    public static Connection getConnection() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(url, userName, passWord);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    //遍历文件夹
    public static void traverseFolder(String path) throws IOException, SQLException {

        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null == files || files.length == 0) {
                System.out.println("文件夹是空的!");
                return;
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        traverseFolder(file2.getAbsolutePath());
                    } else {
                        String pa = file2.getAbsolutePath();
                        String file2Name = file2.getName();
                        String tableName = file2Name.substring(0, file2Name.lastIndexOf("."));
                        if ("csv".equals(pa.substring(pa.lastIndexOf(".") + 1))) {
                            System.out.println("文件:" + pa);
                            System.out.println("数据库表名" + tableName);
                            //创建表
                            String col = createTabale(pa, tableName);
                            // 数据入库
                            readData(tableName, col,pa);
                        }
                        if("xlsx".equals(pa.substring(pa.lastIndexOf(".") + 1))) {
                            //todo 不完善
                            //xlsCreateTable(pa,tableName,null);
                        }

                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    //创建数据库表
    private static String createTabale(String path, String tableName) throws IOException, SQLException {
        //返回的字段
        StringBuilder col = new StringBuilder();

        File file = new File(path);
        //获得字符集
        String code = getset(file);
        System.err.println(code);
        InputStreamReader isr = null;
        if ("GBK".equals(code)) {
            isr = new InputStreamReader(new FileInputStream(file), "gbk");
        } else if ("UTF-8".equals(code)) {
            isr = new InputStreamReader(new FileInputStream(file), "utf-8");
        } else {
            System.err.println(code);
            System.err.println("未读取的文件" + path);
            return null;
        }

        BufferedReader br = new BufferedReader(isr);
        String s = br.readLine();
        String[] fields = s.split(",");
        StringBuilder sb = new StringBuilder("create table " + tableName + " (");
        for (String field : fields) {
            System.out.println(field);
            String szc = ToPinyin(field);
            sb.append(szc.replaceAll("\"", "") + " text DEFAULT NULL COMMENT'" + field + "',");// 根据实际情况修改字段长度
            // System.err.println(szc);
            col.append(szc).append(" ,");
        }
        sb = new StringBuilder(sb.substring(0, sb.length() - 1));
        sb.append(") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT");
        System.err.println(sb);
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(sb.toString());
        ps.execute();
        System.err.println("创建表：" + tableName + "成功");
        ps.close();
        return col.substring(0,col.length()-1);
    }


    /**
     * 读取文件数据
     * @param tableName
     * @param
     */
    private static void readData(String tableName, String col,String path) throws IOException, SQLException {
        Charset charset = guessCharset(new FileInputStream(path));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), charset.toString()));
        String line="";
        String firstline=bufferedReader.readLine();// 去掉第一行表头数据

        List<String[]> list=new ArrayList<>();//需要入库的数据
        while ((line = bufferedReader.readLine()) != null) {
            String[] arr=line.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            list.add(arr);
            if(list.size()%500000==0){  //数据每到50万执行
                savedate(list,col,tableName);
                list.clear();
            }
        }
        savedate(list,col,tableName);//最后将不满50w的执行入库

    }

    /**
     * 数据入库
     * @param listData
     * @param cols
     * @param tableName
     */
    private static void savedate(List<String[]> listData, String cols, String tableName) throws SQLException {
        StringBuilder insertSQL=new StringBuilder();
        insertSQL.append("insert into ").append(tableName).append("(").append(cols.replaceAll("\"","")).append(") values(");
        for(int h=0;h<cols.split(",").length;h++){
            insertSQL.append("?,");
        }
        insertSQL.deleteCharAt(insertSQL.toString().length()-1);
        insertSQL.append(")");
        System.out.println(insertSQL.toString());
        PreparedStatement statement=connection.prepareStatement(insertSQL.toString());
        //这里必须设置为false，我们手动批量提交
        connection.setAutoCommit(false);

        System.out.println("开始插入数据");
        Long startTime = System.currentTimeMillis();
        for (int i = 0; i <listData.size() ; i++) {
            System.out.println(Arrays.toString(listData.get(i)) +"---------------");
            for(int g=0;g<listData.get(i).length;g++){
                statement.setString(g+1,listData.get(i)[g]);
            }
            if(cols.split(",").length != listData.get(i).length ){ //判断数据的列数 是否等于表头的列数
                int dataLength = listData.get(i).length;
                for(int g= dataLength ;g<cols.split(",").length ;g++){
                    statement.setString(g+1,null);
                }
            }
            //将要执行的SQL语句先添加进去，不执行
            statement.addBatch();
        }
        //50W条SQL语句已经添加完成，执行这50W条命令并提交
        statement.executeBatch();
        connection.commit();
        Long endTime = System.currentTimeMillis();
        statement.close();
//        connection.close();
        System.out.println("插入完毕,用时：" + (endTime - startTime));
    }


    /**
     * 将汉字转化为汉字
     * @param chinese
     * @return
     */
    public static String ToPinyin(String chinese) {
//        chinese = chinese.replaceAll("，", ",").replaceAll("（", "(").replaceAll("）", ")")
//                .replaceAll("。", ".").replaceAll("！", "!").replaceAll("“", "\"")
//                .replaceAll("”", "\"").replaceAll("‘", "\'").replaceAll("’", "\'")
//                .replaceAll("；", ";").replaceAll("：", ":").replaceAll("？", "?")
//                .replaceAll("￥","\\$").replaceAll("……","").replaceAll(" ","");

        chinese = chinese.replaceAll("，", "").replaceAll("（", "").replaceAll("）", "")
                .replaceAll("。", "").replaceAll("！", "").replaceAll("“", "")
                .replaceAll("”", "").replaceAll("‘", "").replaceAll("’", "")
                .replaceAll("；", "").replaceAll("：", "").replaceAll("？", "")
                .replaceAll("￥", "").replaceAll("……", "").replaceAll(" ", "")
                .replaceAll("!", "").replaceAll("&", "").replaceAll("#", "")
                .replaceAll("\\*", "").replaceAll("@", "");

        String pinyinStr = "";
        char[] newChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        for (int i = 0; i < newChar.length; i++) {
            if (newChar[i] > 128) {
                //System.err.println((char) newChar[i]);
                try {
                    pinyinStr += PinyinHelper.toHanyuPinyinStringArray(newChar[i], defaultFormat)[0];
                    System.out.println(pinyinStr);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinStr += newChar[i];
                //  System.err.println(pinyinStr);
            }
        }
        return pinyinStr;
    }


    //获得字符集
    static String getset(File file) {
        String set = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) return set;
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                set = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {
                set = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB && first3Bytes[2] == (byte) 0xBF) {
                set = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int len = 0;
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF) //单独出现BF以下的，也算是GBK
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF)//双字节 (0xC0 - 0xDF) (0x80 - 0xBF),也可能在GB编码内
                            continue;
                        else
                            break;

                    } else if (0xE0 <= read && read <= 0xEF) {//也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                set = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
                System.out.println(loc + " " + Integer.toHexString(read));
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
     * 判断文件字符集
     * @param is
     * @return
     * @throws IOException
     */
    public static Charset guessCharset(InputStream is) throws IOException {
        return Charset.forName(new TikaEncodingDetector().guessEncoding(is));
    }

    //todo 删除表


    /**
     * xlsx  创表入库
     * @param filePath
     * @param tableName
     * @param httpServletRequest
     */
    public static  void xlsCreateTable(String filePath, String tableName , HttpServletRequest httpServletRequest){
        ThreadLocalShare.filePath.set(filePath);

        ReadHead readHead = new ReadHead();
        readHead.readHead(filePath,tableName);
    }

}
