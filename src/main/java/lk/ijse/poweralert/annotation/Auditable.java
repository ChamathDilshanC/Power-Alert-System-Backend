package lk.ijse.poweralert.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed (e.g., CREATE, UPDATE, DELETE)
     */
    String action();

    /**
     * The type of entity being acted upon (e.g., User, Outage, Address)
     * If empty, will try to infer from the return type
     */
    String entityType() default "";

    /**
     * The field name to use for getting the entity ID
     * If empty, will try common ID field names (id, Id, ID, entityId)
     */
    String entityIdField() default "";

    /**
     * Additional details to include in the audit log
     * If empty, will include method name and arguments
     */
    String details() default "";
}