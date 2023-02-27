package com.dlink.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author liuwenyi
 * @date 2023/2/20
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dlink_job_restart")
public class JobRestart implements Serializable {

    private static final long serialVersionUID = -5960332040190903443L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * task id
     */
    private Integer taskId;

    private String taskName;

    private Integer isProhibit;

    /**
     * restart_num
     */
    private Integer restartNum;

    /**
     * create time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
