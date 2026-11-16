package ma.emsi.tahirou.tp0.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

@Dependent
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole;
    private String requeteJson;

    @Inject
    private LlmClientPourGemini llmClient;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.requeteJson = null;
    }

    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        String sauvegarde = requeteJson;
        String requete = (requeteJson == null)
                ? creerRequeteJson(systemRole, question)
                : ajouteMessage(requeteJson, "user", question);
        try {
            String reponseJson = llmClient.envoyerRequete(requete);
            String texte = extractReponse(reponseJson);
            requeteJson = ajouteMessage(requete, "model", texte);
            return new LlmInteraction(prettyPrinting(requete), prettyPrinting(reponseJson), texte);
        } catch (RequeteException e) {
            requeteJson = sauvegarde;
            throw e;
        }
    }

    private String creerRequeteJson(String role, String question) {
        JsonObject systemInstruction = Json.createObjectBuilder()
            .add("parts", Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("text", role == null ? "" : role)))
            .build();
        return Json.createObjectBuilder()
            .add("system_instruction", systemInstruction)
            .add("contents", Json.createArrayBuilder().add(message("user", question)))
            .build().toString();
    }

    private String ajouteMessage(String json, String role, String texte) {
        JsonObject racine = lire(json);
        JsonArrayBuilder contents = Json.createArrayBuilder(racine.getJsonArray("contents"))
            .add(message(role, texte));
        return Json.createObjectBuilder(racine).add("contents", contents).build().toString();
    }

    private JsonObject message(String role, String texte) {
        return Json.createObjectBuilder()
            .add("role", role)
            .add("parts", Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("text", texte)))
            .build();
    }

    private String extractReponse(String reponseJson) throws RequeteException {
        try {
            return lire(reponseJson).getJsonArray("candidates").getJsonObject(0)
                .getJsonObject("content").getJsonArray("parts").getJsonObject(0)
                .getString("text");
        } catch (Exception e) {
            throw new RequeteException("Reponse sans texte (bloquee ?) : " + reponseJson, e);
        }
    }

    private JsonObject lire(String json) {
        try (JsonReader r = Json.createReader(new StringReader(json))) {
            return r.readObject();
        }
    }

    private String prettyPrinting(String json) {
        StringWriter sw = new StringWriter();
        try (JsonWriter w = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(sw)) {
            w.write(lire(json));
        }
        return sw.toString();
    }
}