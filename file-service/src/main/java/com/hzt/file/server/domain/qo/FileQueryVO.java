package com.hzt.file.server.domain.qo;

import com.hzt.common.base.domain.qo.BaseQueryVO;

public class FileQueryVO extends BaseQueryVO {

    private FileQueryConditionVO condition;

    public FileQueryConditionVO getCondition() {
        return condition;
    }

    public void setCondition(FileQueryConditionVO condition) {
        this.condition = condition;
    }
}
