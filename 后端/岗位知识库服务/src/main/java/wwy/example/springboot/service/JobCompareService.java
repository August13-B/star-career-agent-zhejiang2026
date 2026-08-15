package wwy.example.springboot.service;

import wwy.example.springboot.dto.JobCompareResult;

public interface JobCompareService {
    JobCompareResult compareWithGraph(Long newJobId);
}