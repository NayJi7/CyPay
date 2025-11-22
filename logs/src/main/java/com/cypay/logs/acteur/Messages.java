package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.logs.model.LogEntry;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;

/**
 * Tous les types de messages utilisés pour la communication entre acteurs
 */
public class Messages {

    // ========== REQUÊTES HTTP ==========

    /**
     * Message pour traiter une requête HTTP entrante
     */
    public record HttpRequest(
            HttpExchange exchange,
            String method,
            String path,
            String query,
            String body
    ) {}

    // ========== REQUÊTES BASE DE DONNÉES ==========

    /**
     * Récupérer tous les logs avec limite
     */
    public record GetAllLogsQuery(
            Acteur<?> replyTo,
            String requestId,
            HttpExchange exchange,
            Integer limit
    ) {}

    /**
     * Récupérer les logs d'un acteur spécifique
     */
    public record GetLogsByActorQuery(
            Acteur<?> replyTo,
            String requestId,
            HttpExchange exchange,
            String acteur,
            Integer limit
    ) {}

    /**
     * Récupérer les logs par niveau (INFO, ERROR, etc.)
     */
    public record GetLogsByLevelQuery(
            Acteur<?> replyTo,
            String requestId,
            HttpExchange exchange,
            String niveau,
            Integer limit
    ) {}

    /**
     * Supprimer tous les logs
     */
    public record DeleteAllLogsCommand(
            Acteur<?> replyTo,
            String requestId,
            HttpExchange exchange
    ) {}

    /**
     * Récupérer les statistiques
     */
    public record GetStatsQuery(
            Acteur<?> replyTo,
            String requestId,
            HttpExchange exchange
    ) {}

    // ========== RÉPONSES BASE DE DONNÉES ==========

    /**
     * Réponse contenant une liste de logs
     */
    public record LogsResponse(
            String requestId,
            HttpExchange exchange,
            List<LogEntry> logs,
            boolean success,
            String error
    ) {}

    /**
     * Réponse de suppression
     */
    public record DeleteResponse(
            String requestId,
            HttpExchange exchange,
            int deletedCount,
            boolean success,
            String error
    ) {}

    // ========== STATISTIQUES ==========

    /**
     * Réponse avec statistiques
     */
    public record StatsResponse(
            String requestId,
            HttpExchange exchange,
            LogStats stats,
            boolean success,
            String error
    ) {}

    /**
     * Données de statistiques
     */
    public record LogStats(
            long total,
            long errors,
            long infos,
            long warnings,
            long httpRequests,
            long messageIn,
            long messageOut,
            int distinctActors,
            String firstLogTime,
            String lastLogTime
    ) {}

    // ========== ERREURS ==========

    /**
     * Message d'erreur générique
     */
    public record ErrorResponse(
            String requestId,
            HttpExchange exchange,
            String error,
            int statusCode
    ) {}

    // ========== SUPERVISION ==========

    /**
     * Notification de défaillance d'un acteur
     */
    public record ActorFailed(
            String actorName,
            Throwable error,
            long timestamp
    ) {}

    /**
     * Demande de vérification de santé
     */
    public record HealthCheck(
            String requestId
    ) {}
}