package com.hzt.file.client.config;

import com.hzt.file.client.constant.FileClientConstants;

import java.io.File;

public final class FileClientConfig {

    /**
     * 文件客户端参数初始化
     */
    public static void fileClientInit() {

        String filePath = FileClientConstants.CLIENT_STORAGE_PATH;
        File file = new File(filePath);
        if(!file.exists()){
            file.mkdirs();
        }
    }
}
