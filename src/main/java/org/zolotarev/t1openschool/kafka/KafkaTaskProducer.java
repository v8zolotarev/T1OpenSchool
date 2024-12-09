package org.zolotarev.t1openschool.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.zolotarev.t1openschool.dto.TaskDTO;

@Slf4j
@RequiredArgsConstructor
public class KafkaTaskProducer {

    private final KafkaTemplate<String, TaskDTO> kafkaTemplate;

    @Value("${spring.kafka.topic.task-updates}")
    private String taskStatusTopic;

    public void sendTaskStatusUpdate(TaskDTO taskDTO) {
        kafkaTemplate.send(taskStatusTopic, taskDTO.getUser_id().toString(), taskDTO);
    }
}
