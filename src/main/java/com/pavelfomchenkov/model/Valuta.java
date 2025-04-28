package com.pavelfomchenkov.model;

import lombok.Data;

@Data
public class Valuta {
    private String valuteID;
    private int numCode;
    private String charCode;
    private int nominal;
    private String name;
    private double value;
    private double vUnitRate;
}
