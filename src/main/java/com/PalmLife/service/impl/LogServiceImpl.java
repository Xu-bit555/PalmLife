package com.PalmLife.service.impl;

import com.PalmLife.entity.LogEntity;
import com.PalmLife.mapper.LogMapper;
import com.PalmLife.service.LogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, LogEntity> implements LogService{

}
