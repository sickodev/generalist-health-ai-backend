package com.generalisthealthai.rcm.domain.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringListJsonConverterTest {

    private StringListJsonConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListJsonConverter();
    }

    @Test
    void testConvertToDatabaseColumn() {
        List<String> codes = Arrays.asList("99213", "99214");
        String json = converter.convertToDatabaseColumn(codes);
        assertEquals("[\"99213\",\"99214\"]", json);
    }

    @Test
    void testConvertToDatabaseColumnNullOrEmpty() {
        assertEquals("[]", converter.convertToDatabaseColumn(null));
        assertEquals("[]", converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void testConvertToEntityAttribute() {
        List<String> list = converter.convertToEntityAttribute("[\"M54.5\",\"R07.9\"]");
        assertEquals(2, list.size());
        assertEquals("M54.5", list.get(0));
        assertEquals("R07.9", list.get(1));
    }

    @Test
    void testConvertToEntityAttributeNullOrEmpty() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertTrue(converter.convertToEntityAttribute("   ").isEmpty());
        assertTrue(converter.convertToEntityAttribute("invalid json").isEmpty());
    }
}
