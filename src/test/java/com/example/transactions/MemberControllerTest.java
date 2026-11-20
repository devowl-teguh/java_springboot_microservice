package com.example.transactions;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.transactions.api.MemberController;
import com.example.transactions.domain.Member;
import com.example.transactions.domain.MemberNotFoundException;
import com.example.transactions.domain.MemberService;
import com.example.transactions.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
class MemberControllerTest {
    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired MockMvc mvc;
    @MockitoBean MemberService service;

    @Test void listMasksPhoneNumbers() throws Exception {
        when(service.recent(10)).thenReturn(List.of(new Member("628123456789")));
        mvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phoneNumber").value("6281*****789"))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test void listLimitIsValidated() throws Exception {
        mvc.perform(get("/members").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.limit").exists());
        verifyNoInteractions(service);
    }

    @Test void detailShowsFullNumberAndHistory() throws Exception {
        var member = new Member("628123456789");
        var tx = new Transaction("INV-1", new BigDecimal("12500.00"), "628123456789", null, null, member.getId());
        when(service.get(member.getId())).thenReturn(member);
        when(service.history(member.getId())).thenReturn(List.of(tx));
        mvc.perform(get("/members/" + member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("628123456789"))
                .andExpect(jsonPath("$.transactions[0].invoice").value("INV-1"))
                .andExpect(jsonPath("$.transactions[0].status").value("PENDING"));
    }

    @Test void unknownMemberIs404AndBadIdIs400() throws Exception {
        var id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new MemberNotFoundException(id));
        mvc.perform(get("/members/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Member not found"));
        mvc.perform(get("/members/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
