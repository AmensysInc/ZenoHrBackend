package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Campaign;
import com.application.employee.service.repositories.CampaignRepository;
import com.application.employee.service.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;

    @Override
    public Campaign saveCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    @Override
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Override
    public Optional<Campaign> getCampaignById(Long id) {
        return campaignRepository.findById(id);
    }

    @Override
    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }

    @Override
    public Campaign updateCampaign(Long id, Campaign campaign) {
        return campaignRepository.findById(id).map(existing -> {
            existing.setName(campaign.getName());
            existing.setSenderEmail(campaign.getSenderEmail());
            existing.setRecipients(campaign.getRecipients());
            existing.setSubject(campaign.getSubject());
            existing.setBody(campaign.getBody());
            return campaignRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Campaign not found with id " + id));
    }
}
