package org.zolotarev.t1openschool.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTaskConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "task-updates", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void listenTaskStatusUpdate(TaskDTO taskDTO) {
        log.info("Received task update for task ID: {}", taskDTO.getId());

        notificationService.sendNotification(taskDTO);
        log.info("Successfully processed task update and sent notification for task ID: {}", taskDTO.getId());
    }
}
