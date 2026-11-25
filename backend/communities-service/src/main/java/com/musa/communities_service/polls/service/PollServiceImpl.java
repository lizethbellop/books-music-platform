package com.musa.communities_service.polls.service;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import com.musa.communities_service.exception.BusinessRuleException;
import com.musa.communities_service.exception.ForbiddenActionException;
import com.musa.communities_service.exception.ResourceNotFoundException;
import com.musa.communities_service.polls.dto.*;
import com.musa.communities_service.polls.entity.Poll;
import com.musa.communities_service.polls.entity.PollOption;
import com.musa.communities_service.polls.entity.PollVote;
import com.musa.communities_service.polls.exception.InvalidPollRequestException;
import com.musa.communities_service.polls.repository.PollOptionRepository;
import com.musa.communities_service.polls.repository.PollRepository;
import com.musa.communities_service.polls.repository.PollVoteRepository;
import com.musa.communities_service.repository.CommunityMemberRepository;
import com.musa.communities_service.repository.CommunityRepository;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.musa.communities_service.polls.messaging.PollChanges;

@Service
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        timeout = 8
)
public class PollServiceImpl implements PollService {

    private final PollRepository polls;
    private final PollOptionRepository options;
    private final PollVoteRepository votes;
    private final CommunityRepository communities;
    private final CommunityMemberRepository members;
    private final JdbcTemplate jdbc;
    private final Validator validator;
    private final PollChanges changes;

    public PollServiceImpl(
            PollRepository polls,
            PollOptionRepository options,
            PollVoteRepository votes,
            CommunityRepository communities,
            CommunityMemberRepository members,
            JdbcTemplate jdbc,
            Validator validator,
            PollChanges changes
    ) {
        this.polls = polls;
        this.options = options;
        this.votes = votes;
        this.communities = communities;
        this.members = members;
        this.jdbc = jdbc;
        this.validator = validator;
        this.changes = changes;
    }

    @Override
    public PollResponse create(
            UUID communityId,
            UUID currentUserId,
            CreatePollRequest request
    ) {
        configureDatabaseLimits();
        requireAdministrator(communityId, currentUserId);
        validateRequest(request);

        String question = request.question().strip();

        List<String> labels = request.options().stream()
                .map(String::strip)
                .toList();

        requireDistinctOptions(labels);

        if (!request.closesAt().isAfter(databaseNow().plusMinutes(1))) {
            throw new InvalidPollRequestException(
                    "El cierre debe estar al menos un minuto en el futuro"
            );
        }

        Poll poll = new Poll(
                communityId,
                currentUserId,
                question,
                request.closesAt()
        );

        // Las opciones necesitan que su encuesta ya exista en PostgreSQL.
        polls.saveAndFlush(poll);

        for (int index = 0; index < labels.size(); index++) {
            options.save(new PollOption(
                    poll.getId(),
                    labels.get(index),
                    (short) (index + 1)
            ));
        }

        options.flush();
        changes.record(poll.getId());

        return toResponse(poll, databaseNow());
    }

    @Override
    public Slice<PollResponse> list(
            UUID communityId,
            UUID currentUserId,
            int page,
            int size
    ) {
        configureDatabaseLimits();
        requireMember(communityId, currentUserId);

        if (page < 0 || page > 1000 || size < 1 || size > 50) {
            throw new InvalidPollRequestException(
                    "La página debe estar entre 0 y 1000 y el tamaño entre 1 y 50"
            );
        }

        OffsetDateTime now = databaseNow();

        return polls.findByCommunityIdOrderByCreatedAtDescIdDesc(
                communityId,
                PageRequest.of(page, size)
        ).map(poll -> toResponse(poll, now));
    }

    @Override
    public PollResponse get(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    ) {
        configureDatabaseLimits();
        requireMember(communityId, currentUserId);

        return toResponse(
                findPoll(communityId, pollId),
                databaseNow()
        );
    }

    @Override
    public PollResultsResponse results(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    ) {
        configureDatabaseLimits();
        requireMember(communityId, currentUserId);

        return toResults(
                findPoll(communityId, pollId),
                currentUserId
        );
    }

    @Override
    public PollResultsResponse vote(
            UUID communityId,
            UUID pollId,
            UUID currentUserId,
            VoteRequest request
    ) {
        configureDatabaseLimits();
        requireMember(communityId, currentUserId);
        validateRequest(request);

        Poll poll = polls.findForVoting(pollId)
                .orElseThrow(this::pollNotFound);

        requireCommunityMatch(poll, communityId);

        PollVote previousVote = votes.findByPollIdAndUserId(
                pollId,
                currentUserId
        ).orElse(null);

        if (previousVote != null) {
            requireSameOption(previousVote, request.optionId());
            return toResults(poll, currentUserId);
        }

        // Se consulta la hora después de obtener el bloqueo.
        if (!poll.isOpenAt(databaseNow())) {
            throw new BusinessRuleException(
                    "La encuesta ya terminó"
            );
        }

        if (!options.existsByIdAndPollId(request.optionId(), pollId)) {
            throw new InvalidPollRequestException(
                    "Selecciona una opción válida para esta encuesta"
            );
        }

        int inserted = votes.insertIfAbsent(
                UUID.randomUUID(),
                pollId,
                request.optionId(),
                currentUserId
        );

        if (inserted == 1) {
            changes.record(pollId);
        }

        if (inserted == 0) {
            PollVote concurrentVote = votes.findByPollIdAndUserId(
                    pollId,
                    currentUserId
            ).orElseThrow(() -> new IllegalStateException(
                    "No se encontró el voto que produjo el conflicto"
            ));

            requireSameOption(concurrentVote, request.optionId());
        }

        return toResults(poll, currentUserId);
    }

    @Override
    public PollResponse close(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    ) {
        configureDatabaseLimits();
        requireAdministrator(communityId, currentUserId);

        Poll poll = polls.findForClosing(pollId)
                .orElseThrow(this::pollNotFound);

        requireCommunityMatch(poll, communityId);

        OffsetDateTime now = databaseNow();

        if (poll.isOpenAt(now)) {
            poll.close(now);
            changes.record(pollId);
        }

        return toResponse(poll, now);
    }

    private void requireMember(UUID communityId, UUID userId) {
        requireRole(communityId, userId, false);
    }

    private void requireAdministrator(UUID communityId, UUID userId) {
        requireRole(communityId, userId, true);
    }

    private void requireRole(
            UUID communityId,
            UUID userId,
            boolean administrator
    ) {
        var roles = jdbc.query("""
            SELECT m.role
            FROM communities c
            JOIN community_members m
                ON m.community_id = c.id
            WHERE c.id = ?
              AND c.status = 'ACTIVA'
              AND m.user_id = ?
              AND m.status = 'ACTIVO'
            FOR SHARE OF c, m
            """,
                (row, index) -> row.getString(1),
                communityId,
                userId
        );

        if (roles.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Comunidad no disponible"
            );
        }

        if (administrator
                && !"ADMINISTRADOR".equals(roles.getFirst())) {
            throw new ForbiddenActionException(
                    "Esta acción requiere ser administrador de la comunidad"
            );
        }
    }

    private void requireActiveCommunity(UUID communityId) {
        Community community = communities.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comunidad no disponible"
                ));

        if (community.getStatus() != CommunityStatus.ACTIVA) {
            throw new ResourceNotFoundException(
                    "Comunidad no disponible"
            );
        }
    }

    private Poll findPoll(UUID communityId, UUID pollId) {
        Poll poll = polls.findById(pollId)
                .orElseThrow(this::pollNotFound);

        requireCommunityMatch(poll, communityId);
        return poll;
    }

    private void requireCommunityMatch(Poll poll, UUID communityId) {
        if (!poll.getCommunityId().equals(communityId)) {
            throw pollNotFound();
        }
    }

    private void requireSameOption(PollVote vote, UUID optionId) {
        if (!vote.getOptionId().equals(optionId)) {
            throw new BusinessRuleException(
                    "Ya votaste y no puedes cambiar de opción"
            );
        }
    }

    private void requireDistinctOptions(List<String> labels) {
        var normalized = new HashSet<String>();

        for (String label : labels) {
            if (label.isBlank()) {
                throw new InvalidPollRequestException(
                        "Las opciones no pueden estar vacías"
                );
            }

            if (!normalized.add(label.toLowerCase(Locale.ROOT))) {
                throw new InvalidPollRequestException(
                        "Las opciones no deben repetirse"
                );
            }
        }
    }

    private <T> void validateRequest(T request) {
        if (request == null) {
            throw new InvalidPollRequestException(
                    "Debes enviar los datos de la solicitud"
            );
        }

        var violations = validator.validate(request);

        if (!violations.isEmpty()) {
            throw new InvalidPollRequestException(
                    violations.iterator().next().getMessage()
            );
        }
    }

    private PollResponse toResponse(Poll poll, OffsetDateTime now) {
        List<PollResponse.Option> pollOptions =
                options.findByPollIdOrderByPositionAsc(poll.getId())
                        .stream()
                        .map(option -> new PollResponse.Option(
                                option.getId(),
                                option.getLabel(),
                                option.getPosition()
                        ))
                        .toList();

        return new PollResponse(
                poll.getId(),
                poll.getCommunityId(),
                poll.getCreatedBy(),
                poll.getQuestion(),
                poll.getCreatedAt(),
                poll.getClosesAt(),
                poll.getClosedAt(),
                poll.isOpenAt(now),
                pollOptions
        );
    }

    private PollResultsResponse toResults(Poll poll, UUID userId) {
        List<PollResultsResponse.OptionResult> results =
                votes.findResults(poll.getId())
                        .stream()
                        .map(result -> new PollResultsResponse.OptionResult(
                                result.getOptionId(),
                                result.getLabel(),
                                result.getPosition(),
                                result.getVoteCount()
                        ))
                        .toList();

        long totalVotes = results.stream()
                .mapToLong(PollResultsResponse.OptionResult::voteCount)
                .sum();

        UUID selectedOptionId = votes.findByPollIdAndUserId(
                poll.getId(),
                userId
        ).map(PollVote::getOptionId).orElse(null);

        OffsetDateTime now = databaseNow();

        return new PollResultsResponse(
                poll.getId(),
                poll.isOpenAt(now),
                totalVotes,
                selectedOptionId,
                now,
                results
        );
    }

    private ResourceNotFoundException pollNotFound() {
        return new ResourceNotFoundException(
                "Encuesta no disponible"
        );
    }

    private void configureDatabaseLimits() {
        jdbc.queryForObject(
                """
                SELECT
                    set_config('lock_timeout', '1500ms', true),
                    set_config('statement_timeout', '3000ms', true),
                    set_config(
                        'idle_in_transaction_session_timeout',
                        '5000ms',
                        true
                    )
                """,
                (row, index) -> row.getString(1)
        );
    }

    private OffsetDateTime databaseNow() {
        return jdbc.queryForObject(
                "SELECT clock_timestamp()",
                (rs, rowNum) -> rs.getObject(1, OffsetDateTime.class)
        );
    }
}