package com.generalisthealthai.rcm.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.ExemplarSearchRequestDto;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExemplarController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExemplarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RcmExemplarRepository exemplarRepository;

    @MockBean
    private VectorSearchService vectorSearchService;

    @Test
    void testListExemplars() throws Exception {
        RcmExemplar exemplar = RcmExemplar.builder()
                .id(UUID.randomUUID())
                .payerId("BCBS")
                .cptCode("93000")
                .groundTruthDecision(ReviewDecision.PAID)
                .build();

        when(exemplarRepository.findAll()).thenReturn(List.of(exemplar));

        mockMvc.perform(get("/api/v1/rcm/exemplars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payerId").value("BCBS"))
                .andExpect(jsonPath("$[0].cptCode").value("93000"));
    }

    @Test
    void testSearchExemplars() throws Exception {
        ExemplarSearchRequestDto searchDto = ExemplarSearchRequestDto.builder()
                .query("EKG chest pain")
                .limit(2)
                .build();

        RcmExemplar exemplar = RcmExemplar.builder()
                .id(UUID.randomUUID())
                .payerId("BCBS")
                .cptCode("93000")
                .groundTruthDecision(ReviewDecision.PAID)
                .build();

        when(vectorSearchService.findTopKExemplarsForText(anyString(), anyInt())).thenReturn(List.of(exemplar));

        mockMvc.perform(post("/api/v1/rcm/exemplars/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cptCode").value("93000"));
    }
}
