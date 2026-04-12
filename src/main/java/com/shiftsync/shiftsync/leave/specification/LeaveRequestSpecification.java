package com.shiftsync.shiftsync.leave.specification;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class LeaveRequestSpecification {

    private LeaveRequestSpecification() {
    }

    public static Specification<LeaveRequest> withPendingFilters(
            Long employeeId,
            Long locationId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Specification.where(hasStatus())
                .and(hasEmployeeId(employeeId))
                .and(hasLocationId(locationId))
                .and(overlapsDateRange(startDate, endDate));
    }

    private static Specification<LeaveRequest> hasStatus() {
        return (root, _, cb) -> cb.equal(root.get("status"), LeaveStatus.PENDING);
    }

    private static Specification<LeaveRequest> hasEmployeeId(Long employeeId) {
        return (root, _, cb) -> employeeId == null
                ? cb.conjunction()
                : cb.equal(root.get("employee").get("id"), employeeId);
    }

    private static Specification<LeaveRequest> hasLocationId(Long locationId) {
        return (root, _, cb) -> locationId == null
                ? cb.conjunction()
                : cb.equal(root.get("employee").get("location").get("id"), locationId);
    }

    private static Specification<LeaveRequest> overlapsDateRange(LocalDate startDate, LocalDate endDate) {
        return (root, _, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate == null) {
                return cb.lessThanOrEqualTo(root.get("startDate"), endDate);
            }
            if (endDate == null) {
                return cb.greaterThanOrEqualTo(root.get("endDate"), startDate);
            }
            return cb.and(
                    cb.lessThanOrEqualTo(root.get("startDate"), endDate),
                    cb.greaterThanOrEqualTo(root.get("endDate"), startDate)
            );
        };
    }
}

