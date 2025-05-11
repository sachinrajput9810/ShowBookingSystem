package com.nt.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Show {
    private String name;
    private String genre;
    private Map<String, Integer> slots = new HashMap<>();
}
