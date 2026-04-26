package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.ShiftSwap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftSwapRepository extends JpaRepository<ShiftSwap, Long> {

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
}
