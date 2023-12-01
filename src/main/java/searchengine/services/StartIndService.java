package searchengine.services;

import searchengine.dto.statistics.StartIndResponse;

import java.util.concurrent.ExecutionException;

public interface StartIndService {

    StartIndResponse getSites();
}
