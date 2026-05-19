package com.nexq.repository;

import com.nexq.model.Queue;
import com.nexq.model.Token;
import com.nexq.model.TokenStatus;
import com.nexq.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    // ── Queue position & count ──────────────────────────────────────────────────
    long countByQueueAndStatus(Queue queue, TokenStatus status);

    @Query("SELECT COUNT(t) FROM Token t WHERE t.queue = :queue AND t.status = 'WAITING' AND t.tokenNumber < :tokenNumber")
    long countAheadInQueue(@Param("queue") Queue queue, @Param("tokenNumber") int tokenNumber);

    // ── Status queries ──────────────────────────────────────────────────────────
    List<Token> findByQueueAndStatusOrderByPriorityWeightDescTokenNumberAsc(Queue queue, TokenStatus status);

    Optional<Token> findFirstByQueueAndStatusOrderByPriorityWeightDescTokenNumberAsc(Queue queue, TokenStatus status);

    List<Token> findByUserOrderByIssuedAtDesc(User user);

    Optional<Token> findByUserAndQueueAndStatusIn(User user, Queue queue, List<TokenStatus> statuses);

    // ── Scheduler: find expired tokens ─────────────────────────────────────────
    @Query("SELECT t FROM Token t WHERE t.expiresAt < :now AND t.status IN ('WAITING', 'SERVING')")
    List<Token> findExpiredTokens(@Param("now") LocalDateTime now);

    // ── Analytics: avg wait time by hour ───────────────────────────────────────
    @Query("SELECT HOUR(t.issuedAt) as hour, AVG(TIMESTAMPDIFF(SECOND, t.issuedAt, t.completedAt)) / 60.0 as avgWait " +
           "FROM Token t WHERE t.queue = :queue AND t.status = 'COMPLETED' AND t.completedAt IS NOT NULL " +
           "GROUP BY HOUR(t.issuedAt) ORDER BY HOUR(t.issuedAt)")
    List<Object[]> findAvgWaitTimeByHour(@Param("queue") Queue queue);

    // ── Analytics: tokens per day ───────────────────────────────────────────────
    @Query("SELECT DATE(t.issuedAt) as day, COUNT(t) as total, " +
           "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as served " +
           "FROM Token t WHERE t.queue = :queue GROUP BY DATE(t.issuedAt) ORDER BY DATE(t.issuedAt) DESC")
    List<Object[]> findDailyStats(@Param("queue") Queue queue);

    // ── Analytics: peak hours ──────────────────────────────────────────────────
    @Query("SELECT HOUR(t.issuedAt) as hour, COUNT(t) as count FROM Token t " +
           "WHERE t.queue = :queue GROUP BY HOUR(t.issuedAt) ORDER BY COUNT(t) DESC")
    List<Object[]> findPeakHours(@Param("queue") Queue queue);

    // ── Notification: users approaching turn (2 ahead) ─────────────────────────
    @Query("SELECT t FROM Token t WHERE t.queue = :queue AND t.status = 'WAITING' " +
           "AND t.tokenNumber = :tokenNumber")
    Optional<Token> findByQueueAndTokenNumber(@Param("queue") Queue queue, @Param("tokenNumber") int tokenNumber);
}
