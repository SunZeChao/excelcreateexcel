package com.excelcreatetable.demo.saveData;/**
 * @author wzj
 * @date 2020/3/17 14:27
 * @version 1.0
 */

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.excelcreatetable.demo.dbconnect.DBConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * auther wzj
 * date 2020/3/17 14:27
 * 将读取到的excel保存数据库
 */
@Service
public class SaveData {

    //数据库表名
    public static String dataTableName;
    //数据库列名
    public static String[] dataCloumn;


    /**
     *
     * @param tableName 文件路径
     * @param cloumn 列名
     */
    public void saveData(String filePath,String tableName,String[] cloumn){
        dataTableName=tableName;
        dataCloumn=cloumn;
        EasyExcel.read(filePath,new SaveDataListener()).sheet().doRead();

    }


}

//监听器
class SaveDataListener extends AnalysisEventListener<Map<Integer, String>> {

    private final static Logger logger = LoggerFactory.getLogger(SaveDataListener.class);

    List<Map<Integer,String>> list = new ArrayList<>();

    //每次多少数据保存一次
    private Integer size = 20000;

    @Override
    public void invoke(Map<Integer, String> integerStringMap, AnalysisContext analysisContext) {
        logger.info("读取到的数据==="+ JSON.toJSONString(integerStringMap));

        list.add(integerStringMap);

        if (list.size()>=size){
            saveDate(list);
            list.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

        saveDate(list);
        logger.info("所有数据解析完成");

    }

    public void saveDate(List<Map<Integer,String>> list){
        //TODO 保存数据

        Connection connection = DBConnect.getConnection();
        String[] clounm = SaveData.dataCloumn;
        //
        String cloumnName="";
        String value = "";
        for (int i = 0; i < clounm.length; i++) {
           cloumnName += clounm[i];
           value+="?";
           if (i<clounm.length-1){
               cloumnName+=",";
               value+=",";
           }
        }

        String sql = "insert into "+SaveData.dataTableName +"("+cloumnName+")"+" values("+value+")";
        System.out.println("保存数据的sql = " + sql);
        try {
            PreparedStatement call = connection.prepareStatement(sql);
            for (int i = 0; i < list.size(); i++) {
                Map<Integer, String> integerStringMap = list.get(i);
                //此处clounm选择表头的长度 不然 表头最后一列是空将报错
                for (int i2=0;i2<clounm.length;i2++){
                    String s = integerStringMap.get(i2);
                    call.setString(i2+1,s);
                }
                call.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
