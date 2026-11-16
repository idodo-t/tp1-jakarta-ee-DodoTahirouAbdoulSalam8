package ma.emsi.tahirou.tp0.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.emsi.tahirou.tp0.llm.GenerateurReponse;
import ma.emsi.tahirou.tp0.llm.JsonAdapterPourGemini;
import ma.emsi.tahirou.tp0.llm.LlmInteraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean de la page index.xhtml.
 * Portée @ViewScoped : l'instance survit aux requêtes POST successives sur la même vue,
 * ce qui permet de conserver l'historique de la conversation.
 */
@Named("chatBean")
@ViewScoped
public class ChatBean implements Serializable {

    private static final String ROLE_GRIOT = "GRIOT";

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private String question;
    private String reponse;
    private final StringBuilder conversation = new StringBuilder();

    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private FacesContext facesContext;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    public ChatBean() {
    }

    public String getRoleSysteme() { return roleSysteme; }
    public void setRoleSysteme(String roleSysteme) { this.roleSysteme = roleSysteme; }

    public boolean isRoleSystemeChangeable() { return roleSystemeChangeable; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getReponse() { return reponse; }

    public String getConversation() { return conversation.toString(); }
    public void setConversation(String texte) { /* lecture seule : valeur ignorée */ }

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public String getTexteRequeteJson() { return texteRequeteJson; }
    public void setTexteRequeteJson(String texteRequeteJson) { this.texteRequeteJson = texteRequeteJson; }

    public String getTexteReponseJson() { return texteReponseJson; }
    public void setTexteReponseJson(String texteReponseJson) { this.texteReponseJson = texteReponseJson; }

    /** Liste des rôles proposés dans le menu déroulant. */
    public List<SelectItem> getRolesDisponibles() {
        List<SelectItem> roles = new ArrayList<>();
        for (GenerateurReponse.Role r : GenerateurReponse.Role.values()) {
            roles.add(new SelectItem(r.name(), r.getLibelle()));
        }
        roles.add(new SelectItem(ROLE_GRIOT, "Griot sage (proverbes)"));
        return roles;
    }

    /** Texte du rôle système envoyé au LLM pour le rôle choisi dans le menu. */
    private String promptPour(String role) {
        String libelle = "";
        try {
            libelle = GenerateurReponse.Role.valueOf(role).getLibelle();
        } catch (IllegalArgumentException e) {
            // rôle ajouté en dehors de l'enum (ex. GRIOT)
        }
        String cle = (role + " " + libelle).toLowerCase();
        if (cle.contains("griot")) {
            return "Tu es un griot sage. Réponds toujours en français, en deux phrases courtes, "
                 + "puis termine par un proverbe inventé que tu expliques en une phrase.";
        }
        if (cle.contains("traduc") || cle.contains("transl")) {
            return "Tu es un traducteur professionnel anglais-français. Traduis le texte reçu de l'anglais "
                 + "vers le français (ou du français vers l'anglais si le texte est en français). "
                 + "Réponds uniquement par la traduction.";
        }
        if (cle.contains("guide") || cle.contains("tour")) {
            return "Tu es un guide touristique expérimenté. Réponds en français avec des conseils précis "
                 + "sur les lieux à visiter, la culture et les bonnes adresses.";
        }
        return "You are a helpful assistant. Réponds en français de façon claire et concise.";
    }

    /** Traite la question : appelle le LLM via le JsonAdapter et ajoute l'échange à l'historique. */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Texte vide", "Entrez un texte à traiter."));
            return null;
        }
        if (roleSysteme == null || roleSysteme.isBlank()) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Rôle manquant", "Choisissez un rôle."));
            return null;
        }
        try {
            if (roleSystemeChangeable) {
                jsonAdapter.setSystemRole(promptPour(roleSysteme));
            }
            LlmInteraction interaction = jsonAdapter.envoyerRequete(question);
            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.questionJson();
            this.texteReponseJson = interaction.reponseJson();
        } catch (Exception e) {
            this.reponse = "Erreur : " + e.getMessage();
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Problème de connexion avec l'API du LLM",
                            "Problème de connexion avec l'API du LLM : " + e.getMessage()));
            return null;
        }
        afficherConversation();
        this.roleSystemeChangeable = false;
        this.question = "";
        return null;
    }

    /** Réinitialise la conversation en recréant une nouvelle vue (donc un nouveau bean @ViewScoped). */
    public String nouveauChat() {
        return "index?faces-redirect=true";
    }

    private void afficherConversation() {
        conversation.append("== Rôle : ").append(roleSysteme).append(" ==\n");
        conversation.append("Utilisateur : ").append(question).append("\n");
        conversation.append("Système : ").append(reponse).append("\n\n");
    }
}