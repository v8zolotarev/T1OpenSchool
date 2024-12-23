package org.zolotarev.t1openschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.entity.Task;
import org.zolotarev.t1openschool.enums.TaskStatus;
import org.zolotarev.t1openschool.kafka.KafkaTaskProducer;
import org.zolotarev.t1openschool.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTaskProducer kafkaTaskProducer;

    private TaskDTO taskDTO;

    private List<Long> createdTaskIds;

    @BeforeEach
    public void setUp() {
        taskDTO = new TaskDTO(null, "Test Task", "Test Description", TaskStatus.CREATED, 100L);
        createdTaskIds = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        for (Long taskId : createdTaskIds) {
            taskRepository.deleteById(taskId);
        }
    }

    @Test
    public void testCreateTask() throws Exception {
        String taskJson = objectMapper.writeValueAsString(taskDTO);
        mockMvc.perform(post("/tasks/create")
                        .contentType("application/json")
                        .content(taskJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Test Task")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.status", is("CREATED")));

        Optional<Task> createdTask = taskRepository.findByTitle("Test Task");
        if (createdTask.isPresent()) {
            createdTaskIds.add(createdTask.get().getId());
        } else {
            throw new RuntimeException("Task not found after creation");
        }
    }


    @Test
    public void testUpdateTask() throws Exception {
        Task task = new Task(null, "Test Task", "Test Description", TaskStatus.CREATED, 100L);
        taskRepository.save(task);
        createdTaskIds.add(task.getId());

        TaskDTO updatedTaskDTO = new TaskDTO(task.getId(), "Updated Task", "Updated Description", TaskStatus.UPDATED, 100L);
        String updatedTaskJson = objectMapper.writeValueAsString(updatedTaskDTO);

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .contentType("application/json")
                        .content(updatedTaskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Task")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.status", is("UPDATED")));

        verify(kafkaTaskProducer, times(1)).sendTaskStatusUpdate(updatedTaskDTO);
    }

    @Test
    public void testDeleteTask() throws Exception {
        Task task = new Task(null, "Test Task", "Test Description", TaskStatus.CREATED, 100L);
        taskRepository.save(task);
        createdTaskIds.add(task.getId());

        mockMvc.perform(delete("/tasks/{id}", task.getId()))
                .andExpect(status().isNoContent());

        boolean taskExists = taskRepository.existsById(task.getId());
        assert !taskExists;
    }

    @Test
    public void testGetAllTasks() throws Exception {
        Task task1 = new Task(null, "Task 1", "Description 1", TaskStatus.CREATED, 100L);
        Task task2 = new Task(null, "Task 2", "Description 2", TaskStatus.CREATED, 100L);
        taskRepository.save(task1);
        taskRepository.save(task2);
        createdTaskIds.add(task1.getId());
        createdTaskIds.add(task2.getId());

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Task 1")))
                .andExpect(jsonPath("$[1].title", is("Task 2")));
    }
}