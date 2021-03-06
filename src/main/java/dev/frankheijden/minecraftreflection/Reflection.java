package dev.frankheijden.minecraftreflection;

import dev.frankheijden.minecraftreflection.cache.NamedReflectionCacheTree;
import dev.frankheijden.minecraftreflection.cache.ReflectionCacheTree;
import dev.frankheijden.minecraftreflection.exceptions.MinecraftReflectionException;
import dev.frankheijden.minecraftreflection.utils.ReflectionStringUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Reflection {

    private final Class<?> clazz;
    private final Map<String, MethodHandle> fieldGetterMap;
    private final Map<String, MethodHandle> fieldSetterMap;
    private final NamedReflectionCacheTree<MethodHandle> methodTree;
    private final ReflectionCacheTree<MethodHandle> constructorTree;

    public Reflection(Class<?> clazz) {
        this.clazz = clazz;
        this.fieldGetterMap = new HashMap<>();
        this.fieldSetterMap = new HashMap<>();
        this.methodTree = new NamedReflectionCacheTree<>();
        this.constructorTree = new ReflectionCacheTree<>(null);
    }

    protected Reflection(Reflection reflection) {
        this.clazz = reflection.clazz;
        this.fieldGetterMap = reflection.fieldGetterMap;
        this.fieldSetterMap = reflection.fieldSetterMap;
        this.methodTree = reflection.methodTree;
        this.constructorTree = reflection.constructorTree;
    }

    public static Class<?> getClassFromName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new MinecraftReflectionException("Invalid class '" + className + "'!", ex);
        }
    }

    public Class<?> getClazz() {
        return clazz;
    }

    @SuppressWarnings("unchecked")
    public <R> R newInstance(Object... parameters) {
        try {
            return (R) constructorTree.computeIfAbsent(getTypes(parameters), types -> getConstructorHandle(clazz, types)).invoke(parameters);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R newInstance(ClassObject<?>... classObjects) {
        try {
            Class<?>[] parameterTypes = ClassObject.getTypes(classObjects);
            Object[] parameters = ClassObject.getObjects(classObjects);
            return (R) constructorTree.computeIfAbsent(parameterTypes, types -> getConstructorHandle(clazz, types)).invoke(parameters);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R get(Object instance, String field) {
        try {
            return (R) fieldGetterMap.computeIfAbsent(field, k -> getFieldHandle(clazz, field, true)).invoke(instance);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    public void set(Object instance, String field, Object value) {
        try {
            fieldSetterMap.computeIfAbsent(field, k -> getFieldHandle(clazz, field, false)).invoke(instance, value);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(Object instance, String method, ClassObject<?>... classObjects) {
        try {
            Class<?>[] parameterTypes = ClassObject.getTypes(classObjects);
            Object[] parameters = ClassObject.getObjects(classObjects);
            return (R) methodTree.computeIfAbsent(method, parameterTypes, types -> getMethodHandle(clazz, method, types)).invoke(instance, parameters);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(Object instance, String method, Object... parameters) {
        try {
            return (R) methodTree.computeIfAbsent(method, getTypes(parameters), types -> getMethodHandle(clazz, method, types)).invoke(instance, parameters);
        } catch (Throwable th) {
            throw new MinecraftReflectionException(th);
        }
    }

    public static Class<?>[] getTypes(Object[] objects) {
        Class<?>[] types = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            types[i] = objects[i].getClass();
        }
        return types;
    }

    public static MethodHandle getConstructorHandle(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return MethodHandles.lookup().unreflectConstructor(getAccessibleConstructor(clazz, parameterTypes));
        } catch (IllegalAccessException ex) {
            throw new MinecraftReflectionException("Constructor '" + ReflectionStringUtils.constructor(clazz, parameterTypes) + "' could not be unreflected!", ex);
        }
    }

    public static Constructor<?> getAccessibleConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Constructor<?> c = getConstructor(clazz, parameterTypes);
        if (!c.isAccessible()) c.setAccessible(true);
        return c;
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException ignored) {
            try {
                return clazz.getDeclaredConstructor(parameterTypes);
            } catch (NoSuchMethodException ex) {
                throw new MinecraftReflectionException("Constructor '" + ReflectionStringUtils.constructor(clazz, parameterTypes) + "' not found!", ex);
            }
        }
    }

    public static MethodHandle getFieldHandle(Class<?> clazz, String field, boolean isGetter) {
        try {
            Field f = getAccessibleField(clazz, field);
            if (isGetter) {
                return MethodHandles.lookup().unreflectGetter(f);
            } else {
                return MethodHandles.lookup().unreflectSetter(f);
            }
        } catch (IllegalAccessException ex) {
            throw new MinecraftReflectionException("Field '" + ReflectionStringUtils.field(clazz, field) + "' could not be unreflected!", ex);
        }
    }

    public static Field getAccessibleField(Class<?> clazz, String field) {
        Field f = getField(clazz, field);
        if (!f.isAccessible()) f.setAccessible(true);
        return f;
    }

    public static Field getField(Class<?> clazz, String field) {
        try {
            return clazz.getField(field);
        } catch (NoSuchFieldException ignored) {
            try {
                return clazz.getDeclaredField(field);
            } catch (NoSuchFieldException ex) {
                throw new MinecraftReflectionException("Field '" + ReflectionStringUtils.field(clazz, field) + "' not found!", ex);
            }
        }
    }

    public static MethodHandle getMethodHandle(Class<?> clazz, String method, Class<?>... parameterTypes) {
        try {
            return MethodHandles.lookup().unreflect(getAccessibleMethod(clazz, method, parameterTypes));
        } catch (IllegalAccessException ex) {
            throw new MinecraftReflectionException("Method '" + ReflectionStringUtils.method(clazz, method, parameterTypes) + "' could not be unreflected!", ex);
        }
    }

    public static Method getAccessibleMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        Method m = getMethod(clazz, method, parameterTypes);
        if (!m.isAccessible()) m.setAccessible(true);
        return m;
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(method, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            try {
                return clazz.getDeclaredMethod(method, parameterTypes);
            } catch (NoSuchMethodException ex) {
                throw new MinecraftReflectionException("Method '" + ReflectionStringUtils.method(clazz, method, parameterTypes) + "' not found!", ex);
            }
        }
    }
}
