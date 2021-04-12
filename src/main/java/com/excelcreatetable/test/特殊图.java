package com.excelcreatetable.test;


import org.apache.commons.lang3.StringUtils;

/**
 * @author szc
 * @date 2021/4/7 - 13:54
 */
public class 特殊图 {
    public static void main(String[] args) {
        System.out.println(-21>>>1);


        String  str = "      gsff sfasfasf  asdfasdsd ff dsfsf   c  ";
        String s = StringUtils.deleteWhitespace(str);
        System.out.println(s);
        String trim = str.trim();
        System.out.println(trim);
        String replace = str.replace(" ", "");
        System.out.println(replace);
        String replaceAll = str.replaceAll(" ", "");
        System.out.println(replaceAll);

    }


}
