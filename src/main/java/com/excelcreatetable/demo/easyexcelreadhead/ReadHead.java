package com.excelcreatetable.demo.easyexcelreadhead;/**
 * @author wzj
 * @date 2020/3/17 14:12
 * @version 1.0
 */

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.fastjson.JSON;
import com.excelcreatetable.demo.createtable.CreateTable;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * auther wzj
 * date 2020/3/17 14:12
 *
 * 读取excel的头
 */
@Service
public class ReadHead {

    /**
     *
     * @param fileName 文件路径
     * @param tableName 表名
     */
    public void readHead(String fileName,String tableName){
        if (StringUtils.isEmpty(fileName)){
            throw new RuntimeException("请输入文件地址");
        }
        if (StringUtils.isEmpty(tableName)){
            throw new RuntimeException("请输入表名");
        }
        ClassLoadTableName.local.set(tableName);
        EasyExcel.read(fileName,new ReadHeadListener()).sheet().doRead();
        ClassLoadTableName.local.remove();
    }

}

class ClassLoadTableName{
    public static ThreadLocal<String> local = new ThreadLocal<>();

}


//监听器
class ReadHeadListener extends AnalysisEventListener<Map<String,String>> {

    private final static Logger logger = LoggerFactory.getLogger(ReadHeadListener.class);

    @Override
    public void invoke(Map<String, String> stringStringMap, AnalysisContext analysisContext) {

    }


    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        logger.info("读取到的表头数据为="+ JSON.toJSONString(headMap));
        CreateTable createTable = new CreateTable();
        String[] head=new String[headMap.size()];
        for (int i=0;i<headMap.size();i++){
            String tableHead = headMap.get(i);
            head[i]=tableHead;
        }
        String tableName = ClassLoadTableName.local.get();
        createTable.createTable(head,tableName);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
