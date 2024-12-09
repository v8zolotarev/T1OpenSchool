package org.zolotarev.t1openschool.service;

import org.zolotarev.t1openschool.aspect.annotations.LogAfterReturning;
import org.zolotarev.t1openschool.aspect.annotations.LogAfterThrowing;
import org.zolotarev.t1openschool.aspect.annotations.LogAround;
import org.zolotarev.t1openschool.aspect.annotations.LogBefore;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.entity.Task;
import org.zolotarev.t1openschool.kafka.KafkaTaskProducer;
import org.zolotarev.t1openschool.mapper.TaskMapper;
import org.zolotarev.t1openschool.repository.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final KafkaTaskProducer kafkaTaskProducer;

    @LogBefore
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = taskMapper.taskDTOToTask(taskDTO);
        task = taskRepository.save(task);
        return taskMapper.taskToTaskDTO(task);
    }

    @LogAfterReturning
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task with " + id + "not found"));
        return taskMapper.taskToTaskDTO(task);
    }

    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task with ID " + id + " not found"));

        existingTask.setTitle(taskDTO.getTitle());
        existingTask.setDescription(taskDTO.getDescription());
        existingTask.setStatus(taskDTO.getStatus());
        Task updatedTask = taskRepository.save(existingTask);

        kafkaTaskProducer.sendTaskStatusUpdate(taskMapper.taskToTaskDTO(updatedTask));

        return taskMapper.taskToTaskDTO(updatedTask);
    }

    @LogAfterThrowing
    public ResponseEntity<Void> deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task with " + id + "not found"));
        taskRepository.delete(task);
        return ResponseEntity.noContent().build();
    }

    @LogAround
    public List<TaskDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(taskMapper::taskToTaskDTO)
                .collect(Collectors.toList());
    }
}
