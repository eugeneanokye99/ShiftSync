package com.shiftsync.shiftsync.audit.annotation;

import com.shiftsync.shiftsync.common.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

    String entityType();

    AuditAction action();

    /**
     * Zero-based index of the method parameter that holds the entity ID.
     * Use -1 (default) to extract the ID from the return value instead.
     */
    int entityIdParam() default -1;
}
