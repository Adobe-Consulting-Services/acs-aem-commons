package com.adobe.acs.commons.mcp.form;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static org.junit.Assert.*;

public class FieldComponentTest {
    @Test
    public void hasOption() throws Exception {
        FieldComponent testComponent = new TestFieldComponent(new String[]{"a=b", "c", "d="});
        assertTrue(testComponent.hasOption("a"));
        assertTrue(testComponent.hasOption("c"));
        assertTrue(testComponent.hasOption("d"));
        assertFalse(testComponent.hasOption("z"));
    }

    @Test
    public void getOption() throws Exception {
        FieldComponent testComponent = new TestFieldComponent(new String[]{"a=b", "c", "d="});
        assertEquals("b", testComponent.getOption("a").get());
        assertEquals(Optional.empty(), testComponent.getOption("c"));
        assertEquals(Optional.empty(), testComponent.getOption("z"));
    }

    public class TestFieldComponent extends FieldComponent {
        public TestFieldComponent(String[] options) {
            formField = new FormField() {
                @Override
                public boolean equals(Object obj) {
                    return false;
                }

                @Override
                public int hashCode() {
                    return 0;
                }

                @Override
                public String toString() {
                    return null;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }

                @Override
                public String name() {
                    return null;
                }

                @Override
                public String hint() {
                    return null;
                }

                @Override
                public String description() {
                    return null;
                }

                @Override
                public boolean required() {
                    return false;
                }

                @Override
                public Class<? extends FieldComponent> component() {
                    return null;
                }

                @Override
                public String[] options() {
                    return options;
                }
            };
        }

        @Override
        public void init() {

        }
    }

}