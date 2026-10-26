package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.repository.TempleRepository;

@ExtendWith(MockitoExtension.class)
class TempleGuardTest {

    @Mock
    private TempleRepository templeRepository;
    @InjectMocks
    private TempleGuard templeGuard;

    @Test
    void passesWhenTempleExists() {
        when(templeRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> templeGuard.requireExists(1L)).doesNotThrowAnyException();
    }

    @Test
    void throwsWhenTempleMissing() {
        when(templeRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> templeGuard.requireExists(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Temple not found with id: 9");
    }
}
