package ma.emsi.tahirou.tp0.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;

@ApplicationScoped
public class LlmClientPourGemini implements Serializable {

    private static final String URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";

    private final String cle;
    private transient Client client;

    public LlmClientPourGemini() {
        this.cle = System.getenv("GEMINI_KEY");
    }

    public String envoyerRequete(String requeteJson) throws RequeteException {
        if (cle == null || cle.isBlank()) {
            throw new RequeteException("Variable d'environnement GEMINI_KEY introuvable");
        }
        if (client == null) client = ClientBuilder.newClient();
        try (Response r = client.target(URL)
                .queryParam("key", cle)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(requeteJson))) {
            String corps = r.readEntity(String.class);
            if (r.getStatus() != 200) {
                throw new RequeteException("Erreur HTTP " + r.getStatus() + " : " + corps);
            }
            return corps;
        }
    }
}