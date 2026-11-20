package com.example.transactions.api;

import com.example.transactions.api.dto.ApiProblem;
import com.example.transactions.api.dto.MemberDetail;
import com.example.transactions.api.dto.MemberSummary;
import com.example.transactions.domain.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "People who have transacted, identified by a normalized phone number")
public class MemberController {
    private static final String PROBLEM_JSON = "application/problem+json";

    private final MemberService service;
    public MemberController(MemberService service) { this.service = service; }

    @GetMapping
    @Operation(operationId = "listMembers", summary = "List members",
            description = "Newest first. Members are created automatically by their first transaction. "
                    + "Phone numbers are masked because this list is public.")
    @ApiResponse(responseCode = "200", description = "Members")
    @ApiResponse(responseCode = "400", description = "limit is outside 1..50",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public List<MemberSummary> list(@Parameter(description = "How many to return (1-50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(MemberService.MAX_LIST) int limit) {
        return service.recent(limit).stream().map(MemberSummary::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMember", summary = "Get a member and their recent transactions")
    @ApiResponse(responseCode = "200", description = "Member found")
    @ApiResponse(responseCode = "400", description = "ID is not a valid UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Member not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public MemberDetail get(@Parameter(description = "Member ID, e.g. from a transaction's memberId",
            example = "b1c7e4a2-8f3d-4e5a-9c61-2d7f0a3b8e94") @PathVariable UUID id) {
        return MemberDetail.from(service.get(id), service.history(id));
    }
}
