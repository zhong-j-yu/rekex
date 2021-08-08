package org.rekex.annotype;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnoTypeTokenTest
{
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface A{ int value() default 0; }

    record C(){}

    @Test
    void test() throws Exception
    {
        AnnoType type;
        
        type = new AnnoTypeToken<C>(){}.get();
        assertEquals(type.toString(false), "C");

        type = new AnnoTypeToken<@A(1)C>(){}.get();
        assertEquals(type.toString(false), "@A(1)C");

        type = new AnnoTypeToken<@A(2)List<C>>(){}.get();
        assertEquals(type.toString(false), "@A(2)List<C>");

        type = new AnnoTypeToken<@A(3)List<@A(4)C>>(){}.get();
        assertEquals(type.toString(false), "@A(3)List<@A(4)C>");

    }
}