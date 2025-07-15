package eu.deltasw.tmdb_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import eu.deltasw.common.model.ProviderInfo;
import eu.deltasw.common.model.WatchInfo;
import info.movito.themoviedbapi.model.core.watchproviders.Provider;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;

@Service
public class WatchProvidersMapperService {

    private final ModelMapper modelMapper;

    public WatchProvidersMapperService(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ProviderInfo convertTo(Provider provider) {
        return modelMapper.map(provider, ProviderInfo.class);
    }

    public WatchInfo convertTo(WatchProviders watchProviders) {
        if (watchProviders == null) {
            return null;
        }
        WatchInfo dto = new WatchInfo();

        List<ProviderInfo> rentProviders = mapProviderList(watchProviders.getRentProviders());
        if (rentProviders != null) {
            dto.setRent(rentProviders);
        }

        List<ProviderInfo> flatrateProviders = mapProviderList(watchProviders.getFlatrateProviders());
        if (flatrateProviders != null) {
            dto.setFlatrate(flatrateProviders);
        }

        if (flatrateProviders == null && rentProviders == null) {
            return null;
        }

        return dto;
    }

    private List<ProviderInfo> mapProviderList(List<Provider> providers) {
        return providers == null ? null
                : providers.stream()
                        .map(this::convertTo)
                        .collect(Collectors.toList());
    }
}
