package dev.frankheijden.minecraftreflection;

import dev.frankheijden.minecraftreflection.cache.NamedReflectionCacheTree;
import dev.frankheijden.minecraftreflection.cache.ReflectionCacheTree;
import dev.frankheijden.minecraftreflection.exceptions.MinecraftReflectionException;
import dev.frankheijden.minecraftreflection.utils.ReflectionStringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Reflection {

    private final Class<?> clazz;
    private final Map<String, Field> fieldMap;
    private final NamedReflectionCacheTree<Method> methodTree;
    private final ReflectionCacheTree<Constructor<?>> constructorTree;

    public Reflection(Class<?> clazz) {
        this.clazz = clazz;
        this.fieldMap = new ConcurrentHashMap<>();
        this.methodTree = new NamedReflectionCacheTree<>();
        this.constructorTree = new ReflectionCacheTree<>(null);
    }

    protected Reflection(Reflection reflection) {
        this.clazz = reflection.clazz;
        this.fieldMap = reflection.fieldMap;
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
            return (R) constructorTree.computeIfAbsent(getTypes(parameters), types -> getAccessibleConstructor(clazz, types)).newInstance(parameters);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R newInstance(ClassObject<?>... classObjects) {
        try {
            Class<?>[] parameterTypes = ClassObject.getTypes(classObjects);
            Object[] parameters = ClassObject.getObjects(classObjects);
            return (R) constructorTree.computeIfAbsent(parameterTypes, types -> getAccessibleConstructor(clazz, types)).newInstance(parameters);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R get(Object instance, String field) {
        try {
            return (R) fieldMap.computeIfAbsent(field, k -> getAccessibleField(clazz, field)).get(instance);
        } catch (IllegalAccessException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    public void set(Object instance, String field, Object value) {
        try {
            fieldMap.computeIfAbsent(field, k -> getAccessibleField(clazz, field)).set(instance, value);
        } catch (IllegalAccessException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(Object instance, String method, ClassObject<?>... classObjects) {
        try {
            Class<?>[] parameterTypes = ClassObject.getTypes(classObjects);
            Object[] parameters = ClassObject.getObjects(classObjects);
            return (R) methodTree.computeIfAbsent(method, parameterTypes, types -> getAccessibleMethod(clazz, method, types)).invoke(instance, parameters);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(Object instance, String method, Object... parameters) {
        try {
            return (R) methodTree.computeIfAbsent(method, getTypes(parameters), types -> getAccessibleMethod(clazz, method, types)).invoke(instance, parameters);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new MinecraftReflectionException(ex);
        }
    }

    public static Class<?>[] getTypes(Object[] objects) {
        Class<?>[] types = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            types[i] = objects[i].getClass();
        }
        return types;
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
