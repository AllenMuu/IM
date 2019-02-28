package com.hzt.file.server.service.impl;

import com.hzt.common.base.domain.Result;
import com.hzt.common.base.domain.vo.CommonFileVO;
import com.hzt.common.constant.Constants;
import com.hzt.file.server.dao.FileDao;
import com.hzt.file.server.domain.po.CommonFile;
import com.hzt.file.server.domain.qo.FileQueryVO;
import com.hzt.file.server.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件服务
 * @author wangqinjun@vichain.com
 * @since 2018-09-28
 */
@Service("FileService")
public class FileServiceImpl implements FileService {

    @Autowired
    private FileDao fileDao;

    @Override
    public Result getFileById(Long fileId) {
        CommonFile commonFile = fileDao.selectById(fileId);
        return Result.createWithModel("文件查询成功！", commonFile);
    }

    @Override
    public Result getFileById(FileQueryVO fileQueryVO) {

        // 参数校验
        boolean paramCheck = fileQueryVO == null || fileQueryVO.getCondition() == null || fileQueryVO.getCondition().getFileId() == null;
        if(paramCheck){
            return Result.createWithErrorMessage("文件查询失败！", Constants.QUERY_FAIL);
        }

        // 查询文件
        CommonFile commonFile = fileDao.selectById(fileQueryVO.getCondition().getFileId());

        // 准备返回文件对象
        CommonFileVO commonFileVO = new CommonFileVO();
        commonFileVO.setId(commonFile.getId());
        commonFileVO.setOriginalName(commonFile.getOriginalName());
        commonFileVO.setExtend(commonFile.getExtend());
        commonFileVO.setSize(commonFile.getSize());
        commonFileVO.setCreateTime(commonFile.getCreateTime());

        return Result.createWithModel("文件查询成功！", commonFile);
    }

    @Override
    public Result createFileIndex(CommonFile commonFile) {
        fileDao.insert(commonFile);
        return Result.createWithSuccessMessage("文件保存成功！");
    }
}
