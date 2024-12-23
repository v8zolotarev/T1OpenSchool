package org.zolotarev.t1openschool.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.entity.Task;
import org.zolotarev.t1openschool.enums.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskMapperTest {

    private TaskMapper taskMapper;

    @BeforeEach
    public void setUp() {
        taskMapper = Mappers.getMapper(TaskMapper.class);
    }

    @Test
    public void testTaskToTaskDTO() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Task description");
        task.setStatus(TaskStatus.CREATED);
        task.setUser_id(100L);

        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        assertEquals(task.getId(), taskDTO.getId());
        assertEquals(task.getTitle(), taskDTO.getTitle());
        assertEquals(task.getDescription(), taskDTO.getDescription());
        assertEquals(task.getStatus(), taskDTO.getStatus());
        assertEquals(task.getUser_id(), taskDTO.getUser_id());
    }

    @Test
    public void testTaskDTOToTask() {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Test Task DTO");
        taskDTO.setDescription("DTO description");
        taskDTO.setStatus(TaskStatus.UPDATED);
        taskDTO.setUser_id(200L);

        Task task = taskMapper.taskDTOToTask(taskDTO);

        assertEquals(taskDTO.getId(), task.getId());
        assertEquals(taskDTO.getTitle(), task.getTitle());
        assertEquals(taskDTO.getDescription(), task.getDescription());
        assertEquals(taskDTO.getStatus(), task.getStatus());
        assertEquals(taskDTO.getUser_id(), task.getUser_id());
    }
}