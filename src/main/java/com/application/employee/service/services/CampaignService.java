package com.application.employee.service.services;
import com.application.employee.service.entities.Campaign;

import java.util.List;
import java.util.Optional;

public interface CampaignService {

    Campaign saveCampaign(Campaign campaign);

    List<Campaign> getAllCampaigns();

    Optional<Campaign> getCampaignById(Long id);

    void deleteCampaign(Long id);

    Campaign updateCampaign(Long id, Campaign campaign);
}

