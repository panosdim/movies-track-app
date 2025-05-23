package eu.deltasw.tmdb_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import eu.deltasw.tmdb_service.model.dto.ProviderDto;
import eu.deltasw.tmdb_service.model.dto.WatchProvidersDto;
import info.movito.themoviedbapi.model.core.watchproviders.Provider;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;

@Service
public class WatchProvidersMapperService {

    private final ModelMapper modelMapper;

    public WatchProvidersMapperService(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ProviderDto convertToDto(Provider provider) {
        return modelMapper.map(provider, ProviderDto.class);
    }

    public WatchProvidersDto convertToDto(WatchProviders watchProviders) {
        if (watchProviders == null) {
            return null;
        }
        WatchProvidersDto dto = new WatchProvidersDto();

        dto.setRent(mapProviderList(watchProviders.getRentProviders()));
        dto.setBuy(mapProviderList(watchProviders.getBuyProviders()));
        dto.setFlatrate(mapProviderList(watchProviders.getFlatrateProviders()));

        return dto;
    }

    private List<ProviderDto> mapProviderList(List<Provider> providers) {
        return providers == null ? null
                : providers.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
    }
}
