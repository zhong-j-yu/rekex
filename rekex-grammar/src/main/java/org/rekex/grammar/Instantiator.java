package org.rekex.grammar;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public sealed interface Instantiator
{
    record NewInstance(Constructor<?> constructor) implements Instantiator{}

    record StaticMethod(Method method) implements Instantiator{}

    record StaticField(Field field) implements Instantiator{}
}
