package com.student.studentscoresystem.vo;


import lombok.Data;
import com.student.studentscoresystem.vo.DepartmentMemberVO;

import java.util.List;


@Data
public class LoginVO {


    private Long id;

    private String username;

    private String realName;

    private String role;

    private String token;
    private List<DepartmentMemberVO> departments;
}