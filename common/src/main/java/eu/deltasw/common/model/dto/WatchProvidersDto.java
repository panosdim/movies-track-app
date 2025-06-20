package eu.deltasw.common.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class WatchProvidersDto {
    private List<ProviderDto> rent;
    private List<ProviderDto> buy;
    private List<ProviderDto> flatrate;
}
