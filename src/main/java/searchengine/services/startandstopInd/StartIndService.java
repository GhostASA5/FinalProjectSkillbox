package searchengine.services.startandstopInd;

import searchengine.dto.startandstop.StartIndResponse;
import searchengine.dto.startandstop.StopIndResponse;

public interface StartIndService {

    StartIndResponse beginIndexing();

    StopIndResponse stopIndexing();
}
