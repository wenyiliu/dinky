package com.dlink.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.dlink.db.mapper.SuperMapper;
import com.dlink.model.JobRestart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author liuwenyi
 * @date 2023/2/20
 **/
@Mapper
public interface JobRestartMapper extends SuperMapper<JobRestart> {

    @InterceptorIgnore(tenantLine = "true")
    JobRestart selectLatestByTaskId(@Param("taskId") Integer taskId);
}
