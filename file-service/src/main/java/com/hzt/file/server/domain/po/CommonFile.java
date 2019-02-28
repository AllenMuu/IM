package com.hzt.file.server.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件对象
 * @author wangqinjun@vichain.com
 * @since 2018-09-28
 */
@TableName("file_file")
public class CommonFile extends Model<CommonFile> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String code;

    private String originalName;

    private String storeName;

    private String extend;

    private Long size;

    private String storePath;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private Integer isDeleted;


    public Long getId() {
        return id;
    }

    public CommonFile setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public CommonFile setCode(String code) {
        this.code = code;
        return this;
    }

    public String getOriginalName() {
        return originalName;
    }

    public CommonFile setOriginalName(String originalName) {
        this.originalName = originalName;
        return this;
    }

    public String getStoreName() {
        return storeName;
    }

    public CommonFile setStoreName(String storeName) {
        this.storeName = storeName;
        return this;
    }

    public String getExtend() {
        return extend;
    }

    public CommonFile setExtend(String extend) {
        this.extend = extend;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public CommonFile setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getStorePath() {
        return storePath;
    }

    public CommonFile setStorePath(String storePath) {
        this.storePath = storePath;
        return this;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public CommonFile setCreateBy(Long createBy) {
        this.createBy = createBy;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public CommonFile setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public CommonFile setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public CommonFile setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public CommonFile setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
        return this;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "File{" +
        "id=" + id +
        ", originalName=" + originalName +
        ", storeName=" + storeName +
        ", extend=" + extend +
        ", size=" + size +
        ", storePath=" + storePath +
        ", createBy=" + createBy +
        ", createTime=" + createTime +
        ", updateBy=" + updateBy +
        ", updateTime=" + updateTime +
        ", isDeleted=" + isDeleted +
        "}";
    }
}
