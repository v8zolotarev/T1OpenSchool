package org.zolotarev.t1openschool.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.entity.Task;
import org.zolotarev.t1openschool.kafka.KafkaTaskProducer;
import org.zolotarev.t1openschool.mapper.TaskMapper;
import org.zolotarev.t1openschool.repository.TaskRepository;
import org.zolotarev.t1openschool.enums.TaskStatus;

import java.util.Optional;
import java.util.List;

public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private KafkaTaskProducer kafkaTaskProducer;

    private TaskService taskService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        taskService = new TaskService(taskRepository, taskMapper, kafkaTaskProducer);
    }

    @Test
    public void testCreateTask() {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Description");
        taskDTO.setStatus(TaskStatus.CREATED);
        taskDTO.setUser_id(1L);

        Task task = new Task(1L, "Test Task", "Description", TaskStatus.CREATED, 1L);

        when(taskMapper.taskDTOToTask(taskDTO)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.taskToTaskDTO(task)).thenReturn(taskDTO);

        TaskDTO result = taskService.createTask(taskDTO);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals(TaskStatus.CREATED, result.getStatus());
        verify(taskRepository).save(task);
    }

    @Test
    public void testGetTaskById_Success() {
        Task task = new Task(1L, "Test Task", "Description", TaskStatus.CREATED, 1L);
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Description");
        taskDTO.setStatus(TaskStatus.CREATED);
        taskDTO.setUser_id(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.taskToTaskDTO(task)).thenReturn(taskDTO);

        TaskDTO result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals(TaskStatus.CREATED, result.getStatus());
    }

    @Test
    public void testGetTaskById_TaskNotFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taskService.getTaskById(1L));
        assertEquals("Task with 1not found", exception.getMessage());
    }

    @Test
    public void testUpdateTask() {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Updated Task");
        taskDTO.setDescription("Updated Description");
        taskDTO.setStatus(TaskStatus.UPDATED);
        taskDTO.setUser_id(1L);

        Task existingTask = new Task(1L, "Old Task", "Old Description", TaskStatus.CREATED, 1L);
        Task updatedTask = new Task(1L, "Updated Task", "Updated Description", TaskStatus.UPDATED, 1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskMapper.taskToTaskDTO(updatedTask)).thenReturn(taskDTO);
        when(taskRepository.save(existingTask)).thenReturn(updatedTask);

        TaskDTO result = taskService.updateTask(1L, taskDTO);

        assertNotNull(result);
        assertEquals("Updated Task", result.getTitle());
        assertEquals(TaskStatus.UPDATED, result.getStatus());
        verify(kafkaTaskProducer).sendTaskStatusUpdate(taskDTO);
    }

    @Test
    public void testDeleteTask() {
        Task task = new Task(1L, "Test Task", "Description", TaskStatus.CREATED, 1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        ResponseEntity<Void> result = taskService.deleteTask(1L);

        assertEquals(ResponseEntity.noContent().build(), result);
        verify(taskRepository).delete(task);
    }

    @Test
    public void testGetAllTasks() {
        Task task1 = new Task(1L, "Task 1", "Description 1", TaskStatus.CREATED, 1L);
        Task task2 = new Task(2L, "Task 2", "Description 2", TaskStatus.UPDATED, 1L);

        List<Task> tasks = List.of(task1, task2);
        List<TaskDTO> taskDTOs = List.of(
                new TaskDTO(1L, "Task 1", "Description 1", TaskStatus.CREATED, 1L),
                new TaskDTO(2L, "Task 2", "Description 2", TaskStatus.UPDATED, 1L)
        );

        when(taskRepository.findAll()).thenReturn(tasks);
        when(taskMapper.taskToTaskDTO(task1)).thenReturn(taskDTOs.get(0));
        when(taskMapper.taskToTaskDTO(task2)).thenReturn(taskDTOs.get(1));

        List<TaskDTO> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals(TaskStatus.CREATED, result.get(0).getStatus());
    }
}
