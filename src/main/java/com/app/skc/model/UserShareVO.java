package com.app.skc.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserShareVO implements Serializable {
    private String id;
    private String level;
    private String name;
    private String password;
    private String mobilePhone;
    private String creatDateTime;
    private String status;
    private String partentId;
    private String tradePassword;
    private String gradeId;
    private List<UserShareVO> subUsers;

}
