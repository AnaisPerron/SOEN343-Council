package com.council.tenderapp.dlv.repositories;

import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.entities.AssignmentStatus;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    java.util.List<Assignment> findByOrderIdInOrderByAssignedAtDescIdDesc(Collection<Long> orderIds);
    Optional<Assignment> findFirstByOrderIdAndCourierIdOrderByAssignedAtDescIdDesc(Long orderId, Long courierId);
    Optional<Assignment> findByOrderIdAndStatusIn(Long orderId, Collection<AssignmentStatus> statuses);
    boolean existsByCourierIdAndStatusIn(Long courierId, Collection<AssignmentStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Assignment> findWithLockById(Long id);
}
