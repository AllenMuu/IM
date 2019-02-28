package com.hzt.file.client.vo;

import io.netty.handler.codec.http.HttpResponse;

import java.io.FileOutputStream;
import java.util.List;

public class FileClientHttpResponse {

    private HttpResponse response;
    private boolean isFile;
    private String downFileName;
    private List<byte[]> resultByteList;
    private FileOutputStream fileOutputStream;
    private long contentLength = 0;

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getDownFileName() {
        return downFileName;
    }

    public void setDownFileName(String downFileName) {
        this.downFileName = downFileName;
    }

    public List<byte[]> getResultByteList() {
        return resultByteList;
    }

    public void setResultByteList(List<byte[]> resultByteList) {
        this.resultByteList = resultByteList;
    }

    public FileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void setFileOutputStream(FileOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
