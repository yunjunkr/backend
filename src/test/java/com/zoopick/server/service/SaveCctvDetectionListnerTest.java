package com.zoopick.server.service;

import com.zoopick.server.dto.match.SaveCctvDetectionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SaveCctvDetectionListnerTest {

    @Mock CctvMatchService cctvMatchService;

    @InjectMocks SaveCctvDetectionListner listner;

    @Test
    @DisplayName("SaveCctvDetectionEvent 수신 시 matchCctvToLostItems()를 호출한다")
    void handleMatchCctvToLostItems_delegatesToMatchService() {
        SaveCctvDetectionEvent event = new SaveCctvDetectionEvent(42L);

        listner.handleMatchCctvToLostItems(event);

        then(cctvMatchService).should().matchCctvToLostItems(42L);
    }
}
