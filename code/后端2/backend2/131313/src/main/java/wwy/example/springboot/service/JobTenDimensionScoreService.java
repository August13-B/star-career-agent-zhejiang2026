package wwy.example.springboot.service;

import wwy.example.springboot.dto.JobTenDimensionScoreDTO;

public interface JobTenDimensionScoreService {
    JobTenDimensionScoreDTO calculateScores(Long profileId);
}