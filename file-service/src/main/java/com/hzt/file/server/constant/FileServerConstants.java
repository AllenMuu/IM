package com.hzt.file.server.constant;

public class FileServerConstants {

    // 文件存储路径
    public static final String FILE_STORAGE_PATH = "/home/hzt_file";

    // 文件分片缓存路径(临时目录)
    public static final String FILE_TEMP_STORAGE_PATH = FILE_STORAGE_PATH + "/temp";

    // 响应头格式
    public static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    // 响应头跨域设置
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "*";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "post, get, options, delete";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "origin, x-requested-with, content-type, accept, authorization";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "true";
    public static final String ACCESS_CONTROL_MAX_AGE = "3600";

    // 响应头缓存
    public static final String CACHE_CONTROL_MAX_AGE = "max-age=31536000";
    public static final String CACHE_CONTROL_NO_CACHE = "no-cache";
    public static final String CACHE_CONTROL_CACHE = "only-if-cached";

    // 断点续传响应头
    public static final String ACCEPT_RANGES = "bytes";

    // 响应头附件
    public static final String CONTENT_DISPOSITION = "attachment;filename=";

    // 断点续传缓冲大小，默认为 1MB（content-range的长度）
    public static final int CONTENT_RANGE_BUFF_LENGTH = 1024 * 1024;

    // 上传文件名称长度限制
    public static final int FILE_NAME_LENGTH = 64;

    // 上传文件大小限制
    public static final int FILE_SIZE = 1024 * 1024 * 30;
}
