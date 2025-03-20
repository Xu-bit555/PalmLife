package com.PalmLife.LogRecord;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AOPLog {
    String value() default "";

}
