package com.iseria.domain;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Rumor {
    // ✅ FORMAT TNCD CONSERVÉ (Core data)
    private String type;        // T - Type de rumeur
    private String name;        // N - Nom/titre
    private String content;     // C - Contenu
    private LocalDateTime date; // D - Date

    // ✨ EXTENSIONS pour le workflow admin
    private Long id;
    private String authorFactionId;
    private DATABASE.RumorStatus status; // PENDING, APPROVED, REJECTED
    private Set<String> targetFactions;
    private String submittedBy;
    private String validatedBy;
    private LocalDateTime validationDate;

    // Constructeur compatible TNCD
    public Rumor(String type, String name, String content, LocalDateTime date) {
        this.type = type;
        this.name = name;
        this.content = content;
        this.date = date;
        this.status = DATABASE.RumorStatus.DRAFT;
    }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getContent() { return content; }
    public LocalDateTime getDate() { return date; }
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setContent(String content) { this.content = content; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DATABASE.RumorStatus getStatus() { return status; }
    public void setStatus(DATABASE.RumorStatus status) { this.status = status; }

    public Set<String> getTargetFactions() { return targetFactions; }
    public void setTargetFactions(Set<String> targetFactions) { this.targetFactions = targetFactions; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public LocalDateTime getValidationDate() { return validationDate; }
    public void setValidationDate(LocalDateTime validationDate) { this.validationDate = validationDate; }

    public String getAuthorFactionId() { return authorFactionId; }
    public void setAuthorFactionId(String authorFactionId) { this.authorFactionId = authorFactionId; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", type, name,
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }
}
