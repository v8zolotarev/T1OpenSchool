package org.zolotarev.t1openschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zolotarev.t1openschool.entity.Task;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTitle(String title);

}
