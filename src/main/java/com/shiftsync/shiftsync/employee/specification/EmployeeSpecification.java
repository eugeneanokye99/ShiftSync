package com.shiftsync.shiftsync.employee.specification;

import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.employee.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class EmployeeSpecification {

    public static Specification<Employee> withFilters(
            Long departmentId,
            Long locationId,
            EmploymentType employmentType,
            Boolean active,
            List<Long> locationScope
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (departmentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("department").get("id"), departmentId));
            }

            if (locationId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            }

            if (employmentType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("employmentType"), employmentType));
            }

            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }

            if (locationScope != null) {
                if (locationScope.isEmpty()) {
                    predicate = cb.and(predicate, cb.disjunction());
                } else {
                    predicate = cb.and(predicate, root.get("location").get("id").in(locationScope));
                }
            }

            return predicate;
        };
    }
}

