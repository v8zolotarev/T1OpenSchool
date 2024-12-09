package org.zolotarev.t1openschool.dto;

import lombok.Data;
import org.zolotarev.t1openschool.enums.TaskStatus;

@Data
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long user_id;
}
