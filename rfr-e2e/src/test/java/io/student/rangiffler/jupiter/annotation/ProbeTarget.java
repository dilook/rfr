package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("probe")
@ExtendWith(ProbeTargetCondition.class)
public @interface ProbeTarget {
}
