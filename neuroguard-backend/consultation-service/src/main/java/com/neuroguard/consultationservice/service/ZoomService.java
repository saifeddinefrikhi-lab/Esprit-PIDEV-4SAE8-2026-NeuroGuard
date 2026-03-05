package com.neuroguard.consultationservice.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ZoomService {

    // Création d'une salle Jitsi Meet (gratuit, sans clé API)
    // La vraie URL de rejoindre est générée par getJoinLink avec l'ID de la consultation
    public MeetingInfo createMeeting(String topic, LocalDateTime startTime, long durationMinutes) {
        String meetingId = UUID.randomUUID().toString();
        // Stocké pour compatibilité; getJoinLink utilise l'ID consultation pour Jitsi
        String joinUrl = "https://meet.jit.si/NeuroGuard-" + meetingId;
        return new MeetingInfo(meetingId, joinUrl);
    }

    public static class MeetingInfo {
        private final String meetingId;
        private final String joinUrl;

        public MeetingInfo(String meetingId, String joinUrl) {
            this.meetingId = meetingId;
            this.joinUrl = joinUrl;
        }

        public String getMeetingId() { return meetingId; }
        public String getJoinUrl() { return joinUrl; }
    }
}