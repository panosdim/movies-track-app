package eu.deltasw.common.model;

import java.util.List;

import lombok.Data;

@Data
public class WatchInfo {
    private List<ProviderInfo> rent;
    private List<ProviderInfo> flatrate;
}
