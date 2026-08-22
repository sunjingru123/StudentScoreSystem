package com.student.studentscoresystem.service.impl;

import com.student.studentscoresystem.entity.Position;
import com.student.studentscoresystem.mapper.PositionMapper;
import com.student.studentscoresystem.service.PositionService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PositionServiceImpl implements PositionService {


    private final PositionMapper positionMapper;


    public PositionServiceImpl(PositionMapper positionMapper){
        this.positionMapper = positionMapper;
    }


    @Override
    public List<Position> list(){

        return positionMapper.selectList(null);

    }

}