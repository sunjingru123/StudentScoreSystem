package com.student.studentscoresystem.controller;


import com.student.studentscoresystem.entity.Position;
import com.student.studentscoresystem.service.PositionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/position")
public class PositionController {


    private final PositionService positionService;


    public PositionController(PositionService positionService){
        this.positionService = positionService;
    }


    @GetMapping("/list")
    public List<Position> list(){

        return positionService.list();

    }

}