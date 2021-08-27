package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.spec.Permits;
import org.rekex.spec.Regex;
import org.rekex.spec.SizeLimit;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.AtLeast;
import org.rekex.helper.anno.AtMost;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@SuppressWarnings("all") // supress never used
public class GrammarBadTest extends GrammarTestBase
{
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface A{ int value() default 0; }

    public record Foo(Foo foo){}

    public static class EmptyCatalog{}

    public record NoRuleForPrimitive(@Regex("\\d+")double x){}

    public record ZeroRegex(@A()char x){}
    public record TwoRegex(@Ch("")@Str("")char x){}

    public record InvalidRegex(@Regex("(")String str){}
    public record InvalidRegexGroup(@Regex(value="a", group=1)String str){}

    public record WrongSizeLimit1(@SizeLimit(min=5,max=3) List<Foo> list){}
    public record WrongSizeLimit2(@AtLeast(5)@AtMost(3)List<Foo> list){}

    static class ClassNotPublic{}
    public class ClassNotStatic {}

    public static class ZeroPublicConstructor{
        ZeroPublicConstructor(){}
    }
    public static class OneDefaultConstructor{
    }
    public static class TwoPublicConstructor{
        public TwoPublicConstructor(){}
        public TwoPublicConstructor(int x){}
    }

    public enum EmptyEnum{}
    public enum EnumZeroRegex{ e1; }
    public enum EnumTwoRegex{ @Ch("")@Str("")e1; }

    public sealed interface SuperT<T>{
        record SubString() implements SuperT<String>{}
    }
    public record SubInferFail(SuperT<Integer> s){}

    @Permits({})
    public interface PermitsEmpty{}

    @Permits({PermitsDup.Sub.class, PermitsDup.Sub.class})
    public interface PermitsDup{
        public record Sub() implements PermitsDup{}
    }

    @Permits(PermitsSelf.class)
    public interface PermitsSelf{}

    @Permits(Foo.class)
    public interface PermitsNonSub{}

    @Permits(PermitsIncomplete.Sub1.class)
    public sealed interface PermitsIncomplete{
        record Sub1() implements PermitsIncomplete{}
        record Sub2() implements PermitsIncomplete{}
    }

    public static class CtorNotPublic
    {
        @Ctor static CtorNotPublic ctor(){ return null; }
    }
    public static class CtorWrongType
    {
        @Ctor public static
        Foo ctor(){ return null; }
    }


    @Test
    void test1()
    {
        Exception ex = null;

        ex=failOn(Foo.class, EmptyCatalog.class, "no ctor");

        Class<?>[] disallowed = {int.class, char.class, boolean.class,
            Object.class, Class.class, String.class, Character.class};
        for(var clazz : disallowed)
            ex=failOn(clazz, "disallowed rule datatype");

        // char[] -> char -> fail
        ex=failOn(char[].class, "disallowed rule datatype");

        ex=failOn(NoRuleForPrimitive.class, "no predefined rule");

        ex=failOn(ZeroRegex.class, "no @Regex");
        ex=failOn(TwoRegex.class, "multiple @Regex");

        ex=failOn(InvalidRegex.class, "PatternSyntaxException");
        ex=failOn(InvalidRegexGroup.class, "invalid group number");

        ex=failOn(WrongSizeLimit1.class, "incorrect @SizeLimit");
        ex=failOn(WrongSizeLimit2.class, "incorrect @SizeLimit");

        ex=failOn(ClassNotStatic.class, "static");

        ex=failOn(ClassNotPublic.class, "public");

        ex=failOn(ZeroPublicConstructor.class, "single non-default public constructor");
        ex=failOn(TwoPublicConstructor.class, "single non-default public constructor");
        ex=failOn(OneDefaultConstructor.class, "found only a default constructor");

        ex=failOn(EmptyEnum.class, "empty");

        ex=failOn(EnumZeroRegex.class, "no @Regex");

        ex=failOn(EnumTwoRegex.class, "multiple @Regex");

        ex=failOn(SubInferFail.class, "cannot infer");

        ex=failOn(PermitsEmpty.class, "contains no");

        ex=failOn(PermitsDup.class, "duplicate");

        ex=failOn(PermitsSelf.class, "not a proper subclass");

        ex=failOn(PermitsNonSub.class, "not a proper subclass");

        ex=failOn(PermitsIncomplete.class, "inconsistent");

        ex=failOn(CtorNotPublic.class, CtorNotPublic.class, "public");
        ex=failOn(CtorNotPublic.class, "public");

        ex=failOn(CtorWrongType.class, "not a subtype of");

    }
}
