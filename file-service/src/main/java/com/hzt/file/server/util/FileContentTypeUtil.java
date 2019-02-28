package com.hzt.file.server.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Description:
 * @Auther: wangqinjun@vichain.com
 * @Date: 2019/1/26 14:40
 */
public class FileContentTypeUtil {

    /**
     * 根据文件获取响应头的Content_Type
     * @param file
     * @return
     */
    public static String getContentTypeByFile(File file) {
        String fileName = file.getName();
        int index = StringUtils.isBlank(fileName) ? -1 : fileName.lastIndexOf(".");
        String suffix = index < 0 ? null : fileName.substring(index).toLowerCase();
        return FileContentType.getContentType(suffix);
    }

    /**
     * 内部类，用于初始化文件后缀对应的content-type
     */
    private static final class FileContentType {

        private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
        private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

        static {
            try {
                Properties properties = PropertiesLoaderUtils.loadAllProperties("resource/content-type.properties");
                properties.forEach((key, value) -> {
                    CONTENT_TYPE_MAP.put(key.toString(), value.toString());
                });
            } catch (Exception e) {
            }
        }

        private static final String getContentType(String suffix) {
            String contentType = CONTENT_TYPE_MAP.get(suffix);
            return contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
        }
    }
}
