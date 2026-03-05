package com.neuroguard.riskalertservice.client;

import com.neuroguard.riskalertservice.dto.PredictionRequest;
import com.neuroguard.riskalertservice.dto.PredictionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ml-predictor", url = "${ml.service.url:http://localhost:5000}")
public interface MlPredictorClient {

    @PostMapping("/predict")
    PredictionResponse predict(@RequestBody PredictionRequest request);
}