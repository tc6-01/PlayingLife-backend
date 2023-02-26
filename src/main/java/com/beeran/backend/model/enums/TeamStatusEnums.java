package com.beeran.backend.model.enums;


/**
 * 队伍状态枚举
 */
public enum TeamStatusEnums {

    PUBLIC(0, "公开"),
    PRIVATE(1,"私有"),
    CRYPTO(2,"加密");
    private int value;
    private String description;
    public static TeamStatusEnums getEnumsByValue(Integer value){
        if (value == null) {
            return null;
        }
        TeamStatusEnums[] values = TeamStatusEnums.values();
        for (TeamStatusEnums teamStatusEnums:values) {
            if (teamStatusEnums.getValue() == value ){
                return teamStatusEnums;
            }
        }
        return  null;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    TeamStatusEnums(int value, String description){
        this.value = value;
        this.description = description;
    }
}
