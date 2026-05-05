package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.ShiftSwap;
import com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The interface Shift swap repository.
 */
@Repository
public interface ShiftSwapRepository extends JpaRepository<ShiftSwap, Long> {

    /**
     * Find by id with details optional.
     *
     * @param id the id
     * @return the optional
     */
    @Query("""
            select sw
            from ShiftSwap sw
            join fetch sw.requester req
            join fetch req.user reqUser
            join fetch sw.requesterAssignment ra
            join fetch ra.shift raShift
            join fetch sw.targetEmployee te
            join fetch te.user teUser
            left join fetch sw.targetAssignment ta
            left join fetch ta.shift taShift
            where sw.id = :id
            """)
    Optional<ShiftSwap> findByIdWithDetails(@Param("id") Long id);

    /**
     * Find by participant list.
     *
     * @param employeeId the employee id
     * @param status     the status
     * @return the list
     */
    @Query("""
            select sw
            from ShiftSwap sw
            join fetch sw.requester req
            join fetch req.user
            join fetch sw.requesterAssignment ra
            join fetch ra.shift
            join fetch sw.targetEmployee te
            join fetch te.user
            left join fetch sw.targetAssignment ta
            left join fetch ta.shift
            where (sw.requester.id = :employeeId or sw.targetEmployee.id = :employeeId)
              and (:status is null or sw.status = :status)
            order by sw.createdAt desc
            """)
    List<ShiftSwap> findByParticipant(@Param("employeeId") Long employeeId, @Param("status") ShiftSwapStatus status);

    /**
     * Find pending by location ids list.
     *
     * @param locationIds the location ids
     * @return the list
     */
    @Query("""
            select sw
            from ShiftSwap sw
            join fetch sw.requester req
            join fetch req.user
            join fetch sw.requesterAssignment ra
            join fetch ra.shift raShift
            join fetch sw.targetEmployee te
            join fetch te.user
            left join fetch sw.targetAssignment ta
            left join fetch ta.shift
            where sw.status = com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus.PENDING_MANAGER_APPROVAL
              and raShift.location.id in :locationIds
            order by sw.createdAt asc
            """)
    List<ShiftSwap> findPendingByLocationIds(@Param("locationIds") List<Long> locationIds);

    /**
     * Find all pending list.
     *
     * @return the list
     */
    @Query("""
            select sw
            from ShiftSwap sw
            join fetch sw.requester req
            join fetch req.user
            join fetch sw.requesterAssignment ra
            join fetch ra.shift
            join fetch sw.targetEmployee te
            join fetch te.user
            left join fetch sw.targetAssignment ta
            left join fetch ta.shift
            where sw.status = com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus.PENDING_MANAGER_APPROVAL
            order by sw.createdAt asc
            """)
    List<ShiftSwap> findAllPending();
}
