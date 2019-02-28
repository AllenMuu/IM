package com.hzt.file.server.service;

import com.hzt.common.base.domain.Result;
import com.hzt.file.server.domain.po.CommonFile;
import com.hzt.file.server.domain.qo.FileQueryVO;


/**
 *  服务类
 * @author wangqinjun@vichain.com
 * @since 2018-09-28
 */
public interface FileService {

    Result getFileById(Long fileId);

    Result getFileById(FileQueryVO fileQueryVO);

    Result createFileIndex(CommonFile commonFile);
}
