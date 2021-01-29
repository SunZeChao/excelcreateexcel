package com.excelcreatetable.test;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author
 * @date 2020/7/3 - 14:35
 */
public class test {
    private final static String driver="com.mysql.cj.jdbc.Driver";
    private final static String url="jdbc:mysql://localhost:3306/s?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
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

    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\超\\Desktop\\11.txt";
        //获取文件的后缀名
        String suffix = path.substring(path.lastIndexOf(".")+1);
        System.err.println(suffix);

        File file = new File(path);
        //获得字符集
        String code = getset(file);
        System.err.println(code);
        InputStreamReader isr=null;
        if("GBK".equals(code)){
            isr = new InputStreamReader(new FileInputStream(file), "gbk");
        }else if ("UTF-8".equals(code)){
             isr = new InputStreamReader(new FileInputStream(file), "utf-8");
        }else {
            System.err.println(code);
            return;
        }


        BufferedReader br = new BufferedReader(isr);
        String s = br.readLine();
        String[] fields = s.split(",");
        StringBuilder sb = new StringBuilder("create table szc (");
        for (String field : fields) {
            System.out.println(field);
            String szc = ToPinyin(field);
            sb.append(szc+" varchar(255) DEFAULT NULL COMMENT'"+field+"',");
           // System.err.println(szc);
        }
        sb = new StringBuilder(sb.substring(0, sb.length() - 1));
        sb.append(") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT");
        System.err.println(sb);
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(sb.toString());
        ps.execute();

    }

    /**
     * 将汉字转化为汉字
     *
     * @param chinese
     * @return
     */
    public static String ToPinyin(String chinese) {
        chinese = chinese.replaceAll("，", ",").replaceAll("（", ")").replaceAll("）", ")")
                .replaceAll("。", ".").replaceAll("！", "!").replaceAll("“", "\"")
                .replaceAll("”", "\"").replaceAll("‘", "\'").replaceAll("’", "\'")
                .replaceAll("；", ";").replaceAll("：", ":").replaceAll("？", "?");

        String pinyinStr = "";
        char[] newChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        for (int i = 0; i < newChar.length; i++) {
            if (newChar[i] > 128) {
                System.err.println((char) newChar[i]);
                try {
                    pinyinStr += PinyinHelper.toHanyuPinyinStringArray(newChar[i], defaultFormat)[0];
                    System.out.println(pinyinStr);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinStr += newChar[i];
                System.err.println(pinyinStr);
            }
        }
        return pinyinStr;
    }


    //获得字符集
    static String getset(File file) {

        String set = "GBK";

        byte [] first3Bytes = new byte[3];

        try {

            boolean checked = false;

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            bis.mark(0);

            int read = bis.read(first3Bytes, 0, 3);

            if (read == -1) return set;

            if (first3Bytes[0] == (byte)0xFF && first3Bytes[1] == (byte)0xFE) {

                set = "UTF-16LE";

                checked = true;

            }

            else if(first3Bytes[0] == (byte)0xFE && first3Bytes[1] == (byte)0xFF) {

                set = "UTF-16BE";

                checked = true;

            }
            else if(first3Bytes[0] == (byte)0xEF && first3Bytes[1] == (byte)0xBB && first3Bytes[2] == (byte)0xBF) {

                set = "UTF-8";

                checked = true;

            }

            bis.reset();

            if (!checked) {

                int len = 0;

                int loc = 0;

                while ((read = bis.read()) != -1) {

                    loc ++;

                    if (read >= 0xF0)

                        break;

                    if (0x80<=read && read <= 0xBF) //单独出现BF以下的，也算是GBK

                        break;

                    if (0xC0<=read && read <= 0xDF) {

                        read = bis.read();

                        if (0x80<= read && read <= 0xBF)//双字节 (0xC0 - 0xDF) (0x80 - 0xBF),也可能在GB编码内

                            continue;

                        else

                            break;

                    } else if (0xE0 <= read && read <= 0xEF) {//也有可能出错，但是几率较小
                        read = bis.read();

                        if (0x80<= read && read <= 0xBF) {
                            read = bis.read();

                            if (0x80<= read && read <= 0xBF) {
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

}
