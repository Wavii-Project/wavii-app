package com.wavii.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTestHelper {

    public static <T> void testEqualsAndHashCodeExhaustively(T base, T other, Class<T> clazz) throws Exception {
        // Test basic cases
        assertEquals(base, base);
        assertNotEquals(base, null);
        assertNotEquals(base, new Object());
        assertEquals(base, other);
        assertEquals(base.hashCode(), other.hashCode());
        assertNotNull(base.toString());

        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            Object originalVal = f.get(base);
            
            // Generar un valor diferente
            Object diffVal = getDifferentValue(f.getType(), originalVal);
            
            if (diffVal != null) {
                // Caso: uno es null, el otro no (solo si no es primitivo)
                if (!f.getType().isPrimitive()) {
                    f.set(base, null);
                    f.set(other, diffVal);
                    assertNotEquals(base, other);
                    
                    f.set(base, diffVal);
                    f.set(other, null);
                    assertNotEquals(base, other);
                    
                    // Caso: ambos null
                    f.set(base, null);
                    f.set(other, null);
                    assertEquals(base, other);
                    assertEquals(base.hashCode(), other.hashCode());
                }
                
                // Caso: valores diferentes
                f.set(base, originalVal);
                f.set(other, diffVal);
                assertNotEquals(base, other);
                
                // Restaurar
                f.set(base, originalVal);
                f.set(other, originalVal);
            }
        }
    }

    private static Object getDifferentValue(Class<?> type, Object current) {
        if (type == String.class) return "diff_" + current;
        if (type == int.class || type == Integer.class) return (current == null ? 0 : (Integer)current) + 1;
        if (type == long.class || type == Long.class) return (current == null ? 0L : (Long)current) + 1L;
        if (type == boolean.class || type == Boolean.class) return current == null ? true : !(Boolean)current;
        if (type == double.class || type == Double.class) return (current == null ? 0.0 : (Double)current) + 1.1;
        if (type == float.class || type == Float.class) return (current == null ? 0.0f : (Float)current) + 1.1f;
        if (type == BigDecimal.class) return (current == null ? BigDecimal.ZERO : (BigDecimal)current).add(BigDecimal.ONE);
        if (type == UUID.class) return UUID.randomUUID();
        if (type == LocalDateTime.class) return LocalDateTime.now().plusDays(1);
        if (type == LocalDate.class) return LocalDate.now().plusDays(1);
        if (type == List.class) {
            List<String> list = new java.util.ArrayList<>();
            if (current == null || ((List<?>)current).isEmpty()) {
                list.add("diff_item");
            }
            return list;
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants.length > 1) {
                return constants[0].equals(current) ? constants[1] : constants[0];
            }
        }
        // Para tipos complejos (User, etc), devolvemos un nuevo objeto si podemos, o null si es muy complejo
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
