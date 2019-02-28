package com.hzt.file.server.config;

import com.hzt.common.context.BeanTool;
import com.hzt.file.server.constant.FileServerConstants;
import com.hzt.file.server.controller.FileServerController;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于缓存文件服务器的 URL
 */
public final class FileServerConfig {

    // 文件服务器URL及对应处理类列表
    private static final Map<String, FileServerController> fileServerControllerMap = new HashMap<String, FileServerController>();

    /**
     * 文件服务器参数初始化
     */
    public static void fileSeverInit() {

        // 初始化Controller缓存
        fileSeverControllerInit();

        // 初始化系统文件存储路径
        fileSeverFilePathInit();
    }

    /**
     * 初始化Controller映射
     */
    private static void fileSeverControllerInit(){
        fileServerControllerMap.put("/file/upload", BeanTool.getBean("FileServerControllerUpload", FileServerController.class));
        fileServerControllerMap.put("/file/download", BeanTool.getBean("FileServerControllerDownload", FileServerController.class));
        fileServerControllerMap.put("/favicon.ico", BeanTool.getBean("FileServerControllerFavicon", FileServerController.class));
    }

    private static void fileSeverFilePathInit(){

        // 若存储目录不存在，则创建该目录
        File storagePath = new File(FileServerConstants.FILE_STORAGE_PATH);
        if(!storagePath.exists()){
            storagePath.mkdirs();
        }

        // 若临时文件目录不存在，则创建该目录
        File storageTempPath = new File(FileServerConstants.FILE_TEMP_STORAGE_PATH);
        if(!storageTempPath.exists()){
            storageTempPath.mkdirs();
        }

        // 设置大文件上传的临时目录，建议与存文件存放目录放在位置：Linux：同一驱动器下，Windows：同一盘符下
        // 好处：1.文件移动操作更高效 2.解决因不同驱动器（盘符）下文件系统不同而导致的失败
        DiskFileUpload.baseDirectory = FileServerConstants.FILE_TEMP_STORAGE_PATH;
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = FileServerConstants.FILE_TEMP_STORAGE_PATH;
        DiskAttribute.deleteOnExitTemporaryFile = true;
    }

    /**
     * 根据URL获取文件处理类
     * @param handlerName
     * @return
     */
    public static FileServerController getHandler(String handlerName){
        return StringUtils.isBlank(handlerName)? null :  fileServerControllerMap.get(handlerName);
    }
}
