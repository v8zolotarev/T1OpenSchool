package org.zolotarev.t1openschool.mapper;

import org.mapstruct.Mapper;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.entity.Task;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskDTO taskToTaskDTO(Task task);
    Task taskDTOToTask(TaskDTO taskDTO);
}